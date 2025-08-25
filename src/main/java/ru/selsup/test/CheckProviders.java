package ru.selsup.test;
import java.security.Provider;
import java.security.Security;

public class CheckProviders {
    public static void main(String[] args) {
        System.out.println("Доступные провайдеры:");
        for (Provider provider : Security.getProviders()) {
            System.out.println(provider.getName() + " - " + provider.getInfo());
        }

        System.out.println("\nПопробуем найти КриптоПро провайдеры...");
        String[] cryptoProProviders = {"JCSP", "JCP", "CryptoPro", "CP"};

        for (String providerName : cryptoProProviders) {
            Provider provider = Security.getProvider(providerName);
            if (provider != null) {
                System.out.println("Найден провайдер: " + providerName);
                System.out.println("Инфо: " + provider.getInfo());
            }
        }
    }
}
