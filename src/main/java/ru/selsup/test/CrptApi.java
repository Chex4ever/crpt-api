package ru.selsup.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;

import java.time.Duration;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    public static final Logger logger = LoggerFactory.getLogger(CrptApi.class);
    public static final ObjectMapper mapper = new ObjectMapper();

    private static volatile Pair<PrivateKey, X509Certificate> selectedCertInfo;

    private final String baseUrl;
    private final String authGetDataEndpoint;
    private final String authGetTokenEndpoint;
    private final String createDocumentEndpoint;

    private final HttpClient httpClient;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong requestsCounter = new AtomicLong(0);
    private final Duration periodDuration;
    private final int requestLimit;
    private final ScheduledExecutorService scheduler;
    private String authToken;

    public CrptApi(Config config) {
        logger.info("Initializing CrptApi with config: {}", config);
        if (config == null) {
            logger.error("Config cannot be null");
            throw new IllegalArgumentException("Invalid parameters");
        }
        this.baseUrl = config.getBaseUrl();
        this.authGetTokenEndpoint = config.getAuthGetTokenEndpoint();
        this.authGetDataEndpoint = config.getAuthGetDataEndpoint();
        this.createDocumentEndpoint = config.getCreateDocumentEndpoint();

        this.requestLimit = config.getRequestLimit();
        this.periodDuration = Duration.of(config.getTimeUnit().toMillis(1), TimeUnit.MILLISECONDS.toChronoUnit());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getConnectionTimeout())
                .build();
        resetRequestsCounter();
        logger.debug("CrptApi initialized successfully. Request limit: {}, Period: {}", requestLimit, periodDuration);
    }


    public synchronized CompletableFuture<Void> authenticate() {
        logger.info("Starting authentication process");
        return fetchUuidAndDataFromServer()
                .thenCompose(this::signData)
                .thenCompose(this::sendSignedDataToServer);
    }

