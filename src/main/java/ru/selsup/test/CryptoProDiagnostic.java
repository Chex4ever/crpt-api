package ru.selsup.test;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class CryptoProDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== ДИАГНОСТИКА КРИПТОПРО ===");

        // 1. Проверяем провайдеры
        System.out.println("\n1. ДОСТУПНЫЕ ПРОВАЙДЕРЫ:");
        for (Provider provider : Security.getProviders()) {
            System.out.println("   " + provider.getName() + " - " + provider.getInfo());
        }

        // 2. Проверяем JCSP
        try {
            System.out.println("\n2. ПРОВЕРКА JCSP:");
            Provider jcsp = Security.getProvider("JCSP");
            if (jcsp == null) {
                System.out.println("   JCSP не найден, пробуем добавить...");
                Security.addProvider(new ru.CryptoPro.JCSP.JCSP());
                jcsp = Security.getProvider("JCSP");
            }

            if (jcsp != null) {
                System.out.println("   JCSP: " + jcsp.getInfo());
            } else {
                System.out.println("   JCSP: НЕ ДОСТУПЕН");
            }

        } catch (Exception e) {
            System.out.println("   Ошибка JCSP: " + e.getMessage());
        }

        // 3. Проверяем типы хранилищ
        System.out.println("\n3. ПРОВЕРКА ТИПОВ ХРАНИЛИЩ:");
        String[] storeTypes = {"HDIMAGE", "REGISTRY", "CertStore"};

        for (String type : storeTypes) {
            try {
                KeyStore ks = KeyStore.getInstance(type, "JCSP");
                System.out.println("   " + type + ": ДОСТУПЕН");

                // Пробуем загрузить
                try {
                    ks.load(null, null);
                    System.out.println("     Загрузка: УСПЕШНО");

                    // Пробуем получить алиасы
                    try {
                        java.util.Enumeration<String> aliases = ks.aliases();
                        int count = 0;
                        if (aliases != null) {
                            while (aliases.hasMoreElements()) {
                                aliases.nextElement();
                                count++;
                            }
                        }
                        System.out.println("     Сертификатов: " + count);
                    } catch (Exception e) {
                        System.out.println("     Сертификатов: ОШИБКА - " + e.getMessage());
                    }

                } catch (Exception e) {
                    System.out.println("     Загрузка: ОШИБКА - " + e.getMessage());
                }

            } catch (Exception e) {
                System.out.println("   " + type + ": НЕДОСТУПЕН - " + e.getMessage());
            }
        }

        System.out.println("\n=== ДИАГНОСТИКА ЗАВЕРШЕНА ===");

        try {
            KeyStore ks = KeyStore.getInstance("Windows-ROOT", "SunMSCAPI");
            ks.load(null, null);
            Enumeration<String> aliases = ks.aliases();
            if (aliases != null) {
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    System.out.println("Alias: " + alias);
                }
            }
        } catch (KeyStoreException | NoSuchProviderException | CertificateException | IOException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


        System.out.println("\n=== ДИАГНОСТИКА ЗАВЕРШЕНА ===");

    }
}