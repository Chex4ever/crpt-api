package ru.selsup.trueapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.selsup.crypto.JcspSigningService;
import ru.selsup.crypto.SigningException;
import ru.selsup.trueapi.model.AuthDataPair;
import ru.selsup.trueapi.model.Document;
import ru.selsup.trueapi.model.Organization;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    public static final Logger logger = LoggerFactory.getLogger(CrptApi.class);
    public static final ObjectMapper mapper = new ObjectMapper();
    private static final Duration TOKEN_TTL = Duration.ofHours(10);
    private final JcspSigningService signer;
    private final TokenStorage tokenStorage;
    private final String baseUrlV3;
    private final String baseUrlV4;
    private final String authGetDataEndpoint;
    private final String authGetTokenEndpoint;
    private final String createDocumentEndpoint;

    private final HttpClient httpClient;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition resetCondition = lock.newCondition();
    private final AtomicLong requestsCounter = new AtomicLong(0);
    private final Duration periodDuration;
    private final int requestLimit;
    private final ScheduledExecutorService scheduler;
    private String authToken;

    public CrptApi(JcspSigningService signer, Config config, TokenStorage tokenStorage) {
        this.signer = signer;
        this.tokenStorage = tokenStorage;
        logger.info("Инициализирую CrptApi с конфигурацией: {}", config);
        if (config == null) {
            logger.error("Нет конфигурации");
            throw new IllegalArgumentException("Неверная конфигурация");
        }
        this.baseUrlV3 = config.getBaseUrlV3();
        this.baseUrlV4 = config.getBaseUrlV4();
        this.authGetTokenEndpoint = config.getAuthGetTokenEndpoint();
        this.authGetDataEndpoint = config.getAuthGetDataEndpoint();
        this.createDocumentEndpoint = config.getCreateDocumentEndpoint();

        this.requestLimit = config.getRequestLimit();
        this.periodDuration = Duration.of(config.getTimeUnit().toMillis(1), TimeUnit.MILLISECONDS.toChronoUnit());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.httpClient = HttpClient.newBuilder().connectTimeout(config.getConnectionTimeout()).build();
        schedulePeriodicCounterReset();
        logger.debug("CrptApi успешно инициализирован. Лимит запросов: {}, период: {}", requestLimit, periodDuration.toSeconds());
    }


    public synchronized CompletableFuture<Void> authenticate() {
        if (tokenStorage.isValid()) {
            logger.info("Использую сохранённый токен, годен до: {}", tokenStorage.getExpirationTime());
            authToken = tokenStorage.getToken();
            return CompletableFuture.completedFuture(null);
        }
        logger.info("Начинаю процесс авторизации");
        try {
            return fetchUuidAndDataFromServer()
                    .thenCompose(this::signData)
                    .thenCompose(this::sendSignedDataToServer)
                    .thenRun(() -> logger.info("Авторизация прошла успешна"));
        } catch (Exception e) {
            logger.error("Ошибка авторизации", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<AuthDataPair> fetchUuidAndDataFromServer() throws InterruptedException {
        acquireRequestPermit();
        logger.info("Запрашиваю UUID и data для авторизации с сервера.");
        URI uri = URI.create(baseUrlV3 + authGetDataEndpoint);
        HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                logger.error("Ошибка. Не удалось запросить uuid и data: {}", response.statusCode());
                throw new RuntimeException("Ошибка. Не удалось запросить uuid и data: " + response.statusCode());
            }
            try {
                ObjectNode root = mapper.readValue(response.body(), ObjectNode.class);
                String uuid = root.get("uuid").asText();
                String data = root.get("data").asText();
                logger.info("Получил uuid={}, data={}", uuid, data);
                return new AuthDataPair(uuid, data);
            } catch (JsonProcessingException e) {
                logger.error("Ошибка. Не могу прочитать ответ: body:{}, headers:{}", response.body(), response.headers(), e);

                throw new RuntimeException(e);
            }
        }).exceptionally(throwable -> {
            logger.error("Ошибка запроса UUID и data с сервера авторизации", throwable);

            throw new CompletionException(throwable);
        });
    }

    private CompletableFuture<AuthDataPair> signData(AuthDataPair authDataPair) {
        String uuid = authDataPair.uuid;
        String data = authDataPair.data;
        try {
            return CompletableFuture.completedFuture(new AuthDataPair(uuid, signer.signData(data, false)));
        } catch (SigningException e) {
            logger.warn("Ошибка создания присоединенной подписи", e);
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Void> sendSignedDataToServer(AuthDataPair authDataPair) {
        try {
            acquireRequestPermit();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String uuid = authDataPair.uuid;
        String signedData = authDataPair.data;
        if (!isValidBase64(signedData)) {
            logger.error("Ошибка. Данные не в формате base64!");
            throw new RuntimeException("Invalid base64 format");
        }
        logger.info("Посылаю подписанную data на сервер.");
        ObjectNode jsonBody = mapper.createObjectNode();
        jsonBody.put("uuid", uuid).put("data", signedData);
        URI uri = URI.create(baseUrlV3 + authGetTokenEndpoint);
        String body = jsonBody.toString();
        HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Content-Type", "application/json").header("charset'", "UTF-8").POST(HttpRequest.BodyPublishers.ofString(body)).build();

        logger.debug("Посылаю запрос авторизации. адрес: {}, body: {}, headers: {}", uri, HttpRequest.BodyPublishers.ofString(body), request.headers());

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() != 200) {
                logger.error("Ошиюка. Авторизация сломалась. Код: {}, body: {}, headers: {}", response.statusCode(), response.body(), response.headers());
                throw new RuntimeException("Authentication failed: " + response.statusCode());
            }
            try {
                ObjectNode root = mapper.readValue(response.body(), ObjectNode.class);
                authToken = root.get("token").asText();

                Instant expirationTime = Instant.now().plus(TOKEN_TTL);
                tokenStorage.saveToken(authToken, expirationTime);

                logger.info("Успешная авторизация. Токен: {}", authToken);
            } catch (JsonProcessingException e) {
                logger.error("Ошибка чтения ответа:", e);
                throw new RuntimeException(e);
            }
        }).exceptionally(throwable -> {
            logger.error("Ошибка авторизации.", throwable);
            throw new CompletionException(throwable);
        });
    }

    private boolean isValidBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public CompletableFuture<String> sendDocument(Document document) throws JsonProcessingException, InterruptedException {
        logger.info("Создаём документ: {}", document.getType());
        logger.debug("Документ: {}", document);
        acquireRequestPermit();

        String jsonDocument = mapper.writeValueAsString(document);
        logger.debug("json: {}", jsonDocument);
        String base64Document = Base64.getEncoder().encodeToString(jsonDocument.getBytes(StandardCharsets.UTF_8));
        ObjectNode requestBody = mapper.createObjectNode();
        String signature;
        try {
            signature = signer.signData(jsonDocument, true);
            if (signature == null) {
                logger.error("Нет подписи");
                throw new SigningException("Signature is null");
            }
        } catch (SigningException e) {
            logger.error("Ошибка подписания документа", e);
            throw new RuntimeException("Document signing failed", e);
        }
        String base64Signature = Base64.getEncoder().encodeToString(signature.getBytes(StandardCharsets.UTF_8));
        requestBody.put("document_format", document.getType().getFormat().getCode())
                .put("product_document", base64Document)
                .put("type", document.getType().getCode())
                .put("signature", base64Signature);
        URI uri = URI.create(baseUrlV3 + createDocumentEndpoint);
//        URI uri = URI.create(baseUrlV3 + createDocumentEndpoint + "?pg=" + document.getProductGroup().getCode());
        HttpRequest request = HttpRequest.newBuilder().uri(uri)
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        logger.debug("Посылаю документ: {}, токен: {}", uri, authToken);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).handle((response, throwable) -> {
            if (throwable != null) {
                logger.error("Document creation request failed", throwable);
                throw new CompletionException(throwable);
            }
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                logger.error("Document creation failed with status: {}. Response: {}", response.statusCode(), response.body());
                throw new CompletionException(new IOException("Create document failed: " + response.statusCode() + ", " + response.body()));
            }
            logger.info("Document created successfully. Status: {}", response.statusCode());
            logger.debug("Response body: {}", response.body());
            return response.body();
        });
    }

    public CompletableFuture<Organization> checkParticipantsByINN(String inn) throws JsonProcessingException, InterruptedException {
        logger.info("Проверяеи участников по ИНН: {}", inn);
        acquireRequestPermit();
        URI uri = URI.create(baseUrlV3 + "/participants" + "?inns=" + inn);
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + authToken)
                .header("accept", "application/json")
                .GET().uri(uri)
                .build();
        logger.debug("Посылаю запрос на поиск участников по ИНН: {}, токен: {}", uri, authToken);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                logger.error("Ошибка. Не удалось запросить участников по ИНН: код:{}, ошибка: {}", response.statusCode(), response.body());
                throw new RuntimeException("Check participants by INN request failure: " + response.statusCode() + response.body());
            }
            try {
                ObjectNode root = mapper.readValue(response.body(), ObjectNode.class);
                Organization org = new Organization(
                        root.get("inn").asText(),
                        root.get("status").asText(),
                        root.get("productGroups").asText(),
                        root.get("is_registered").asText(),
                        root.get("is_kfh").asText()
                );
                logger.info("Получил организацию: {}", org);
                return org;
            } catch (JsonProcessingException e) {
                logger.error("Ошибка. Не могу прочитать ответ: body:{}, headers:{}", response.body(), response.headers(), e);
                throw new RuntimeException(e);
            }
        });
    }

    public void resetRateLimit() {
        lock.lock();
        try {
            long currentCount = requestsCounter.get();
            if (currentCount > 0) {
                logger.info("Manual reset: counter {} → 0", currentCount);
                requestsCounter.set(0);
                resetCondition.signalAll(); // ← ВАЖНО: пробуждаем ждущие потоки!
            }
        } finally {
            lock.unlock();
        }
    }

    private void acquireRequestPermit() throws InterruptedException {
        lock.lock();
        try {
            while (true) {
                long currentCount = requestsCounter.get();

                if (currentCount < requestLimit) {
                    requestsCounter.incrementAndGet();
                    logger.debug("Request permitted. Current count: {}", currentCount + 1);
                    return;
                }
                logger.warn("Rate limit exceeded ({} >= {}). Waiting for reset...",
                        currentCount, requestLimit);
                boolean signaled = resetCondition.await(periodDuration.toMillis(), TimeUnit.MILLISECONDS);
                if (signaled) {
                    logger.debug("Awaken by reset signal");
                } else {
                    logger.debug("Awaken by timeout");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void schedulePeriodicCounterReset() {
        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                long currentCount = requestsCounter.get();
                if (currentCount > 0) {
                    logger.debug("Periodic reset: counter {} → 0", currentCount);
                    requestsCounter.set(0);
                    resetCondition.signalAll(); // ← ПРОБУЖДАЕМ все ожидающие потоки!
                }
            } finally {
                lock.unlock();
            }
        }, periodDuration.toMillis(), periodDuration.toMillis(), TimeUnit.MILLISECONDS);
    }


//    private void schedulePeriodicCounterReset() {
//        scheduler.schedule(() -> {
//            lock.lock();
//            try {
//                long currentCount = requestsCounter.get();
//                requestsCounter.set(0);
//                logger.debug("Сброс счётчика {} на 0", currentCount);
//            } finally {
//                lock.unlock();
//            }
//        }, periodDuration.toMillis(), TimeUnit.MILLISECONDS);
//    }

//    private void acquireRequestPermit() throws InterruptedException {
//        lock.lock();
//        try {
//            long currentCount = requestsCounter.incrementAndGet();
//            logger.debug("Current request count: {}", currentCount);
//
//            while (currentCount > requestLimit) {
//                logger.warn("Rate limit exceeded ({} > {}). Waiting for reset...", currentCount, requestLimit);
//                Thread.sleep(periodDuration.toMillis());
//                requestsCounter.set(0);
//                currentCount = requestsCounter.incrementAndGet();
//                logger.debug("Request counter reset. New count: {}", requestsCounter.get());
//
//            }
//        } finally {
//            lock.unlock();
//        }
//    }


}