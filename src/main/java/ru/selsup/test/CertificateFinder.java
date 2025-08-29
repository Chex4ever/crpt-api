package ru.selsup.test;

import java.io.File;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class CertificateFinder {

    public static void search() {
        System.out.println("=== ПОИСК ВСЕХ ДОСТУПНЫХ ХРАНИЛИЩ И ПРОВАЙДЕРОВ ===");

        String[] storeTypes = Stream.concat(Arrays.stream(searchForFAT12Containers()), Arrays.stream(new String[]{"CNG", "HDIMAGE", "REGISTRY", "CertStore"})).toArray(String[]::new);
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
                                String s = aliases.nextElement();
                                System.out.println("alias: " + s);
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
    }

    public static void showCertificatesDetails(KeyStore keyStore) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                try {
                    java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof java.security.cert.X509Certificate x509) {
                        System.out.println("      📄 " + alias + " -> " + x509.getSubjectX500Principal().getName());
                    }
                } catch (Exception e) {
                    System.out.println("      ❌ Ошибка сертификата " + alias + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("      ❌ Ошибка перечисления: " + e.getMessage());
        }
    }


    private static String[] searchForFAT12Containers() {
        File[] roots = File.listRoots();
        Set<String> result = new HashSet<>();
        System.out.println(Arrays.toString(roots));
        for (File root : roots) {
            char driveLetter = root.getPath().charAt(0);
            String containerType = "FAT12_" + driveLetter;
            try {
                KeyStore ks = KeyStore.getInstance(containerType, "JCSP");
                ks.load(null, null);
                System.out.println("Контейнер найден на диске: " + driveLetter);
                result.add(containerType);

            } catch (Exception e) {
                System.out.println("Контейнер не обнаружен на диске: " + driveLetter);
            }
        }
        return result.toArray(new String[0]);
    }
}