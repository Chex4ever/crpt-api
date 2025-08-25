package ru.selsup.test;

import javax.swing.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class CorrectCertificateSelector {

    static {
        try {
            // Регистрируем провайдер
            if (Security.getProvider("CP") == null) {
                Security.addProvider(new ru.CryptoPro.JCP.JCP());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static X509Certificate selectCertificate() {
        try {
            // Используем правильное имя провайдера - "CP" вместо "JCSP"
            Provider provider = Security.getProvider("CP");
            if (provider == null) {
                JOptionPane.showMessageDialog(null, 
                    "КриптоПро провайдер не найден!\nУбедитесь, что КриптоПро CSP установлен.");
                return null;
            }

            System.out.println("Используем провайдер: " + provider.getName());

            // Пробуем разные имена хранилищ
            String[] storeTypes = {"HDImage", "Image", "System"};

            for (String storeType : storeTypes) {
                try {
                    System.out.println("Пробуем хранилище: " + storeType);
                    
                    KeyStore keyStore = KeyStore.getInstance(storeType, "CP");
                    
                    // Пробуем загрузить хранилище
                    try {
                        keyStore.load(null, null);
                        System.out.println("Хранилище загружено: " + storeType);
                    } catch (Exception e) {
                        if (e.getMessage().contains("PIN")) {
                            // Запрос PIN
                            String pin = JOptionPane.showInputDialog("Введите PIN-код для токена:");
                            if (pin != null && !pin.isEmpty()) {
                                keyStore.load(null, pin.toCharArray());
                            } else {
                                continue;
                            }
                        } else {
                            throw e;
                        }
                    }

                    // Ищем сертификаты
                    Enumeration<String> aliases = keyStore.aliases();
                    if (aliases == null) {
                        System.out.println("Нет алиасов в хранилище");
                        continue;
                    }

                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        try {
                            java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                            if (cert instanceof X509Certificate) {
                                X509Certificate x509Cert = (X509Certificate) cert;
                                
                                // Проверяем, что это нужный сертификат по издателю
                                if (x509Cert.getIssuerDN().toString().contains("Федеральное казначейство")) {
                                    System.out.println("Найден сертификат Казначейства: " + alias);
                                    return x509Cert;
                                }
                                
                                System.out.println("Найден сертификат: " + x509Cert.getSubjectDN());
                            }
                        } catch (Exception e) {
                            System.out.println("Ошибка чтения сертификата " + alias + ": " + e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Ошибка с хранилищем " + storeType + ": " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null, 
                "Сертификаты не найдены.\nВозможно, требуется:\n" +
                "1. Ввести PIN-код\n" +
                "2. Проверить подключение токена\n" +
                "3. Обновить драйверы КриптоПро");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка: " + e.getMessage());
        }

        return null;
    }

    public static void main(String[] args) {
        X509Certificate cert = selectCertificate();
        if (cert != null) {
            System.out.println("УСПЕХ! Выбран сертификат:");
            System.out.println("Владелец: " + cert.getSubjectDN());
            System.out.println("Издатель: " + cert.getIssuerDN());
            System.out.println("Серийный номер: " + cert.getSerialNumber());
        } else {
            System.out.println("Сертификат не найден");
        }
    }
}