//    private static class CertSelectionCallback implements CryptoUICallback {
//
//    }

    private static Pair<PrivateKey, X509Certificate> selectUserCertificate() {
        if (selectedCertInfo != null) {
            logger.info("Сертификат выбран: {}", selectedCertInfo);
            return selectedCertInfo;
        }
        try {
            logger.info("...сертификат не выбран. Выбираем: keyStore = KeyStore.getInstance(\"Windows-MY\", \"SunMSCAPI\")");
            KeyStore keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            keyStore.load(null, null);
            Enumeration<String> aliasesEnum = keyStore.aliases();
            //todo - если только один сертификат, то не спрашивать...

            logger.info("Начинаю перебирать сертификаты {}", aliasesEnum);
            while (aliasesEnum.hasMoreElements()) {
                String alias = aliasesEnum.nextElement();
                logger.info("Нашёл сертификат {}", alias);
                Certificate cert = keyStore.getCertificate(alias);
                if (!(cert instanceof X509Certificate)) {
                    logger.info("Не X509Certificate");
                    continue;
                }
                if (((X509Certificate) cert).getBasicConstraints() < 0) {
                    logger.info("Cert basic constraints is less than zero");
                    continue;
                }

                logger.info("Choose certificate [\" + alias + \"] ? (y/n)");
                System.out.println("Choose certificate [" + alias + "] ? (y/n)");
                char answer = (char) System.in.read();
                if (answer == 'y') {
                    PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, "".toCharArray());
                    selectedCertInfo = new Pair<>(privateKey, (X509Certificate) cert);
                    break;
                }
            }
        } catch (KeyStoreException | NoSuchProviderException | IOException | NoSuchAlgorithmException |
                 CertificateException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }

        if (selectedCertInfo == null) {
            throw new RuntimeException("No valid certificates found or chosen!");
        }
        logger.info("Выбран сертификат {}",selectedCertInfo);
        return selectedCertInfo;
    }

    private void resetRequestsCounter() {
        scheduler.schedule(() -> {
            lock.lock();
            try {
                long currentCount = requestsCounter.get();
                requestsCounter.set(0);
                logger.debug("Request counter reset from {} to 0", currentCount);
            } finally {
                lock.unlock();
            }
        }, periodDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    private CompletableFuture<Pair<String, String>> fetchUuidAndDataFromServer() {
        logger.info("Fetching UUID and data from server.");
        URI uri = URI.create(baseUrl + authGetDataEndpoint);
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.error("Failed to fetch uuid and data: {}", response.statusCode());
                        throw new RuntimeException("Failed to fetch uuid and data: " + response.statusCode());
                    }
                    try {
                        ObjectNode root = mapper.readValue(response.body(), ObjectNode.class);
                        String uuid = root.get("uuid").asText();
                        String data = root.get("data").asText();
                        logger.info("Received uuid={}, data={}", uuid, data);
                        return new Pair<>(uuid, data);
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing resonse:", e);
                        throw new RuntimeException(e);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Fetching UUID and data from server failed", throwable);
                    throw new CompletionException(throwable);
                });
    }

    private CompletableFuture<Pair<String, String>> signData(Pair<String, String> pair) {
        String uuid = pair.first;
        String data = pair.second;

        logger.info("Выбираем сертификат пользователя (Если ещё не выбрали)");
        Pair<PrivateKey, X509Certificate> certInfo = selectUserCertificate();
        PrivateKey privateKey = certInfo.first;
        X509Certificate cert = certInfo.second;

        logger.info("Signing data using CryptoPro CSP");
        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256withRSA", "CryptoPro");
            sig.initSign(privateKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signedData = sig.sign();
            String base64SignedData = Base64.getEncoder().encodeToString(signedData);
            return CompletableFuture.completedFuture(new Pair<>(uuid, base64SignedData));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Void> sendSignedDataToServer(Pair<String, String> pair) {
        String uuid = pair.first;
        String signedData = pair.second;

        logger.info("Sending signed data back to server.");
        ObjectNode jsonBody = mapper.createObjectNode();
        jsonBody.put("uuid", uuid)
                .put("data", signedData);
        URI uri = URI.create(baseUrl + authGetTokenEndpoint);
        String body = jsonBody.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        logger.debug("Sending authentication request to: {}, body: {}", uri, body);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        logger.error("Authentication failed with status code: {}", response.statusCode());
                        throw new RuntimeException("Authentication failed: " + response.statusCode());
                    }
                    try {
                        ObjectNode root = mapper.readValue(response.body(), ObjectNode.class);
                        authToken = root.get("token").asText();
                        logger.info("Authentication successful. Token received: {}", authToken);
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing resonse:", e);
                        throw new RuntimeException(e);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Authentication request failed", throwable);
                    throw new CompletionException(throwable);
                });
    }

    public CompletableFuture<String> createDocument(Document document, String signature) throws JsonProcessingException, InterruptedException {
        logger.info("Creating document of type: {}", document.getType());
        logger.debug("Document details: {}", document);
        checkRateLimit();

        String encodedDocument = encodeDocument(document);
        String encodedSignature = encodeSignature(signature);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonBody = objectMapper.createObjectNode();
        jsonBody.put("product_document", encodedDocument)
                .put("document_format", "MANUAL")
                .put("type", document.getType().getCode())
                .put("signature", encodedSignature);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + createDocumentEndpoint))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();
        logger.debug("Sending document creation request to: {}", baseUrl + createDocumentEndpoint);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        logger.error("Document creation request failed", throwable);
                        throw new CompletionException(throwable);
                    }
                    if (response.statusCode() != 200) {
                        logger.error("Document creation failed with status: {}. Response: {}",
                                response.statusCode(), response.body());
                        throw new CompletionException(
                                new IOException("Create document failed: " + response.statusCode()
                                        + ", " + response.body())
                        );
                    }
                    logger.info("Document created successfully. Status: {}", response.statusCode());
                    logger.debug("Response body: {}", response.body());
                    return response.body();
                });
    }

    private void checkRateLimit() throws InterruptedException {
        lock.lock();
        try {
            long currentCount = requestsCounter.incrementAndGet();
            logger.debug("Current request count: {}", currentCount);

            while (requestsCounter.incrementAndGet() > requestLimit) {
                logger.warn("Rate limit exceeded ({} > {}). Waiting for reset...", currentCount, requestLimit);
                Thread.sleep(periodDuration.toMillis());
                requestsCounter.set(0);
                logger.debug("Request counter reset. New count: {}", requestsCounter.get());

            }
        } finally {
            lock.unlock();
        }
    }

    private String generateRandomData(UUID uuid) {
        String data = uuid.toString() + "-" + System.currentTimeMillis();
        logger.trace("Generated random data: {}", data);
        return data;
    }

    private String signData(String data, String password) throws NoSuchAlgorithmException {
        logger.trace("Signing data with SHA-256");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest((data + password).getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hash);
        logger.trace("Data signed successfully");
        return signature;
    }

    private String encodeSignature(String signature) {
        String encoded = Base64.getEncoder().encodeToString(signature.getBytes(StandardCharsets.UTF_8));
        logger.trace("Signature encoded: {}", encoded);
        return encoded;
    }

    private String encodeDocument(Document document) throws JsonProcessingException {
        String encoded = Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(document));
        logger.trace("Document encoded successfully");
        return encoded;
    }

    public static class Product {
        private String uitCode;
        private String uituCode;
        private String tnvedCode;
        private String certificateDocument;
        private String certificateDocumentNumber;
        private String certificateDocumentDate;

        public Product(String uitCode, String uituCode, String tnvedCode, String certificateDocument, String certificateDocumentNumber, String certificateDocumentDate) {
            this.uitCode = uitCode;
            this.uituCode = uituCode;
            this.tnvedCode = tnvedCode;
            this.certificateDocument = certificateDocument;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }
    }

    public static class Document {
        private String description;
        private String id;
        private String status;
        private DocumentType type;
        private boolean importRequest;
        private ProductGroup productGroup;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public DocumentType getType() {
            return type;
        }

        public void setType(DocumentType type) {
            this.type = type;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public ProductGroup getProductGroup() {
            return productGroup;
        }

        public void setProductGroup(ProductGroup productGroup) {
            this.productGroup = productGroup;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }
    }


    public enum DocumentType {
        // Агрегация
        AGGREGATION_DOCUMENT("AGGREGATION_DOCUMENT", "Документ агрегации", DocumentFormat.JSON),
        AGGREGATION_DOCUMENT_CSV("AGGREGATION_DOCUMENT_CSV", "Агрегация", DocumentFormat.CSV),
        AGGREGATION_DOCUMENT_XML("AGGREGATION_DOCUMENT_XML", "Агрегация", DocumentFormat.XML),

        // Дезагрегация
        DISAGGREGATION_DOCUMENT("DISAGGREGATION_DOCUMENT", "Дезагрегация", DocumentFormat.JSON),
        DISAGGREGATION_DOCUMENT_CSV("DISAGGREGATION_DOCUMENT_CSV", "Дезагрегация", DocumentFormat.CSV),
        DISAGGREGATION_DOCUMENT_XML("DISAGGREGATION_DOCUMENT_XML", "Дезагрегация", DocumentFormat.XML),

        // Переагрегация
        REAGGREGATION_DOCUMENT("REAGGREGATION_DOCUMENT", "Переагрегация", DocumentFormat.JSON),
        REAGGREGATION_DOCUMENT_CSV("REAGGREGATION_DOCUMENT_CSV", "Переагрегация", DocumentFormat.CSV),
        REAGGREGATION_DOCUMENT_XML("REAGGREGATION_DOCUMENT_XML", "Переагрегация", DocumentFormat.XML),

        // Ввод в оборот (Производство РФ)
        LP_INTRODUCE_GOODS("LP_INTRODUCE_GOODS", "Ввод в оборот. Производство РФ", DocumentFormat.JSON),
        LP_INTRODUCE_GOODS_CSV("LP_INTRODUCE_GOODS_CSV", "Ввод в оборот. Производство РФ", DocumentFormat.CSV),
        LP_INTRODUCE_GOODS_XML("LP_INTRODUCE_GOODS_XML", "Ввод в оборот. Производство РФ", DocumentFormat.XML),

        // Отгрузка
        LP_SHIP_GOODS("LP_SHIP_GOODS", "Отгрузка", DocumentFormat.JSON),
        LP_SHIP_GOODS_CSV("LP_SHIP_GOODS_CSV", "Отгрузка", DocumentFormat.CSV),
        LP_SHIP_GOODS_XML("LP_SHIP_GOODS_XML", "Отгрузка", DocumentFormat.XML),

        // Приемка
        LP_ACCEPT_GOODS("LP_ACCEPT_GOODS", "Приемка", DocumentFormat.JSON),
        LP_ACCEPT_GOODS_XML("LP_ACCEPT_GOODS_XML", "Приемка", DocumentFormat.XML),

        // ПеремарDocumentFormat.JSON),
        LK_REMARK("LK_REMARK", "Перемаркировка", DocumentFormat.JSON),
        LK_REMARK_CSV("LK_REMARK_CSV", "Перемаркировка", DocumentFormat.CSV),
        LK_REMARK_XML("LK_REMARK_XML", "Перемаркировка", DocumentFormat.XML),

        // Вывод из оборота по чеку
        LK_RECEIPT("LK_RECEIPT", "Вывод из оборота по чеку через личный кабинет", DocumentFormat.JSON),
        LK_RECEIPT_CSV("LK_RECEIPT_CSV", "Вывод из оборота по чеку через личный кабинет", DocumentFormat.CSV),
        LK_RECEIPT_XML("LK_RECEIPT_XML", "Вывод из оборота по чеку через личный кабинет", DocumentFormat.XML),

        // Ввод в оборот (Импорт)
        LP_GOODS_IMPORT("LP_GOODS_IMPORT", "Ввод в оборот. Импорт", DocumentFormat.JSON),
        LP_GOODS_IMPORT_CSV("LP_GOODS_IMPORT_CSV", "Ввод в оборот. Импорт", DocumentFormat.CSV),
        LP_GOODS_IMPORT_XML("LP_GOODS_IMPORT_XML", "Ввод в оборот. Импорт", DocumentFormat.XML),

        // Отмена отгрузки
        LP_CANCEL_SHIPMENT("LP_CANCEL_SHIPMENT", "Отмена отгрузки", DocumentFormat.JSON),
        LP_CANCEL_SHIPMENT_CSV("LP_CANCEL_SHIPMENT_CSV", "Отмена отгрузки", DocumentFormat.CSV),
        LP_CANCEL_SHIPMENT_XML("LP_CANCEL_SHIPMENT_XML", "Отмена отгрузки", DocumentFormat.XML),

        // Списание ненанесенных КИ
        LK_KM_CANCELLATION("LK_KM_CANCELLATION", "Списание ненанесенных КИ", DocumentFormat.JSON),
        LK_KM_CANCELLATION_CSV("LK_KM_CANCELLATION_CSV", "Списание ненанесенных КИ", DocumentFormat.CSV),
        LK_KM_CANCELLATION_XML("LK_KM_CANCELLATION_XML", "Списание ненанесенных КИ", DocumentFormat.XML),

        // Списание нанесенных КИ
        LK_APPLIED_KM_CANCELLATION("LK_APPLIED_KM_CANCELLATION", "Списание нанесенных КИ", DocumentFormat.JSON),
        LK_APPLIED_KM_CANCELLATION_CSV("LK_APPLIED_KM_CANCELLATION_CSV", "Списание нанесенных КИ", DocumentFormat.CSV),
        LK_APPLIED_KM_CANCELLATION_XML("LK_APPLIED_KM_CANCELLATION_XML", "Списание нанесенных КИ", DocumentFormat.XML),

        // Ввод в оборот товара (Контракт)
        LK_CONTRACT_COMMISSIONING("LK_CONTRACT_COMMISSIONING", "Ввод в оборот товара. Контракт", DocumentFormat.JSON),
        LK_CONTRACT_COMMISSIONING_CSV("LK_CONTRACT_COMMISSIONING_CSV", "Ввод в оборот товара. Контракт", DocumentFormat.CSV),
        LK_CONTRACT_COMMISSIONING_XML("LK_CONTRACT_COMMISSIONING_XML", "Ввод в оборот товара. Контракт", DocumentFormat.XML),

        // Ввод в оборот товара (ФизЛицо)
        LK_INDI_COMMISSIONING("LK_INDI_COMMISSIONING", "Ввод в оборот товара. ФизЛицо", DocumentFormat.JSON),
        LK_INDI_COMMISSIONING_CSV("LK_INDI_COMMISSIONING_CSV", "Ввод в оборот товара. ФизЛицо", DocumentFormat.CSV),
        LK_INDI_COMMISSIONING_XML("LK_INDI_COMMISSIONING_XML", "Ввод в оборот товара. ФизЛицо", DocumentFormat.XML),

        // Вывод отгрузкой
        LP_SHIP_RECEIPT("LP_SHIP_RECEIPT", "Вывод отгрузкой", DocumentFormat.JSON),
        LP_SHIP_RECEIPT_CSV("LP_SHIP_RECEIPT_CSV", "Вывод отгрузкой", DocumentFormat.CSV),
        LP_SHIP_RECEIPT_XML("LP_SHIP_RECEIPT_XML", "Вывод отгрузкой", DocumentFormat.XML),

        // Описание остатков товара
        OST_DESCRIPTION("OST_DESCRIPTION", "Описание остатков товара", DocumentFormat.JSON),
        OST_DESCRIPTION_CSV("OST_DESCRIPTION_CSV", "Описание остатков товара", DocumentFormat.CSV),
        OST_DESCRIPTION_XML("OST_DESCRIPTION_XML", "Описание остатков товара", DocumentFormat.XML),

        // Трансграничная торговля
        CROSSBORDER("CROSSBORDER", "Трансгран", DocumentFormat.JSON),
        CROSSBORDER_CSV("CROSSBORDER_CSV", "Трансгран", DocumentFormat.CSV),
        CROSSBORDER_XML("CROSSBORDER_XML", "Трансгран", DocumentFormat.XML),

        // Ввод в оборот остатков
        LP_INTRODUCE_OST("LP_INTRODUCE_OST", "Ввод в оборот остатков", DocumentFormat.JSON),
        LP_INTRODUCE_OST_CSV("LP_INTRODUCE_OST_CSV", "Ввод в оборот остатков", DocumentFormat.CSV),
        LP_INTRODUCE_OST_XML("LP_INTRODUCE_OST_XML", "Ввод в оборот остатков", DocumentFormat.XML),

        // Возврат в оборот
        LP_RETURN("LP_RETURN", "Возврат в оборот", DocumentFormat.JSON),
        LP_RETURN_CSV("LP_RETURN_CSV", "Возврат в оборот", DocumentFormat.CSV),
        LP_RETURN_XML("LP_RETURN_XML", "Возврат в оборот", DocumentFormat.XML),

        // Отгрузка при трансграничной торговле
        LP_SHIP_GOODS_CROSSBORDER("LP_SHIP_GOODS_CROSSBORDER", "Отгрузка при трансграничной торговле", DocumentFormat.JSON),
        LP_SHIP_GOODS_CROSSBORDER_CSV("LP_SHIP_GOODS_CROSSBORDER_CSV", "Отгрузка при трансграничной торговле", DocumentFormat.CSV),
        LP_SHIP_GOODS_CROSSBORDER_XML("LP_SHIP_GOODS_CROSSBORDER_XML", "Отгрузка при трансграничной торговле", DocumentFormat.XML),

        // Отмена отгрузки при трансграничной торговле
        LP_CANCEL_SHIPMENT_CROSSBORDER("LP_CANCEL_SHIPMENT_CROSSBORDER", "Отмена отгрузки при трансграничной торговле. Производство РФ", DocumentFormat.JSON);

        private final String code;
        private final String description;
        private final DocumentFormat format;

        DocumentType(String code, String description, DocumentFormat format) {
            this.code = code;
            this.description = description;
            this.format = format;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public DocumentFormat getFormat() {
            return format;
        }
    }

    public enum DocumentFormat {
        JSON("MANUAL", "формат json"),
        CSV("CSV", " формат csv"),
        XML("XML", "формат xml");

        private final String code;
        private final String description;

        DocumentFormat(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ProductGroup {
        CLOTHES("clothes", "Предметы одежды, белье постельное, столовое, туалетное и кухонное"),
        SHOES("shoes", "Обувные товары"),
        TOBACCO("tobacco", "Табачная продукция"),
        PERFUMERY("perfumery", "Духи и туалетная вода"),
        TIRES("tires", "Шины и покрышки пневматические резиновые новые"),
        ELECTRONICS("electronics", "Фотокамеры (кроме кинокамер), фотовспышки и лампы-вспышки"),
        PHARMA("pharma", "Лекарственные препараты для медицинского применения"),
        MILK("milk", "Молочная продукция"),
        BICYCLE("bicycle", "Велосипеды и велосипедные рамы"),
        WHEELCHAIRS("wheelchairs", "Кресла-коляски");

        private final String code;
        private final String description;

        ProductGroup(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static ProductGroup fromCode(String code) {
            for (ProductGroup group : values()) {
                if (group.code.equals(code)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("Unknown product group code: " + code);
        }
    }

    public enum ProductionType {
        OWN("OWN_PRODUCTION", "Собственное производство"),
        CONTRACT("CONTRACT_PRODUCTION", "Производство товара по договору");

        private final String code;
        private final String description;

        ProductionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    record Pair<T, U>(T first, U second) {
    }

}