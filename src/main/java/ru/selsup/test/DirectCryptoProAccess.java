package ru.selsup.test;

import ru.CryptoPro.JCSP.JCSP;
import java.security.Security;

public class DirectCryptoProAccess {

    static {
        // Принудительно регистрируем провайдер
        try {
            Security.removeProvider("JCSP");
            Security.addProvider(new JCSP());
            System.out.println("JCSP провайдер принудительно зарегистрирован");
        } catch (Exception e) {
            System.out.println("Ошибка регистрации JCSP: " + e.getMessage());
        }
    }

    public static void listContainers() {
        try {
            System.out.println("=== ПРЯМОЙ ДОСТУП К КРИПТОПРО ===");
            
            // Используем reflection для доступа к внутренним классам
            Class<?> configClass = Class.forName("ru.CryptoPro.JCSP.KeyStore.KeyStoreConfig");
            java.lang.reflect.Method getReadersMethod = configClass.getMethod("getReaders");
            
            Object[] readers = (Object[]) getReadersMethod.invoke(null);
            System.out.println("Найдено устройств: " + readers.length);
            
            for (Object reader : readers) {
                Class<?> readerClass = reader.getClass();
                java.lang.reflect.Method getNameMethod = readerClass.getMethod("getName");
                java.lang.reflect.Method getTypeMethod = readerClass.getMethod("getType");
                
                String name = (String) getNameMethod.invoke(reader);
                String type = (String) getTypeMethod.invoke(reader);
                
                System.out.println("Устройство: " + name + " (тип: " + type + ")");
                
                // Пробуем получить хранилище для этого устройства
                try {
                    java.lang.reflect.Method getKeyStoreMethod = configClass.getMethod("getKeyStore", String.class);
                    Object keyStore = getKeyStoreMethod.invoke(null, name);
                    
                    if (keyStore != null) {
                        Class<?> keyStoreClass = keyStore.getClass();
                        java.lang.reflect.Method aliasesMethod = keyStoreClass.getMethod("aliases");
                        
                        String[] aliases = (String[]) aliasesMethod.invoke(keyStore);
                        System.out.println("   Сертификатов: " + (aliases != null ? aliases.length : 0));
                        
                        if (aliases != null) {
                            for (String alias : aliases) {
                                java.lang.reflect.Method getCertificateMethod = keyStoreClass.getMethod("getCertificate", String.class);
                                Object cert = getCertificateMethod.invoke(keyStore, alias);
                                
                                if (cert != null) {
                                    System.out.println("   📄 " + alias);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("   ❌ Ошибка доступа: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.out.println("Ошибка прямого доступа: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        listContainers();
    }
}