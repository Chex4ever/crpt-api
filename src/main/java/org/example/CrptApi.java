package org.example;
import okhttp3.OkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    public static final HttpClient httpClient = HttpClient.newHttpClient();
    public static final ObjectMapper mapper = new ObjectMapper();
    public static final String BASE_URL = "https://ismp.crpt.ru/api/v3";
    public static final String AUTH_ENDPOINT = "/auth/cert";
    public static final String CREATE_DOCUMENT_ENDPOINT = "/lk/documents/create";

    private final Semaphore semaphore;
    private final long periodMillis;
    private final ScheduledExecutorService scheduler;
    private final RateLimiter rateLimiter;
    private String authToken;

    public CrptApi (TimeUnit timeUnit, int requestLimit){
        if (requestLimit <= 0 || timeUnit == null) {
            throw new IllegalArgumentException("Invalid parameters");
        }

        this.periodMillis = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.rateLimiter = new RateLimiter(scheduler, requestLimit, periodMillis);
    }

    public synchronized void authenticate (String username, String password) throws NoSuchAlgorithmException {
        UUID uuid = UUID.randomUUID();
        String data = generateRandomData(uuid);
        String signedData = signData(data, password);

//        RequestBody body = new FormBody.Builder()
//                .add("uuid", uuid.toString())
//                .add("data", signedData)
//                .build();
//        HttpRequest request = HttpRequest.Builder()
//                url
    }

    private String generateRandomData(UUID uuid){
        return uuid.toString() + "-" + System.currentTimeMillis();
    }

    private String signData(String data, String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest((data+password).getBytes(StandardCharsets.UTF_8));
        return  Base64.getEncoder().encodeToString(hash);
    }

    private String encodeSignature(String signature){
        return Base64.getEncoder().encodeToString(signature.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeSignature(Document document) {
        return Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(document));
    }

    private static class RateLimiter {
        private final ScheduledExecutorService scheduler;
        private final int permitsPerPeriod;
        private final long periodMillis;
        private volatile long lastRefillTimestamp;
        private volatile long availablePermits;

        public RateLimiter(ScheduledExecutorService scheduler, int permitsPerPeriod, long periodMillis) {
            this.scheduler = scheduler;
            this.permitsPerPeriod = permitsPerPeriod;
            this.periodMillis = periodMillis;
            this.lastRefillTimestamp = System.currentTimeMillis();
            this.availablePermits = permitsPerPeriod;
            scheduleRefill();
        }

        private synchronized void acquire() throws InterruptedException{
            while (availablePermits < 1){
                wait();
            }
            availablePermits--;
        }


        private synchronized void refill() {
            long currentTime = System.currentTimeMillis();
            long elapsedMillis = currentTime - lastRefillTimestamp;
            double refilledPermits = Math.min(elapsedMillis * permitsPerPeriod / (double)periodMillis, permitsPerPeriod);
            availablePermits += refilledPermits;
            availablePermits = Math.min(availablePermits, permitsPerPeriod);
            lastRefillTimestamp = currentTime;
            notifyAll();
            scheduleRefill();
        }

        private void scheduleRefill() {
            scheduler.schedule(this::refill, periodMillis - (System.currentTimeMillis() % periodMillis), TimeUnit.MILLISECONDS);
        }

    }

    public static class Product{
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

    public static abstract class Document {
        protected String documentFormat;
        protected String productGroup;
        protected String type;
        protected String signature;

        public Document(String documentFormat, String productGroup, String type, String signature) {
            this.documentFormat = documentFormat;
            this.productGroup = productGroup;
            this.type = type;
            this.signature = signature;
        }

        public String getDocumentFormat() {
            return documentFormat;
        }

        public void setDocumentFormat(String documentFormat) {
            this.documentFormat = documentFormat;
        }

        public String getProductGroup() {
            return productGroup;
        }

        public void setProductGroup(String productGroup) {
            this.productGroup = productGroup;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }
    public static class IntroduceGoodsDocument extends Document {
        private String participantInn;
        private List<Product> products;

        public IntroduceGoodsDocument(String documentFormat, String productGroup, String type, String signature, String participantInn, List<Product> products) {
            super(documentFormat, productGroup, type, signature);
            this.participantInn = participantInn;
            this.products = products;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }
    }
}
//deepseek:
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.Base64;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//public class CrptApi {
//
//    private final Object lock = new Object();
//    private final long intervalMillis;
//    private final int requestLimit;
//    private final String productGroup;
//    private long lastResetTime;
//    private int requestCount;
//
//    public CrptApi(TimeUnit timeUnit, int requestLimit, String productGroup) {
//        this.intervalMillis = timeUnit.toMillis(1);
//        this.requestLimit = requestLimit;
//        this.productGroup = productGroup;
//        this.lastResetTime = System.currentTimeMillis();
//        this.requestCount = 0;
//    }
//
//    public void createDocument(Document document, String signature) {
//        synchronized (lock) {
//            long currentTime = System.currentTimeMillis();
//            if (currentTime - lastResetTime >= intervalMillis) {
//                requestCount = 0;
//                lastResetTime = currentTime;
//            }
//
//            while (requestCount >= requestLimit) {
//                long waitTime = intervalMillis - (currentTime - lastResetTime);
//                if (waitTime <= 0) {
//                    requestCount = 0;
//                    lastResetTime = currentTime;
//                    break;
//                }
//                try {
//                    lock.wait(waitTime);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("Interrupted while waiting for rate limit", e);
//                }
//                currentTime = System.currentTimeMillis();
//            }
//            requestCount++;
//        }
//
//        try {
//            String documentJson = document.toJson();
//            String base64Document = Base64.getEncoder().encodeToString(documentJson.getBytes());
//
//            String requestBody = String.format(
//                    "{\"document_format\":\"MANUAL\",\"product_document\":\"%s\",\"product_group\":\"%s\",\"signature\":\"%s\",\"type\":\"LP_INTRODUCE_GOODS\"}",
//                    base64Document, productGroup, signature
//            );
//
//            HttpClient client = HttpClient.newBuilder()
//                    .connectTimeout(Duration.ofSeconds(10))
//                    .build();
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//                    .build();
//
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() != 200) {
//                throw new RuntimeException("HTTP error: " + response.statusCode() + " - " + response.body());
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to create document", e);
//        }
//    }
//
//    public static class Document {
//        private Description description;
//        private String doc_id;
//        private String doc_status;
//        private String doc_type;
//        private boolean importRequest;
//        private String owner_inn;
//        private String participant_inn;
//        private String producer_inn;
//        private String production_date;
//        private String production_type;
//        private List<Product> products;
//        private String reg_date;
//        private String reg_number;
//
//        public String toJson() {
//            StringBuilder sb = new StringBuilder();
//            sb.append("{");
//            appendField(sb, "description", description);
//            appendField(sb, "doc_id", doc_id);
//            appendField(sb, "doc_status", doc_status);
//            appendField(sb, "doc_type", doc_type);
//            appendField(sb, "importRequest", importRequest);
//            appendField(sb, "owner_inn", owner_inn);
//            appendField(sb, "participant_inn", participant_inn);
//            appendField(sb, "producer_inn", producer_inn);
//            appendField(sb, "production_date", production_date);
//            appendField(sb, "production_type", production_type);
//            appendField(sb, "products", products);
//            appendField(sb, "reg_date", reg_date);
//            appendField(sb, "reg_number", reg_number);
//            if (sb.charAt(sb.length() - 1) == ',') {
//                sb.setLength(sb.length() - 1);
//            }
//            sb.append("}");
//            return sb.toString();
//        }
//
//        private void appendField(StringBuilder sb, String name, Object value) {
//            if (value != null) {
//                sb.append("\"").append(name).append("\":");
//                if (value instanceof String) {
//                    sb.append("\"").append(value).append("\"");
//                } else if (value instanceof Boolean) {
//                    sb.append(value);
//                } else if (value instanceof List) {
//                    sb.append("[");
//                    List<?> list = (List<?>) value;
//                    for (Object item : list) {
//                        if (item instanceof Product) {
//                            sb.append(((Product) item).toJson());
//                        }
//                        sb.append(",");
//                    }
//                    if (!list.isEmpty()) {
//                        sb.setLength(sb.length() - 1);
//                    }
//                    sb.append("]");
//                } else if (value instanceof Description) {
//                    sb.append(((Description) value).toJson());
//                }
//                sb.append(",");
//            }
//        }
//
//        // Геттеры и сеттеры
//        public Description getDescription() { return description; }
//        public void setDescription(Description description) { this.description = description; }
//        public String getDoc_id() { return doc_id; }
//        public void setDoc_id(String doc_id) { this.doc_id = doc_id; }
//        public String getDoc_status() { return doc_status; }
//        public void setDoc_status(String doc_status) { this.doc_status = doc_status; }
//        public String getDoc_type() { return doc_type; }
//        public void setDoc_type(String doc_type) { this.doc_type = doc_type; }
//        public boolean isImportRequest() { return importRequest; }
//        public void setImportRequest(boolean importRequest) { this.importRequest = importRequest; }
//        public String getOwner_inn() { return owner_inn; }
//        public void setOwner_inn(String owner_inn) { this.owner_inn = owner_inn; }
//        public String getParticipant_inn() { return participant_inn; }
//        public void setParticipant_inn(String participant_inn) { this.participant_inn = participant_inn; }
//        public String getProducer_inn() { return producer_inn; }
//        public void setProducer_inn(String producer_inn) { this.producer_inn = producer_inn; }
//        public String getProduction_date() { return production_date; }
//        public void setProduction_date(String production_date) { this.production_date = production_date; }
//        public String getProduction_type() { return production_type; }
//        public void setProduction_type(String production_type) { this.production_type = production_type; }
//        public List<Product> getProducts() { return products; }
//        public void setProducts(List<Product> products) { this.products = products; }
//        public String getReg_date() { return reg_date; }
//        public void setReg_date(String reg_date) { this.reg_date = reg_date; }
//        public String getReg_number() { return reg_number; }
//        public void setReg_number(String reg_number) { this.reg_number = reg_number; }
//    }
//
//    public static class Description {
//        private String participant_inn;
//
//        public String toJson() {
//            return "{\"participant_inn\":\"" + participant_inn + "\"}";
//        }
//
//        public String getParticipant_inn() { return participant_inn; }
//        public void setParticipant_inn(String participant_inn) { this.participant_inn = participant_inn; }
//    }
//
//    public static class Product {
//        private String certificate_document;
//        private String certificate_document_date;
//        private String certificate_document_number;
//        private String owner_inn;
//        private String producer_inn;
//        private String production_date;
//        private String tnved_code;
//        private String uit_code;
//        private String uitu_code;
//
//        public String toJson() {
//            StringBuilder sb = new StringBuilder();
//            sb.append("{");
//            appendField(sb, "certificate_document", certificate_document);
//            appendField(sb, "certificate_document_date", certificate_document_date);
//            appendField(sb, "certificate_document_number", certificate_document_number);
//            appendField(sb, "owner_inn", owner_inn);
//            appendField(sb, "producer_inn", producer_inn);
//            appendField(sb, "production_date", production_date);
//            appendField(sb, "tnved_code", tnved_code);
//            appendField(sb, "uit_code", uit_code);
//            appendField(sb, "uitu_code", uitu_code);
//            if (sb.charAt(sb.length() - 1) == ',') {
//                sb.setLength(sb.length() - 1);
//            }
//            sb.append("}");
//            return sb.toString();
//        }
//
//        private void appendField(StringBuilder sb, String name, String value) {
//            if (value != null) {
//                sb.append("\"").append(name).append("\":\"").append(value).append("\",");
//            }
//        }
//
//        // Геттеры и сеттеры
//        public String getCertificate_document() { return certificate_document; }
//        public void setCertificate_document(String certificate_document) { this.certificate_document = certificate_document; }
//        public String getCertificate_document_date() { return certificate_document_date; }
//        public void setCertificate_document_date(String certificate_document_date) { this.certificate_document_date = certificate_document_date; }
//        public String getCertificate_document_number() { return certificate_document_number; }
//        public void setCertificate_document_number(String certificate_document_number) { this.certificate_document_number = certificate_document_number; }
//        public String getOwner_inn() { return owner_inn; }
//        public void setOwner_inn(String owner_inn) { this.owner_inn = owner_inn; }
//        public String getProducer_inn() { return producer_inn; }
//        public void setProducer_inn(String producer_inn) { this.producer_inn = producer_inn; }
//        public String getProduction_date() { return production_date; }
//        public void setProduction_date(String production_date) { this.production_date = production_date; }
//        public String getTnved_code() { return tnved_code; }
//        public void setTnved_code(String tnved_code) { this.tnved_code = tnved_code; }
//        public String getUit_code() { return uit_code; }
//        public void setUit_code(String uit_code) { this.uit_code = uit_code; }
//        public String getUitu_code() { return uitu_code; }
//        public void setUitu_code(String uitu_code) { this.uitu_code = uitu_code; }
//    }
//}
//Этот код реализует класс CrptApi с thread-safe ограничением количества запросов к API. Основные особенности:
//Использует synchronized блок для управления лимитом запросов
//Реализует простой JSON serialization через StringBuilder
//Использует стандартный HttpClient из Java 11
//Включает все необходимые вложенные классы для документа
//Кодирует документ в base64 перед отправкой
//Обрабатывает ошибки HTTP запросов
//Для использования создайте экземпляр CrptApi, задав лимит запросов и продуктовую группу, затем вызывайте createDocument с объектом документа и подписью.


//gigachat:
//import java.time.Duration;
//import java.util.concurrent.*;
//        import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import javax.crypto.Cipher;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.Base64;
//import java.util.UUID;
//
//public class CrptApi {
//    private static final OkHttpClient httpClient = new OkHttpClient();
//    private static final ObjectMapper mapper = new ObjectMapper();
//    private static final String BASE_URL = "https://ismp.crpt.ru/api/v3/";
//    private static final String AUTH_ENDPOINT = "/auth/cert/";
//    private static final String CREATE_DOCUMENT_ENDPOINT = "/lk/documents/create";
//
//    private final Semaphore semaphore;
//    private final long periodMillis;
//    private final ScheduledExecutorService scheduler;
//    private final RateLimiter rateLimiter;
//    private String authToken;
//
//    public CrptApi(TimeUnit timeUnit, int requestLimit) {
//        if (requestLimit <= 0 || timeUnit == null) {
//            throw new IllegalArgumentException("Invalid parameters");
//        }
//
//        this.periodMillis = timeUnit.toMillis(1);
//        this.semaphore = new Semaphore(requestLimit);
//        this.scheduler = Executors.newSingleThreadScheduledExecutor();
//        this.rateLimiter = new RateLimiter(scheduler, requestLimit, periodMillis);
//    }
//
//    public synchronized void authenticate(String username, String password) throws Exception {
//        UUID uuid = UUID.randomUUID();
//        String data = generateRandomData(uuid);
//        String signedData = signData(data, password);
//
//        RequestBody body = new FormBody.Builder()
//                .add("uuid", uuid.toString())
//                .add("data", signedData)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(BASE_URL + AUTH_ENDPOINT)
//                .post(body)
//                .build();
//
//        try (Response response = httpClient.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Authentication failed: " + response.code());
//            }
//            authToken = response.body().string();
//        }
//    }
//
//    public synchronized String createDocument(Document document, String signature) throws Exception {
//        rateLimiter.acquire();
//        semaphore.acquire();
//
//        try {
//            String encodedDocument = encodeDocument(document);
//            String encodedSignature = encodeSignature(signature);
//
//            JSONObject requestBody = new JSONObject();
//            requestBody.put("product_document", encodedDocument);
//            requestBody.put("document_format", "MANUAL");
//            requestBody.put("type", "LP_INTRODUCE_GOODS");
//            requestBody.put("signature", encodedSignature);
//
//            Request request = new Request.Builder()
//                    .url(BASE_URL + CREATE_DOCUMENT_ENDPOINT)
//                    .post(RequestBody.create(MediaType.get("application/json"), requestBody.toString()))
//                    .header("Authorization", "Bearer " + authToken)
//                    .build();
//
//            try (Response response = httpClient.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new IOException("Create document failed: " + response.code() + ", " + response.body().string());
//                }
//                return response.body().string();
//            }
//        } finally {
//            semaphore.release();
//        }
//    }
//
//    private String generateRandomData(UUID uuid) {
//        return uuid.toString() + "-" + System.currentTimeMillis();
//    }
//
//    private String signData(String data, String password) throws NoSuchAlgorithmException {
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        byte[] hash = md.digest((data + password).getBytes(StandardCharsets.UTF_8));
//        return Base64.getEncoder().encodeToString(hash);
//    }
//
//    private String encodeDocument(Document document) throws JsonProcessingException {
//        return Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(document));
//    }
//
//    private String encodeSignature(String signature) {
//        return Base64.getEncoder().encodeToString(signature.getBytes(StandardCharsets.UTF_8));
//    }
//
//    private static class RateLimiter {
//        private final ScheduledExecutorService scheduler;
//        private final int permitsPerPeriod;
//        private final long periodMillis;
//        private volatile long lastRefillTimestamp;
//        private volatile double availablePermits;
//
//        public RateLimiter(ScheduledExecutorService scheduler, int permitsPerPeriod, long periodMillis) {
//            this.scheduler = scheduler;
//            this.permitsPerPeriod = permitsPerPeriod;
//            this.periodMillis = periodMillis;
//            this.lastRefillTimestamp = System.currentTimeMillis();
//            this.availablePermits = permitsPerPeriod;
//            scheduleRefill();
//        }
//
//        public synchronized void acquire() throws InterruptedException {
//            while (availablePermits < 1) {
//                wait();
//            }
//            availablePermits--;
//        }
//
//        private synchronized void refill() {
//            long currentTime = System.currentTimeMillis();
//            long elapsedMillis = currentTime - lastRefillTimestamp;
//            double refilledPermits = Math.min(elapsedMillis * permitsPerPeriod / (double)periodMillis, permitsPerPeriod);
//            availablePermits += refilledPermits;
//            availablePermits = Math.min(availablePermits, permitsPerPeriod);
//            lastRefillTimestamp = currentTime;
//            notifyAll();
//            scheduleRefill();
//        }
//
//        private void scheduleRefill() {
//            scheduler.schedule(this::refill, periodMillis - (System.currentTimeMillis() % periodMillis), TimeUnit.MILLISECONDS);
//        }
//    }
//
//    public static abstract class Document {
//        protected String documentFormat;
//        protected String productGroup;
//        protected String type;
//        protected String signature;
//
//        public Document(String documentFormat, String productGroup, String type, String signature) {
//            this.documentFormat = documentFormat;
//            this.productGroup = productGroup;
//            this.type = type;
//            this.signature = signature;
//        }
//
//        // Getters and setters for fields
//    }
//
//    public static class IntroduceGoodsDocument extends Document {
//        private String participantInn;
//        private List<Product> products;
//
//        public IntroduceGoodsDocument(String participantInn, List<Product> products) {
//            super("MANUAL", "milk", "LP_INTRODUCE_GOODS", "");
//            this.participantInn = participantInn;
//            this.products = products;
//        }
//
//        // Getters and setters for fields
//    }
//
//    public static class Product {
//        private String uitCode;
//        private String uituCode;
//        private String tnvedCode;
//        private String certificateDocument;
//        private String certificateDocumentNumber;
//        private String certificateDocumentDate;
//
//        public Product(String uitCode, String uituCode, String tnvedCode, String certificateDocument, String certificateDocumentNumber, String certificateDocumentDate) {
//            this.uitCode = uitCode;
//            this.uituCode = uituCode;
//            this.tnvedCode = tnvedCode;
//            this.certificateDocument = certificateDocument;
//            this.certificateDocumentNumber = certificateDocumentNumber;
//            this.certificateDocumentDate = certificateDocumentDate;
//        }
//
//        // Getters and setters for fields
//    }
//}
//}
//Объяснение реализации:
//Основные компоненты:
//Rate Limiter: Использован для ограничения частоты запросов к API. Реализован с помощью Semaphore и ScheduledExecutorService.
//Аутентификация: Метод authenticate() позволяет получать аутентификационный токен, используя предоставленные учетные данные.
//Создание документа: Метод createDocument() создает новый документ для ввода товара в оборот. Включает в себя кодирование документа и подписи перед отправкой.
//        Безопасность: Используются современные криптографические алгоритмы для безопасной передачи данных.
//Потокобезопасность: Класс спроектирован с учетом многопоточности благодаря использованию синхронизированных методов и объектов.
//Примеры использования:
//
//CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);
//crptApi.authenticate("username", "password");
//
//List<Product> products = Arrays.asList(new Product("UIT_CODE", "UITU_CODE", "TNVED_CODE", "CERTIFICATE_DOCUMENT", "CERTIFICATE_NUMBER", "CERTIFICATE_DATE"));
//IntroduceGoodsDocument document = new IntroduceGoodsDocument("PARTICIPANT_INN", products);
//
//try {
//String response = crptApi.createDocument(document, "SIGNATURE");
//    System.out.println(response);
//} catch (Exception e) {
//        e.printStackTrace();
//}
//Эта реализация обеспечивает надежный и безопасный доступ к API Честного знака с соблюдением всех требований, изложенных в документации