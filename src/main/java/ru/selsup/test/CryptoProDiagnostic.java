package ru.selsup.test;
import org.slf4j.Logger;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
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
        String[] storeTypes = {"CNG","HDIMAGE", "REGISTRY", "CertStore"};

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

        System.out.println("\n=== ДИАГНОСТИКА ЗАВЕРШЕНА ===");

        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
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

        System.out.println("\n4. РЕГИСТРАЦИЯ КОНТЕЙНЕРА В КРИПТОПРО:");

        try {
            // Команда для регистрации дискового контейнера
            String[] cmd = {
                    "cmd", "/c",
                    "csptest", "-addstore", "CERT", "K:\\XolmanMV.024\\",
                    "||",
                    "certutil", "-csp", "Crypto-Pro GOST R 34.10-2012 Cryptographic Service Provider",
                    "-importpfx", "K:\\XolmanMV.024\\", "MyContainer"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("   " + line);
            }
            process.waitFor();

            System.out.println("   Контейнер зарегистрирован, ждем 3 секунды...");
            Thread.sleep(3000);

        } catch (Exception e) {
            System.out.println("   Ошибка регистрации: " + e.getMessage());
        }

        System.out.println("\n5. ПРОВЕРКА ПОСЛЕ РЕГИСТРАЦИИ:");

        try {
            KeyStore ks = KeyStore.getInstance("REGISTRY", "JCSP");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            boolean found = false;

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                System.out.println("   Контейнер: " + alias);

                if (alias.contains("XolmanMV") || alias.contains("2305")) {
                    found = true;
                    System.out.println("   ★ НАЙДЕН ВАШ КОНТЕЙНЕР!");

                    // Получаем сертификат
                    java.security.cert.Certificate cert = ks.getCertificate(alias);
                    if (cert != null) {
                        System.out.println("   Сертификат: " + cert.toString());
//                        System.out.println("   Издатель: " + cert.getIssuerDN());
                    }
                }
            }

            if (!found) {
                System.out.println("   Контейнер не найден в реестре");
            }

        } catch (Exception e) {
            System.out.println("   Ошибка проверки: " + e.getMessage());
        }

        System.out.println("\n6. ПРОВЕРКА ЧЕРЕЗ WINDOWS-MY ХРАНИЛИЩЕ:");

        try {
            // Сертификат уже установлен в Windows, используем SunMSCAPI
            KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                System.out.println("   Сертификат в Windows-MY: " + alias);

                // Проверяем наш сертификат
                java.security.cert.Certificate cert = ks.getCertificate(alias);
                if (cert != null && cert.toString().contains("ГОСУДАРСТВЕННОЕ")) {
                    System.out.println("   ★ НАЙДЕН ВАШ СЕРТИФИКАТ!");
                    System.out.println("   Subject: " + cert.toString());
//                    System.out.println("   Issuer: " + cert.getIssuerDN());
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка Windows-MY: " + e.getMessage());
        }



        System.out.println("\n7. СПИСОК ВСЕХ ДОСТУПНЫХ КОНТЕЙНЕРОВ:");

        try {
            // Получаем все контейнеры через CSP
            KeyStore ks = KeyStore.getInstance("HDIMAGE", "JCSP");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            System.out.println("   Все доступные контейнеры в HDIMAGE:");
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                System.out.println("   - " + alias);
            }

        } catch (Exception e) {
            System.out.println("   Ошибка получения списка: " + e.getMessage());
        }

        System.out.println("\n8. ПРЯМАЯ ПРОВЕРКА КОНТЕЙНЕРА:");

        try {
            // Прямое обращение к контейнеру через файловую систему
            File containerDir = new File("K:\\XolmanMV.024\\");
            if (containerDir.exists() && containerDir.isDirectory()) {
                System.out.println("   Контейнер существует на диске");

                // Проверяем ключевые файлы
                String[] keyFiles = {"header.key", "masks.key", "primary.key", "name.key"};
                for (String file : keyFiles) {
                    File keyFile = new File(containerDir, file);
                    System.out.println("   " + file + ": " + (keyFile.exists() ? "НАЙДЕН" : "ОТСУТСТВУЕТ"));
                }
            } else {
                System.out.println("   Контейнер не найден на диске K:");
            }

        } catch (Exception e) {
            System.out.println("   Ошибка проверки файлов: " + e.getMessage());
        }

        System.out.println("\n9. ПРОВЕРКА КАК PKCS12:");

        try {
            // Попробуем загрузить как PKCS12 файл
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream fis = new FileInputStream("K:\\XolmanMV.024\\");

            // Пробуем с пустым паролем или стандартным
            try {
                ks.load(fis, "".toCharArray());
                System.out.println("   Успешно загружен как PKCS12");
            } catch (Exception e) {
                System.out.println("   Ошибка загрузки PKCS12: " + e.getMessage());
            }
            fis.close();

        } catch (Exception e) {
            System.out.println("   Ошибка PKCS12: " + e.getMessage());
        }

        System.out.println("\n10. ПРОВЕРКА ЧЕРЕЗ CERTUTIL:");

        try {
            // Правильная команда для проверки контейнера
            String[] cmd = {"cmd", "/c", "certutil", "-v", "-store", "My"};
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            String line;
            boolean found = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains("XolmanMV") || line.contains("2305") || line.contains("ГОСУДАРСТВЕННОЕ")) {
                    System.out.println("   ★ " + line);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("   Сертификат не найден в хранилище My");
            }

            process.waitFor();

        } catch (Exception e) {
            System.out.println("   Ошибка certutil: " + e.getMessage());
        }

        System.out.println("\n11. СПИСОК СЕРТИФИКАТОВ В WINDOWS:");

        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            System.out.println("   Сертификаты в Windows-MY:");

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                java.security.cert.Certificate cert = ks.getCertificate(alias);

                if (cert != null) {
                    String subject = cert.toString();
                    System.out.println("   - " + alias);

                    if (subject.contains("ГОСУДАРСТВЕННОЕ")) {
                        System.out.println("     ★ Subject: " + subject);
                        System.out.println("     ★ Issuer: " + cert.toString());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка списка сертификатов: " + e.getMessage());
        }

        System.out.println("\n12. ПРАВИЛЬНОЕ ОБРАЩЕНИЕ К КОНТЕЙНЕРУ:");

        try {
            // Используем правильное имя контейнера из вывода certutil
            String containerName = "FAT12\\2EF24A66_Moons\\XolmanMV.024\\2305";

            KeyStore ks = KeyStore.getInstance("HDIMAGE", "JCSP");
            ks.load(null, null);

            // Проверяем конкретный контейнер
            if (ks.containsAlias(containerName)) {
                System.out.println("   ★ КОНТЕЙНЕР НАЙДЕН: " + containerName);

                // Получаем сертификат
                java.security.cert.Certificate cert = ks.getCertificate(containerName);
                if (cert != null) {
                    System.out.println("   Subject: " + cert.toString());
//                    System.out.println("   Issuer: " + cert.getIssuerDN());

                    // Получаем приватный ключ
                    Key key = ks.getKey(containerName, null);
                    if (key != null) {
                        System.out.println("   Приватный ключ: " + key.getAlgorithm());
                    }
                }
            } else {
                System.out.println("   Контейнер не найден, проверяем все алиасы...");

                Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    System.out.println("   Доступный алиас: " + alias);

                    if (alias.contains("XolmanMV") || alias.contains("2305")) {
                        System.out.println("   ★ ВОЗМОЖНО ЭТО ОН: " + alias);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка: " + e.getMessage());
        }

        System.out.println("\n13. ПРОВЕРКА ЧЕРЕЗ REGISTRY STORE:");

        try {
            KeyStore ks = KeyStore.getInstance("REGISTRY", "JCSP");
            ks.load(null, null);

            String containerName = "FAT12\\2EF24A66_Moons\\XolmanMV.024\\2305";

            if (ks.containsAlias(containerName)) {
                System.out.println("   ★ КОНТЕЙНЕР В РЕЕСТРЕ: " + containerName);
            } else {
                System.out.println("   Контейнер не в реестре, ищем похожие...");

                Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (alias.contains("Moons") || alias.contains("XolmanMV")) {
                        System.out.println("   ★ ПОХОЖИЙ КОНТЕЙНЕР: " + alias);

                        java.security.cert.Certificate cert = ks.getCertificate(alias);
                        if (cert != null) {
                            System.out.println("   Subject: " + cert.toString());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка REGISTRY: " + e.getMessage());
        }

        System.out.println("\n14. ПРОВЕРКА ПОДПИСИ:");

        try {
            String containerName = "FAT12\\2EF24A66_Moons\\XolmanMV.024\\2305";

            KeyStore ks = KeyStore.getInstance("HDIMAGE", "JCSP");
            ks.load(null, null);

            if (ks.containsAlias(containerName)) {
                PrivateKey privateKey = (PrivateKey) ks.getKey(containerName, null);

                // Создаем подпись
                Signature signature = Signature.getInstance("GOST3411withECGOST3410", "JCSP");
                signature.initSign(privateKey);

                byte[] data = "test".getBytes();
                signature.update(data);
                byte[] signedData = signature.sign();

                System.out.println("   ★ ПОДПИСЬ УСПЕШНА! Длина: " + signedData.length + " байт");
            }

        } catch (Exception e) {
            System.out.println("   Ошибка подписи: " + e.getMessage());
        }

        System.out.println("\n15. ПРОВЕРКА ЧЕРЕЗ SUNMSCAPI:");

        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                // Ищем сертификат по издателю или subject
                java.security.cert.Certificate cert = ks.getCertificate(alias);
                String subject = cert.toString();

                if (subject.contains("ГОСУДАРСТВЕННОЕ") || subject.contains("XolmanMV")) {
                    System.out.println("   ★ СЕРТИФИКАТ НАЙДЕН: " + alias);
                    System.out.println("   Subject: " + subject);

                    // Пробуем получить ключ
                    Key key = ks.getKey(alias, null);
                    if (key != null) {
                        System.out.println("   Ключ: " + key.getAlgorithm());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка SunMSCAPI: " + e.getMessage());
        }


        System.out.println("\n16. ДИАГНОСТИКА НАСТРОЕК JCSP:");

        try {
            // Проверяем системные свойства JCSP
            System.out.println("   java.home: " + System.getProperty("java.home"));
            System.out.println("   user.dir: " + System.getProperty("user.dir"));
            System.out.println("   user.name: " + System.getProperty("user.name"));

            // Проверяем настройки КриптоПро
            String[] cspPaths = {
                    "C:\\Program Files\\Crypto Pro\\CSP\\",
                    "C:\\Program Files (x86)\\Crypto Pro\\CSP\\",
                    System.getenv("ProgramFiles") + "\\Crypto Pro\\CSP\\"
            };

            for (String path : cspPaths) {
                File cspDir = new File(path);
                System.out.println("   CSP path: " + path + " -> " + (cspDir.exists() ? "EXISTS" : "MISSING"));
            }

            // Проверяем переменные окружения
            System.out.println("   CSP_INSTALL_PATH: " + System.getenv("CSP_INSTALL_PATH"));

        } catch (Exception e) {
            System.out.println("   Ошибка диагностики: " + e.getMessage());
        }


        System.out.println("\n17. НИЗКОУРОВНЕВЫЙ ДОСТУП ЧЕРЕЗ JCSP:");

        try {
            // Получаем провайдер JCSP
            Provider jcspProvider = Security.getProvider("JCSP");
            if (jcspProvider != null) {
                System.out.println("   JCSP provider: " + jcspProvider.getInfo());

                // Пробуем получить список контейнеров через CSP
                ru.CryptoPro.JCSP.JCSP jcsp = (ru.CryptoPro.JCSP.JCSP) jcspProvider;

                // Используем reflection для доступа к внутренним методам
                try {
                    Class<?> cspClass = Class.forName("ru.CryptoPro.JCSP.CSP");
                    java.lang.reflect.Method getContainersMethod = cspClass.getMethod("getContainers");
                    Object containers = getContainersMethod.invoke(null);

                    System.out.println("   Контейнеры: " + containers);
                } catch (Exception e) {
                    System.out.println("   Reflection error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка низкоуровневого доступа: " + e.getMessage());
        }

        System.out.println("\n18. ПРОВЕРКА ЧЕРЕЗ CSPTEST:");

        try {
            // Запускаем csptest для получения списка контейнеров
            String[] cmd = {"cmd", "/c", "csptest", "-keyset", "-enum_cont", "-fqcn", "-verifycontext"};

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));

            String line;
            boolean found = false;
            System.out.println("   Контейнеры через csptest:");

            while ((line = reader.readLine()) != null) {
                System.out.println("   " + line);
                if (line.contains("XolmanMV") || line.contains("2305") || line.contains("Moons")) {
                    found = true;
                    System.out.println("   ★ НАЙДЕН: " + line);
                }
            }

            if (!found) {
                System.out.println("   Контейнер не найден в csptest");
            }

            process.waitFor();

        } catch (Exception e) {
            System.out.println("   Ошибка csptest: " + e.getMessage());
        }

        System.out.println("\n19. ПРОВЕРКА ДРУГИХ ТИПОВ ХРАНИЛИЩ:");

        try {
            // Пробуем разные типы хранилищ
            storeTypes = new String[]{"FAT12_K", "HDIMAGE", "REGISTRY", "CNG", "CAPI", "PKCS11"};

            for (String type : storeTypes) {
                try {
                    KeyStore ks = KeyStore.getInstance(type, "JCSP");
                    ks.load(null, null);

                    Enumeration<String> aliases = ks.aliases();
                    int count = 0;
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        if (count < 5) { // Выводим первые 5
                            System.out.println("   " + type + ": " + alias);
                        }
                        count++;
                    }
                    System.out.println("   " + type + " всего: " + count + " контейнеров");

                } catch (Exception e) {
                    System.out.println("   " + type + ": НЕДОСТУПЕН - " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("   Ошибка проверки хранилищ: " + e.getMessage());
        }




    }
}