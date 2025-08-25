package ru.selsup.test;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("com.ibm.security.enableCRLDP", "true"); //crl online
        System.setProperty("com.sun.security.enableCRLDP", "true"); //crl online
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true"); // для загрузки сертификатов по AIA из сети
        System.setProperty("java.util.prefs.syncInterval", "99999"); // https://support.cryptopro.ru/index.php?/Knowledgebase/Article/View/315/6/warning-couldnt-flush-system-prefs-javautilprefsbackingstoreexception--sreate-failed

//        boolean isJCSPExists = Security.insertProviderAt("ru.CryptoPro.JCSP.JCSP",1);
//        Security.insertProviderAt(new ru.CryptoPro.JCSP.JCSP(),1);
//        if (isJCSPExists) {
//            if (!addProvider("ru.CryptoPro.sspiSSL.SSPISSL")) {
//                logger.info("ru.CryptoPro.sspiSSL.SSPISSL not registered, trying ru.CryptoPro.ssl.Provider");
//                addProvider("ru.CryptoPro.ssl.Provider");
//            }
//        } else {
//            Security.insertProviderAt(new ru.CryptoPro.JCP.JCP(), 1);
//            addProvider("ru.CryptoPro.JCP.JCP");
//            addProvider("ru.CryptoPro.Crypto.CryptoProvider");
//            addProvider("ru.CryptoPro.ssl.Provider");
//        }
//        addProvider("ru.CryptoPro.reprov.RevCheck");
    }
    public static void main(String[] args) {
        System.out.println("Доступные провайдеры:");

//        for (Provider provider : Security.getProviders()) {
//            System.out.println(provider.getName() + " - " + provider.getInfo());
//        }

        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            System.out.println("✅ Пробуем хранилище: Windows-MY");
            ks.load(null, null); // Пароль не нужен
            Enumeration<String> aliases = ks.aliases();
            System.out.println("Aliases: " + aliases.toString());
            int count = 0;
            if (!aliases.hasMoreElements()) {
                System.out.println("❌ Коллекция пустая!");
            } else {
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    System.out.println("Alias: " + alias);
                }
            }
            System.out.println("   ✅ Найдено сертификатов: " + count);

            if (count > 0) {
                CertificateLocationFinder.showCertificatesDetails(ks);
            }
        } catch (KeyStoreException | NoSuchProviderException | CertificateException | IOException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

//
//        System.out.println("\nПопробуем найти КриптоПро провайдеры...");
//        String[] cryptoProProviders = {"JCSP", "JCP", "CryptoPro", "CP"};
//
//        for (String providerName : cryptoProProviders) {
//            Provider provider = Security.getProvider(providerName);
//            if (provider != null) {
//                System.out.println("Найден провайдер: " + providerName);
//                System.out.println("Инфо: " + provider.getInfo());
//            }
//        }
////        X509Certificate selectedCert = ConsoleCertificateSelector.selectCertificate();
//
//        CertificateLocationFinder.
//                main(null);


//        KeyStore keyStore = null;
//        try {
//            keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
//            keyStore.load(null, null);
//        } catch (KeyStoreException | NoSuchProviderException | IOException | NoSuchAlgorithmException |
//                 CertificateException e) {
//            throw new RuntimeException(e);
//        }

//        SwingUtilities.invokeLater(() -> {
//            try {
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            new CertificateSelectorExample().setVisible(true);
//        });
    }
//
//    private static boolean addProvider(String fullName) {
//        List<String> providers = Arrays.stream(Security.getProviders()).map(Provider::getName).toList();
//        try {
//            String shortName = GostProvidersNames.mapNames(fullName);
//            if (!providers.contains(shortName)) {
//                Security.addProvider((Provider) Class.forName(fullName).getConstructor().newInstance());
//                System.out.println("Provider registered " + fullName);
//            }
//            return true;
//        } catch (InvocationTargetException | InstantiationException | ClassNotFoundException | IllegalAccessException | NoSuchMethodException | RuntimeException e) {
//            logger.warn("Failed add provider: {}", fullName, e);
//            return false;
//        }
//    }
}