package ru.selsup.trueapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.selsup.crypto.JcspSigningService;
import ru.selsup.trueapi.model.Document;
import ru.selsup.trueapi.model.DocumentType;
import ru.selsup.trueapi.model.Product;
import ru.selsup.trueapi.model.ProductGroup;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {
    
    @Test
    void testFullDocumentFlow() throws Exception {
        // Этот тест требует реального API или расширенного мокинга
        // Здесь показана структура интеграционного теста
        
        TokenStorage tokenStorage = new InMemoryTokenStorage();
        JcspSigningService signer = mock(JcspSigningService.class);
        Config config = createTestConfig();
        
        CrptApi api = new CrptApi(signer, config, tokenStorage);
        
        Document document = createTestDocument();
        
        // Мокаем все внешние вызовы
        // when(signer.signData(anyString(), anyBoolean())).thenReturn("mocked-signature");
        
        CompletableFuture<String> result = api.sendDocument(document);
        
        assertNotNull(result);
    }
    
    private Config createTestConfig() {
        Config config = new Config();
        config.setBaseUrlV3("https://ismp.crpt.ru/api/v3");
        config.setAuthGetDataEndpoint("/auth/data");
        config.setAuthGetTokenEndpoint("/auth/token");
        config.setCreateDocumentEndpoint("/lk/documents/create");
        config.setRequestLimit(10);
        config.setTimeUnit(TimeUnit.SECONDS);
        return config;
    }
    
    private Document createTestDocument() {
        Document document = new Document();
        document.setProductGroup(ProductGroup.POLYMER);
        document.setParticipantInn("1234567890");
        document.setType(DocumentType.LP_INTRODUCE_GOODS);
        document.setProducts(List.of(new Product(
            "010461111111111121LLLLLLLLLLLLL",
            null,
            "0000000000",
            List.of()
        )));
        return document;
    }
    
    // Вспомогательный класс для тестов
    private static class InMemoryTokenStorage implements TokenStorage {
        private String token;
        private Instant expirationTime;
        
        @Override
        public void saveToken(String token, Instant expirationTime) {
            this.token = token;
            this.expirationTime = expirationTime;
        }
        
        @Override
        public String getToken() {
            return token;
        }
        
        @Override
        public boolean isValid() {
            return token != null && expirationTime != null && 
                   Instant.now().isBefore(expirationTime);
        }
        
        @Override
        public void clear() {
            token = null;
            expirationTime = null;
        }
        
        @Override
        public Instant getExpirationTime() {
            return expirationTime;
        }
    }
}