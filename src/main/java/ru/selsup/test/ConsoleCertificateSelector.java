package ru.selsup.test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

public class ConsoleCertificateSelector {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleCertificateSelector.class);

    public static X509Certificate selectCertificate() {
        logger.info("Начинаем выбор сертификата из системного хранилища...");

        try {
            // Используем системное хранилище Windows через SunMSCAPI
            logger.info("Пытаемся получить доступ к системному хранилищу Windows...");

            // Пробуем разные провайдеры для системного хранилища
            String[] providers = {"SunMSCAPI", "SunMicrosoft", "Microsoft"};
            String[] storeTypes = {"Windows-MY", "Windows-MY-CURRENTUSER", "MY"};

            for (String providerName : providers) {
                for (String storeType : storeTypes) {
                    try {
                        logger.info("Пробуем: {} + {}", providerName, storeType);

                        Provider provider = Security.getProvider(providerName);
                        if (provider == null) {
                            logger.warn("Провайдер {} не найден", providerName);
                            continue;
                        }

                        logger.info("Найден провайдер: {}", providerName);

                        KeyStore keyStore = KeyStore.getInstance(storeType, providerName);
                        logger.info("KeyStore создан для: {}", storeType);

                        // Загружаем хранилище
                        keyStore.load(null, null);
                        logger.info("Хранилище успешно загружено");

                        // Получаем алиасы
                        Enumeration<String> aliases = keyStore.aliases();
                        if (aliases == null) {
                            logger.warn("aliases() вернул null");
                            continue;
                        }

                        List<X509Certificate> certs = new ArrayList<>();
                        List<String> certInfos = new ArrayList<>();

                        logger.info("Перечисляем сертификаты...");
                        int certCount = 0;

                        while (aliases.hasMoreElements()) {
                            String alias = aliases.nextElement();
                            certCount++;

                            try {
                                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                                if (cert != null) {
                                    certs.add(cert);
                                    String certInfo = String.format("%s (SN: %s, до: %s)",
                                            getCommonName(cert.getSubjectDN().toString()),
                                            cert.getSerialNumber(),
                                            cert.getNotAfter().toString().substring(0, 10));
                                    certInfos.add(certInfo);
                                    logger.info("Найден сертификат: {}", certInfo);
                                }
                            } catch (Exception certEx) {
                                logger.warn("Ошибка получения сертификата для алиаса '{}': {}",
                                        alias, certEx.getMessage());
                            }
                        }

                        logger.info("Найдено {} сертификатов", certs.size());

                        if (!certs.isEmpty()) {
                            return showSelectionDialog(certs, certInfos);
                        }

                    } catch (Exception e) {
                        logger.warn("Ошибка с {} + {}: {}", providerName, storeType, e.getMessage());
                    }
                }
            }

            // Если не нашли в системном хранилище, пробуем КриптоПро
            logger.info("Пробуем хранилища КриптоПро...");
            return tryCryptoProStores();

        } catch (Exception e) {
            logger.error("Критическая ошибка: {}", e.getMessage(), e);
        }

        return null;
    }

    private static X509Certificate tryCryptoProStores() {
        try {
            // Добавляем JCSP если нужно
            if (Security.getProvider("JCSP") == null) {
                Security.addProvider(new ru.CryptoPro.JCSP.JCSP());
            }

            String[] cryptoProStores = {"CryptoPro", "CryptoProStore", "System"};

            for (String storeType : cryptoProStores) {
                try {
                    KeyStore keyStore = KeyStore.getInstance(storeType, "JCSP");
                    keyStore.load(null, null);

                    Enumeration<String> aliases = keyStore.aliases();
                    List<X509Certificate> certs = new ArrayList<>();
                    List<String> certInfos = new ArrayList<>();

                    while (aliases != null && aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                        if (cert != null) {
                            certs.add(cert);
                            certInfos.add(String.format("%s - %s",
                                    getCommonName(cert.getSubjectDN().toString()),
                                    cert.getSerialNumber()));
                        }
                    }

                    if (!certs.isEmpty()) {
                        return showSelectionDialog(certs, certInfos);
                    }

                } catch (Exception e) {
                    logger.warn("Ошибка с хранилищем {}: {}", storeType, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Ошибка при работе с КриптоПро: {}", e.getMessage());
        }

        return null;
    }

    private static X509Certificate showSelectionDialog(List<X509Certificate> certs, List<String> certInfos) {
        if (certs.isEmpty()) {
            return null;
        }

        System.out.println("\n=== НАЙДЕНЫ СЕРТИФИКАТЫ ===");
        for (int i = 0; i < certInfos.size(); i++) {
            System.out.println((i + 1) + ". " + certInfos.get(i));
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("\nВыберите номер сертификата (0 для отмены): ");

        try {
            int choice = scanner.nextInt();
            if (choice == 0) {
                logger.info("Выбор отменен");
                return null;
            }

            if (choice > 0 && choice <= certs.size()) {
                X509Certificate selectedCert = certs.get(choice - 1);
                logger.info("Выбран сертификат: {}", selectedCert.getSubjectDN());
                return selectedCert;
            } else {
                System.out.println("Неверный номер!");
            }
        } catch (Exception e) {
            System.out.println("Ошибка ввода!");
        }

        return null;
    }

    private static String getCommonName(String dn) {
        // Извлекаем CN из Distinguished Name
        String[] parts = dn.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("CN=")) {
                return part.trim().substring(3);
            }
        }
        return dn.length() > 50 ? dn.substring(0, 50) + "..." : dn;
    }

    public static void main(String[] args) {
        logger.info("Запуск выбора сертификата...");
        X509Certificate cert = selectCertificate();

        if (cert != null) {
            System.out.println("\n✅ Выбран сертификат: " + cert.getSubjectDN());
            System.out.println("   Серийный номер: " + cert.getSerialNumber());
            System.out.println("   Действует до: " + cert.getNotAfter());
        } else {
            System.out.println("\n❌ Сертификат не выбран");
            System.out.println("Проверьте:");
            System.out.println("1. Установлен ли сертификат в системное хранилище Windows");
            System.out.println("2. Доступен ли закрытый ключ");
            System.out.println("3. Запущена ли программа с правами администратора");
        }
    }
}