package ru.selsup.test;

import java.security.*;

public class EnvironmentDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== ДИАГНОСТИКА ОКРУЖЕНИЯ ===");
        
        // 1. Версия Java
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Java vendor: " + System.getProperty("java.vendor"));
        
        // 2. Провайдеры
        System.out.println("\n=== ПРОВАЙДЕРЫ ===");
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            System.out.println(provider.getName() + " - " + provider.getInfo());
            
            // Показываем сервисы для CryptoPro
            if (provider.getName().equals("JCP") || provider.getName().equals("JCSP")) {
                for (Provider.Service service : provider.getServices()) {
                    System.out.println("  " + service.getType() + " - " + service.getAlgorithm());
                }
            }
        }
        
        // 3. Проверка типов KeyStore
        System.out.println("\n=== ТИПЫ KEYSTORE ===");
        String[] storeTypes = {"HDImage", "Windows-MY", "PKCS12"};
        
        for (String type : storeTypes) {
            try {
                KeyStore.getInstance(type);
                System.out.println(type + " - ДОСТУПЕН");
            } catch (Exception e) {
                System.out.println(type + " - НЕДОСТУПЕН: " + e.getMessage());
            }
        }
    }
}