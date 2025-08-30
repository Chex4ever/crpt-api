package ru.selsup.trueapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.CryptoPro.reprov.RevCheck;
import ru.selsup.crypto.*;
import ru.selsup.trueapi.model.*;

import java.security.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("com.ibm.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true");
        System.setProperty("java.util.prefs.syncInterval", "99999");
        Security.insertProviderAt(new ru.CryptoPro.JCSP.JCSP(), 1);
        Security.addProvider(new RevCheck());
    }

    public static void main(String[] args) {
        TokenStorage tokenStorage = TokenStorageFactory.createStorage(
                TokenStorageFactory.StorageType.FILE,
                "tokens/auth_token.json"
        );
        try {
            JcspSigningService signer = new JcspSigningService(new StorageDiscoverer());
            Config config = new ConfigLoader().loadConfig();
            CrptApi api = new CrptApi(signer, config, tokenStorage);

            signer.defaultAlgo = CryptoAlgorithms.GOST3411_2012_256_WITH_GOST3410_2012_256.getAlgorithmName();
            logger.info("Используем алгоритм: {}", signer.defaultAlgo);

            CompletableFuture<Void> authFuture = api.authenticate();
            authFuture.get(30, TimeUnit.SECONDS);
            logger.info("Успешно авторизовались с алгоритмом: {}", signer.defaultAlgo);

            api.checkParticipantsByINN("6669008900");

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
            for (int i = 0; i < 20; i++) {
                api.sendDocument(d);
            }
        } catch (NoCertificateFoundException | ExecutionException | InterruptedException | TimeoutException |
                 JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}