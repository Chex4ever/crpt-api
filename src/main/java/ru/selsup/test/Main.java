package ru.selsup.test;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.CryptoPro.JCSP.*;

import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static java.security.Security.addProvider;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        System.setProperty("com.ibm.security.enableCRLDP", "true"); //crl online
        System.setProperty("com.sun.security.enableCRLDP", "true"); //crl online
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        System.setProperty("java.util.prefs.syncInterval", "99999"); // https://support.cryptopro.ru/index.php?/Knowledgebase/Article/View/315/6/warning-couldnt-flush-system-prefs-javautilprefsbackingstoreexception--sreate-failed

        boolean isJCSPExists = addProvider("ru.CryptoPro.JCSP.JCSP");
        logger.info("isJCSPExists = {}", isJCSPExists);
        if (isJCSPExists) {
            if (!addProvider("ru.CryptoPro.sspiSSL.SSPISSL"))
                addProvider("ru.CryptoPro.ssl.Provider");
        } else {
            addProvider("ru.CryptoPro.JCP.JCP");
            addProvider("ru.CryptoPro.Crypto.CryptoProvider");
            addProvider("ru.CryptoPro.ssl.Provider");
        }
        addProvider("ru.CryptoPro.reprov.RevCheck");
    }
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.out.println("Hello, World!");
        System.out.println(Arrays.toString(Security.getProviders()));
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            keyStore.load(null, null);
        } catch (KeyStoreException | NoSuchProviderException | IOException | NoSuchAlgorithmException |
                 CertificateException e) {
            throw new RuntimeException(e);
        }

        // Перечисляем доступные сертификаты
        Enumeration<String> aliases = null;
        try {
            aliases = keyStore.aliases();
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert = null;
            try {
                cert = (X509Certificate) keyStore.getCertificate(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Available certificate: " + cert.getSubjectDN());
            System.exit(0);
            try {
                Config config = ConfigLoader.loadDefaultConfig();
                CrptApi api = new CrptApi(config);
                api.authenticate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean addProvider(String fullName) {
        List<String> providers = Arrays.stream(Security.getProviders()).map(Provider::getName).toList();
        try {
            String shortName = GostProvidersNames.mapNames(fullName);
            if (!providers.contains(shortName)) {
                Security.addProvider((Provider) Class.forName(fullName).getConstructor().newInstance());
                System.out.println("Provider registered " + fullName);
            }
            return true;
        } catch (InvocationTargetException | InstantiationException | ClassNotFoundException | IllegalAccessException | NoSuchMethodException | RuntimeException e) {
            logger.warn("Failed add provider: {}", fullName, e);
            return false;
        }
    }
}