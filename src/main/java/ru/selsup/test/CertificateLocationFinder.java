package ru.selsup.test;

import java.security.KeyStore;
import java.security.Security;
import java.util.Enumeration;

public class CertificateLocationFinder {

    public static void main(String[] args) {
        System.out.println("=== ПОИСК ВСЕХ ДОСТУПНЫХ ХРАНИЛИЩ И ПРОВАЙДЕРОВ ===");

        // Добавляем провайдера JCP
//        Security.addProvider(new ru.CryptoPro.JCP.JCP());
        Security.insertProviderAt(new ru.CryptoPro.JCSP.JCSP(), 1);

        // Используем только хранилище Windows-MY
        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY", "JCSP");
            System.out.println("✅ Пробуем хранилище: Windows-MY");

            ks.load(null, null);
            Enumeration<String> aliases = ks.aliases();
            int count = 0;
            if (aliases != null) {
                while (aliases.hasMoreElements()) {
                    aliases.nextElement();
                    count++;
                }
            }
            System.out.println("   ✅ Найдено сертификатов: " + count);

            if (count > 0) {
                showCertificatesDetails(ks);
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка загрузки хранилища: " + e.getMessage());
        }
    }

    public static void showCertificatesDetails(KeyStore keyStore) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                try {
                    java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof java.security.cert.X509Certificate) {
                        java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cert;
                        System.out.println("      📄 " + alias + " -> " + x509.getSubjectDN());
                    }
                } catch (Exception e) {
                    System.out.println("      ❌ Ошибка сертификата " + alias + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("      ❌ Ошибка перечисления: " + e.getMessage());
        }
    }
}