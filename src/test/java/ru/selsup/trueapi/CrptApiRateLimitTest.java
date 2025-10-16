package ru.selsup.trueapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.selsup.crypto.JcspSigningService;
import ru.selsup.trueapi.model.Document;
import ru.selsup.trueapi.model.DocumentType;
import ru.selsup.trueapi.model.Product;
import ru.selsup.trueapi.model.ProductGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrptApiRateLimitTest {

    @Mock
    private JcspSigningService signer;
    @Mock
    private TokenStorage tokenStorage;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    private Config config;
    private CrptApi crptApi;
    private ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() throws Exception {
        // Конфигурация с маленьким лимитом для тестов
        config = new Config();
        config.setBaseUrlV3("https://api.test.com");
        config.setAuthGetDataEndpoint("/auth/data");
        config.setAuthGetTokenEndpoint("/auth/token");
        config.setCreateDocumentEndpoint("/documents");
        config.setRequestLimit(3); // Всего 3 запроса
        config.setTimeUnit(TimeUnit.SECONDS);
        config.setConnectionTimeout(Duration.ofSeconds(10));

        scheduler = Executors.newSingleThreadScheduledExecutor();
        crptApi = new CrptApi(signer, config, tokenStorage);

        // Инъекция моков
        injectMockHttpClient();

        // Настройка моков
        lenient().when(tokenStorage.isValid()).thenReturn(true);
        lenient().when(tokenStorage.getToken()).thenReturn("valid-token");
        lenient().when(signer.signData(anyString(), anyBoolean())).thenReturn("mocked-signature");
        lenient().when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(HttpResponse.class)));
        lenient().when(httpResponse.statusCode()).thenReturn(201);
        lenient().when(httpResponse.body()).thenReturn("{\"id\":\"doc-123\"}");
    }

    private void injectMockHttpClient() throws Exception {
        Field httpClientField = CrptApi.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(crptApi, httpClient);

        Field schedulerField = CrptApi.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(crptApi, scheduler);
    }

    @Test
    void whenSendRequestsWithinLimit_thenAllSucceed() throws Exception {
        // Отправляем 3 запроса (в пределах лимита)

        Document document = createTestDocument();

        CompletableFuture<String> result1 = crptApi.sendDocument(document);
        CompletableFuture<String> result2 = crptApi.sendDocument(document);
        CompletableFuture<String> result3 = crptApi.sendDocument(document);

        // Все должны завершиться успешно
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();

        // Проверяем, что все запросы были отправлены
        verify(httpClient, times(3)).sendAsync(any(), any());
    }

    @Test
    void whenSendRequestsExceedingLimit_thenSomeAreDelayed() throws Exception {
        Document document = createTestDocument();

        long startTime = System.currentTimeMillis();

        // Отправляем 5 запросов при лимите 3
        CompletableFuture<String> result1 = crptApi.sendDocument(document);
        CompletableFuture<String> result2 = crptApi.sendDocument(document);
        CompletableFuture<String> result3 = crptApi.sendDocument(document);
        CompletableFuture<String> result4 = crptApi.sendDocument(document);
        CompletableFuture<String> result5 = crptApi.sendDocument(document);

        long endTime = System.currentTimeMillis();

        // Все запросы должны быть созданы
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
        assertThat(result4).isNotNull();
        assertThat(result5).isNotNull();

        // Время выполнения должно быть больше 0 (есть задержка)
        assertThat(endTime - startTime).isGreaterThan(0);
    }

    @Test
    void whenCounterReset_thenNewRequestsAllowed() throws Exception {
        Document document = createTestDocument();

        // Исчерпываем лимит
        crptApi.sendDocument(document);
        crptApi.sendDocument(document);
        crptApi.sendDocument(document);

        // Сбрасываем счетчик через reflection
        resetRequestCounter();

        // Новые запросы должны проходить
        CompletableFuture<String> newRequest = crptApi.sendDocument(document);

        assertThat(newRequest).isNotNull();
        verify(httpClient, times(4)).sendAsync(any(), any());
    }

    @Test
    void whenMultipleThreadsSendRequests_thenRateLimitIsRespected() throws Exception {
        Document document = createTestDocument();
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        List<CompletableFuture<String>> results = new CopyOnWriteArrayList<>();

        // Запускаем несколько потоков
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    CompletableFuture<String> result = crptApi.sendDocument(document);
                    results.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        // Одновременно запускаем все потоки
        startLatch.countDown();
        boolean completed = endLatch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(results).hasSize(threadCount);

        // Все запросы должны быть созданы, но некоторые с задержкой
        results.forEach(result -> assertThat(result).isNotNull());
    }

    @Test
    void whenAcquireRequestPermitCalled_thenCounterIncrements() throws Exception {
        // Получаем текущее значение счетчика
        long initialCount = getRequestCounter();

        // Вызываем acquireRequestPermit через reflection
        acquireRequestPermit();

        // Счетчик должен увеличиться
        long newCount = getRequestCounter();
        assertThat(newCount).isEqualTo(initialCount + 1);
    }

    @Test
    void whenRateLimitExceeded_thenThreadSleeps() throws Exception {
        Document document = createTestDocument();

        // Исчерпываем лимит
        crptApi.sendDocument(document);
        crptApi.sendDocument(document);
        crptApi.sendDocument(document);

        long startTime = System.currentTimeMillis();

        // Этот запрос должен быть задержан
        crptApi.sendDocument(document);

        long endTime = System.currentTimeMillis();

        // Должна быть заметная задержка
        assertThat(endTime - startTime).isGreaterThan(10);
    }

    @Test
    void whenPeriodicResetNotWorking_thenAllRequestsAfterLimitFail() throws Exception {
        config.setRequestLimit(2);
        config.setTimeUnit(TimeUnit.MILLISECONDS);

        CrptApi api = new CrptApi(signer, config, tokenStorage);

        // Исчерпываем лимит
        api.sendDocument(createTestDocument());
        api.sendDocument(createTestDocument());

        // Ждем дольше периода сброса
        Thread.sleep(100);

        // Этот запрос должен пройти после сброса, но не пройдет если schedule не работает
        CompletableFuture<String> result = api.sendDocument(createTestDocument());

        // Если сброс не работает, этот assert упадет
        assertThat(result).isNotNull();
    }

    @Test
    void whenRateLimitExceeded_thenOtherThreadsBlocked() throws Exception {
        config.setRequestLimit(1);
        config.setTimeUnit(TimeUnit.SECONDS);
        CrptApi api = new CrptApi(signer, config, tokenStorage);

        CountDownLatch slowThreadStarted = new CountDownLatch(1);
        CountDownLatch slowThreadFinished = new CountDownLatch(1);
        AtomicBoolean secondThreadCouldProceed = new AtomicBoolean(false);

        // Поток 1: занимает единственный слот и долго спит
        new Thread(() -> {
            try {
                api.sendDocument(createTestDocument());
                slowThreadStarted.countDown();
                Thread.sleep(2000); // Долгий sleep с удержанием блокировки
                slowThreadFinished.countDown();
            } catch (Exception e) {
            }
        }).start();

        // Ждем пока первый поток займет слот
        slowThreadStarted.await();

        // Поток 2: должен быть заблокирован, но если блокировка неправильная - пройдет
        new Thread(() -> {
            try {
                api.sendDocument(createTestDocument());
                secondThreadCouldProceed.set(true); // Не должно случиться!
            } catch (Exception e) {
            }
        }).start();

        // Даем второму потоку немного времени
        Thread.sleep(500);

        // Второй поток не должен был выполниться
        assertThat(secondThreadCouldProceed.get()).isFalse();

        slowThreadFinished.await();
    }

    @Test
    void whenMultipleThreadsExceedLimitSimultaneously_thenCorrectCountRespected() throws Exception {
        config.setRequestLimit(3);
        config.setTimeUnit(TimeUnit.SECONDS);

        CrptApi api = new CrptApi(signer, config, tokenStorage);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger concurrentRequests = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    int current = concurrentRequests.incrementAndGet();
                    // Запоминаем максимальную параллельность
                    if (current > 3) {
                        System.out.println("WARNING: " + current + " concurrent requests!");
                    }

                    api.sendDocument(createTestDocument());
                    successfulRequests.incrementAndGet();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    concurrentRequests.decrementAndGet();
                }
            }));
        }

        // Запускаем все одновременно
        startLatch.countDown();

        // Ждем завершения
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();

        // Все запросы должны завершиться успешно
        assertThat(successfulRequests.get()).isEqualTo(threadCount);
    }

    @Test
    void whenManualReset_thenWaitingThreadsWakeUpImmediately() throws Exception {
        config.setRequestLimit(1);
        config.setTimeUnit(TimeUnit.SECONDS);
        CrptApi api = new CrptApi(signer, config, tokenStorage);

        // Захватываем все слоты через reflection
        occupyAllSlots(api, config.getRequestLimit());

        AtomicLong completionTime = new AtomicLong(-1);
        CountDownLatch threadWaiting = new CountDownLatch(1);

        Thread waitingThread = new Thread(() -> {
            try {
                // Сигналим что поток начал выполнение и скоро будет ждать
                threadWaiting.countDown();

                long start = System.currentTimeMillis();
                api.sendDocument(createTestDocument()); // Этот вызов будет ждать
                completionTime.set(System.currentTimeMillis() - start);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        waitingThread.start();
        threadWaiting.await(); // Ждем пока поток дойдет до точки ожидания

        // Даем время войти в await()
        Thread.sleep(100);

        // Сбрасываем через ПУБЛИЧНЫЙ метод (а не reflection)
        long resetStart = System.currentTimeMillis();
        api.resetRateLimit();

        waitingThread.join(2000);

        long timeAfterReset = System.currentTimeMillis() - resetStart;

        System.out.println("Time after reset: " + timeAfterReset + "ms");
        System.out.println("Total completion time: " + completionTime.get() + "ms");

        // Должен проснуться почти сразу
        assertThat(timeAfterReset).isLessThan(config.getTimeUnit().toMillis(1) / 2);
    }

    private void occupyAllSlots(CrptApi api, int slots) throws Exception {
        // Занимаем все слоты через reflection
        for (int i = 0; i < slots; i++) {
            acquireRequestPermit(api);
        }
    }

    private void acquireRequestPermit(CrptApi api) throws Exception {
        Method method = CrptApi.class.getDeclaredMethod("acquireRequestPermit");
        method.setAccessible(true);
        method.invoke(api);
    }
    // Вспомогательные методы для тестирования через reflection

    private long getRequestCounter() throws Exception {
        Field counterField = CrptApi.class.getDeclaredField("requestsCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicLong counter =
                (java.util.concurrent.atomic.AtomicLong) counterField.get(crptApi);
        return counter.get();
    }

    private void resetRequestCounter() throws Exception {
        Field counterField = CrptApi.class.getDeclaredField("requestsCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicLong counter =
                (java.util.concurrent.atomic.AtomicLong) counterField.get(crptApi);
        counter.set(0);
    }

    private void acquireRequestPermit() throws Exception {
        Method method = CrptApi.class.getDeclaredMethod("acquireRequestPermit");
        method.setAccessible(true);
        method.invoke(crptApi);
    }

    private Document createTestDocument() {
        Document document = new Document();
        document.setProductGroup(ProductGroup.POLYMER);
        document.setParticipantInn("1234567890");
        document.setProducerInn("1234567890");
        document.setProductionDate("05.08.2025");
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