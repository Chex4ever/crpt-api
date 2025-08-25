package ru.selsup.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CryptoProInstallationCheck {

    public static void checkCryptoProInstallation() {
        try {
            System.out.println("=== ПРОВЕРКА УСТАНОВКИ КРИПТОПРО ===");
            
            // Проверяем реестр
            Process process = Runtime.getRuntime().exec(
                "reg query \"HKLM\\SOFTWARE\\Crypto Pro\\CSP\" /v InstallPath");
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "CP866"));
            
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("InstallPath")) {
                    System.out.println("Путь установки: " + line);
                    found = true;
                }
            }
            
            if (!found) {
                System.out.println("КриптоПро не найдено в реестре");
                
                // Проверяем стандартные пути
                String[] paths = {
                    "C:\\Program Files\\CryptoPro\\CSP\\",
                    "C:\\Program Files (x86)\\CryptoPro\\CSP\\"
                };
                
                for (String path : paths) {
                    java.io.File dir = new java.io.File(path);
                    if (dir.exists()) {
                        System.out.println("КриптоПро найдено в: " + path);
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                System.out.println("❌ КриптоПро не установлено");
            } else {
                System.out.println("✅ КриптоПро установлено");
            }

        } catch (Exception e) {
            System.out.println("Ошибка проверки: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        checkCryptoProInstallation();
    }
}