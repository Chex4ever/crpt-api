package ru.selsup.test;

import java.io.IOException;
import ru.CryptoPro.JCSP.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, CertificateException, IOException {
        System.setProperty("file.encoding", "UTF-8");
        Security.addProvider(new JCSP());
        System.out.println("Hello, World!");
        System.out.println(Arrays.toString(Security.getProviders()));
        KeyStore keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        keyStore.load(null, null);

        // Перечисляем доступные сертификаты
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
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
}