package ru.selsup.trueapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.CryptoPro.JCSP.JCSP;
import ru.CryptoPro.reprov.RevCheck;
import ru.selsup.crypto.*;
import ru.selsup.trueapi.model.*;

import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {
    public static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

    private static Document newDocument() {
        Document d = new Document();
        d.setProductGroup(ProductGroup.POLYMER);
        d.setParticipantInn("6669008900");
        d.setProducerInn("6669008900");
        d.setOwnerInn("6669008900");
        d.setProductionDate("05.08.2025");
        d.setProductionType(ProductionType.CONTRACT.getCode());
        d.setType(DocumentType.LP_INTRODUCE_GOODS);
        d.setProducts(List.of(new Product(
                "010461111111111121LLLLLLLLLLLLL",
                null,
                "0000000000",
                List.of(new CertificateDocument(
                        PermitDocType.CONFORMITY_CERTIFICATE,
                        "RU С-ХХ.АА00.B.00001/21",
                        "2025-08-01")))));
        return d;
    }

    @Test
    void discoverAndPrintCertificate() {
        // Настройка окружения
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("com.ibm.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true");
        System.setProperty("java.util.prefs.syncInterval", "99999");
        Security.insertProviderAt(new JCSP(), 1);
        Security.addProvider(new RevCheck());

        try {
            StorageDiscoverer discoverer = new StorageDiscoverer();
            Map<String, List<CertificateInfo>> allCertificates = discoverer.discoverAllContainers();
            printAllCertificates(allCertificates);
        } catch (Exception e) {
            System.err.println("Ошибка при поиске сертификатов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printAllCertificates(Map<String, List<CertificateInfo>> allCertificates) {
        System.out.println("=".repeat(80));
        System.out.println("НАЙДЕННЫЕ СЕРТИФИКАТЫ");
        System.out.println("=".repeat(80));
        if (allCertificates.isEmpty()) {
            System.out.println("❌ Сертификаты не найдены");
            return;
        }
        int totalCount = allCertificates.values().stream()
                .mapToInt(List::size)
                .sum();
        System.out.println("Всего найдено сертификатов: " + totalCount);
        System.out.println();
        int globalIndex = 1;
        for (Map.Entry<String, List<CertificateInfo>> entry : allCertificates.entrySet()) {
            String storeType = entry.getKey();
            List<CertificateInfo> certs = entry.getValue();
            System.out.println("📁 Хранилище: " + storeType);
            System.out.println("-".repeat(60));
            for (CertificateInfo cert : certs) {
                System.out.println(globalIndex + ". " + cert.toString());
                System.out.println("   Alias: " + cert.getAlias());
                System.out.println("   Subject: " + cert.getSubject());
                System.out.println("   Issuer: " + cert.getIssuer());
                System.out.println("   Valid From: " + cert.getValidFrom());
                System.out.println("   Valid To: " + cert.getValidTo());
                System.out.println();
                globalIndex++;
            }
            System.out.println();
        }
    }

    @Test
    void testFullCreateDocumentFlowWithSelectedCert() {
        // Сначала запустите discoverAndSelectCertificate() чтобы получить alias
        // Затем запишите в String selectedAlias на нужный alias

        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("com.ibm.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true");
        System.setProperty("java.util.prefs.syncInterval", "99999");
        String selectedAlias = "1026601373765 1206153237";
        System.setProperty("cryptopro.cert.alias", selectedAlias);
        System.setProperty("cryptopro.keystore.alias", selectedAlias);
        System.setProperty("ru.CryptoPro.JCSP.select.cert.auto", "true");
        Security.insertProviderAt(new JCSP(), 1);
        Security.addProvider(new RevCheck());
        TokenStorage tokenStorage = TokenStorageFactory.createStorage(
                TokenStorageFactory.StorageType.FILE,
                "tokens/auth_token.json"
        );
        try {
            JcspSigningService signer = new JcspSigningService(new StorageDiscoverer(), selectedAlias);
            Config config = new ConfigLoader().loadConfig();
            CrptApi api = new CrptApi(signer, config, tokenStorage);
            signer.defaultAlgo = CryptoAlgorithms.GOST3411_2012_256_WITH_GOST3410_2012_256.getAlgorithmName();
            logger.info("Используем алгоритм: {}", signer.defaultAlgo);
            CompletableFuture<Void> authFuture = api.authenticate();
            authFuture.get(30, TimeUnit.SECONDS);
            logger.info("Успешно авторизовались с алгоритмом: {}", signer.defaultAlgo);
            api.checkParticipantsByINN("6669008900");
            Document d = newDocument();
            for (int i = 0; i < 20; i++) {
                api.sendDocument(d);
                System.out.println("✅ Используется сертификат: " + signer.selectedCert().getAlias());
            }
        } catch (ExecutionException | JsonProcessingException | TimeoutException | InterruptedException |
                 NoCertificateFoundException e) {
            throw new RuntimeException(e);
        }
    }
}