package ru.selsup.test;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.selsup.crypto.*;
import ru.selsup.crypto.ConsoleCertificateSelector;

import java.security.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("com.ibm.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true");
        System.setProperty("java.util.prefs.syncInterval", "99999");
//        Security.insertProviderAt(new ru.CryptoPro.JCP.JCP(), 2);
        Security.insertProviderAt(new ru.CryptoPro.JCSP.JCSP(), 1);
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("BouncyCastle provider registered");
        }
    }

    public static void main(String[] args) {
        CertificateSelector selector = new ConsoleCertificateSelector();

        try {
            CertificateSigningService signer = new CertificateSigningService(new StorageDiscoverer());
            Config config = new ConfigLoader().loadConfig();
            CrptApi api = new CrptApi(signer, config);

            List<String> algorithmsToTry = Arrays.stream(CryptoAlgorithms.values()).map(CryptoAlgorithms::getAlgorithmName).toList();
            for (String algorithm : algorithmsToTry) {
                try {
                    signer.defaultAlgo = algorithm;
                    logger.info("Пробуем алгоритм: {}", algorithm);

                    CompletableFuture<Void> authFuture = api.authenticate();
                    authFuture.get(30, TimeUnit.SECONDS);

                    logger.info("Успешно авторизовались с алгоритмом: {}", algorithm);

                } catch (Exception e) {
                    logger.error("Не удалось авторизоваться ", e);
                }
            }

        } catch (NoCertificateFoundException e) {
            throw new RuntimeException(e);
        }
    }

}