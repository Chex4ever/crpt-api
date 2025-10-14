package ru.selsup.trueapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.selsup.crypto.JcspSigningService;
import ru.selsup.crypto.SigningException;
import ru.selsup.trueapi.model.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrptApiTest {

    @Mock private JcspSigningService signer;
    @Mock private TokenStorage tokenStorage;
    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private CrptApi crptApi;
    private Config config;
    private ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() throws Exception {
        config = createTestConfig();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        crptApi = new CrptApi(signer, config, tokenStorage);
        injectMockHttpClient();
    }

    private Config createTestConfig() {
        Config config = new Config();
        config.setBaseUrlV3("https://api.test.com");
        config.setAuthGetDataEndpoint("/auth/data");
        config.setAuthGetTokenEndpoint("/auth/token");
        config.setCreateDocumentEndpoint("/documents");
        config.setRequestLimit(5);
        config.setTimeUnit(TimeUnit.SECONDS);
        config.setConnectionTimeout(Duration.ofSeconds(10));
        return config;
    }

    private void injectMockHttpClient() throws Exception {
        Field httpClientField = CrptApi.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(crptApi, httpClient);

        Field schedulerField = CrptApi.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(crptApi, scheduler);
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void whenConstructorWithNullConfig_thenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrptApi(signer, null, tokenStorage));
    }

    @Test
    void whenAuthenticateWithValidToken_thenReturnsCompletedFuture() {
        when(tokenStorage.isValid()).thenReturn(true);
        when(tokenStorage.getToken()).thenReturn("valid-token");
        when(tokenStorage.getExpirationTime()).thenReturn(Instant.now().plusSeconds(3600));

        CompletableFuture<Void> result = crptApi.authenticate();

        assertThat(result).isDone().isNotCompletedExceptionally();
        verify(tokenStorage).isValid();
    }

    @Test
    void whenAuthenticateWithInvalidToken_thenCompletesAuthFlow() throws SigningException {
        when(tokenStorage.isValid()).thenReturn(false);

        // Мокируем цепочку HTTP запросов
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(httpResponse));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"uuid\":\"test-uuid\",\"data\":\"test-data\"}");
        when(signer.signData(anyString(), anyBoolean())).thenReturn("signed-data");

        CompletableFuture<Void> result = crptApi.authenticate();

        assertThat(result).isNotNull();
        verify(httpClient, atLeastOnce()).sendAsync(any(HttpRequest.class), any());
    }

    @Test
    void whenSendDocument_thenReturnsSuccess() throws Exception {
//        when(tokenStorage.isValid()).thenReturn(true);
//        when(tokenStorage.getToken()).thenReturn("valid-token");

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(httpResponse));

        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\":\"doc-123\"}");
        when(signer.signData(anyString(), anyBoolean())).thenReturn("signature");

        Document document = createTestDocument();
        CompletableFuture<String> result = crptApi.sendDocument(document);

        assertThat(result).isNotNull();
        String response = result.get(5, TimeUnit.SECONDS);
        assertThat(response).contains("doc-123");
    }

    @Test
    void whenSendDocumentWithSigningError_thenThrowsException() throws Exception {
        when(tokenStorage.isValid()).thenReturn(true);
        when(tokenStorage.getToken()).thenReturn("valid-token");
        when(signer.signData(anyString(), anyBoolean()))
                .thenThrow(new ru.selsup.crypto.SigningException("Signing failed"));

        Document document = createTestDocument();

        assertThrows(RuntimeException.class, () -> crptApi.sendDocument(document));
    }

    @Test
    void whenCheckParticipantsByINN_thenReturnsOrganization() throws Exception {
        when(tokenStorage.isValid()).thenReturn(true);
        when(tokenStorage.getToken()).thenReturn("valid-token");

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(httpResponse));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(
                "{\"inn\":\"1234567890\",\"status\":\"ACTIVE\",\"productGroups\":\"group1\"," +
                        "\"is_registered\":\"true\",\"is_kfh\":\"false\"}"
        );

        CompletableFuture<Organization> future = crptApi.checkParticipantsByINN("1234567890");

        assertThat(future).isNotNull();
        Organization org = future.get(5, TimeUnit.SECONDS);
        assertThat(org.getInn()).isEqualTo("1234567890");
        assertThat(org.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void whenHttpRequestFails_thenCompletesExceptionally() throws JsonProcessingException, InterruptedException {
        when(tokenStorage.isValid()).thenReturn(true);
        when(tokenStorage.getToken()).thenReturn("valid-token");

        // Мокируем неудачный HTTP запрос
        CompletableFuture<HttpResponse<String>> failedFuture =
                CompletableFuture.failedFuture(new IOException("Connection failed"));

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(failedFuture);

        Document document = createTestDocument();
        CompletableFuture<String> result = crptApi.sendDocument(document);

        assertThat(result).isCompletedExceptionally();
    }

    private Document createTestDocument() {
        Document document = new Document();
        document.setProductGroup(ProductGroup.POLYMER);
        document.setParticipantInn("1234567890");
        document.setProducerInn("1234567890");
        document.setProductionDate("05.08.2025");
        document.setProductionType(ProductionType.CONTRACT.getCode());
        document.setType(DocumentType.LP_INTRODUCE_GOODS);
        document.setProducts(List.of(new Product(
                "010461111111111121LLLLLLLLLLLLL",
                null,
                "0000000000",
                List.of()
        )));
        return document;
    }
}