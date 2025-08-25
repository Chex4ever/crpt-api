package ru.selsup.test;

import javax.swing.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class WindowsCertificateSelector {

    public static X509Certificate selectCertificate() {
        try {
            // Используем системное хранилище Windows через SunMSCAPI провайдер
            KeyStore keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            keyStore.load(null, null); // Загружаем без пароля
            
            System.out.println("Системное хранилище загружено успешно");
            
            // Перечисляем все сертификаты
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                try {
                    java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509Cert = (X509Certificate) cert;
                        
                        // Проверяем, что это сертификат Казначейства
                        if (x509Cert.getIssuerDN().toString().contains("Федеральное казначейство")) {
                            System.out.println("Найден сертификат Казначейства: " + alias);
                            System.out.println("Владелец: " + x509Cert.getSubjectDN());
                            return x509Cert;
                        }
                        
                        System.out.println("Другой сертификат: " + x509Cert.getSubjectDN());
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка чтения сертификата " + alias + ": " + e.getMessage());
                }
            }
            
            JOptionPane.showMessageDialog(null, 
                "Сертификат Федерального казначейства не найден в системном хранилище");
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Ошибка доступа к системному хранилищу: " + e.getMessage() +
                "\nУбедитесь, что используется Java с поддержкой SunMSCAPI");
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
            
            // Тестируем подписание
            testSignature(cert);
        }
    }
    
    private static void testSignature(X509Certificate cert) {
        try {
            // Пробуем получить приватный ключ для подписи
            KeyStore keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            keyStore.load(null, null);
            
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                java.security.cert.Certificate storeCert = keyStore.getCertificate(alias);
                if (storeCert != null && storeCert.equals(cert)) {
                    try {
                        // Пробуем получить приватный ключ
                        java.security.Key key = keyStore.getKey(alias, null);
                        if (key instanceof java.security.PrivateKey) {
                            System.out.println("✅ Приватный ключ доступен для подписи!");
                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("Для доступа к ключу可能需要 PIN или аутентификация: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("⚠️ Приватный ключ не доступен");
            
        } catch (Exception e) {
            System.out.println("Ошибка тестирования ключа: " + e.getMessage());
        }
    }
}