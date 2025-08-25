package ru.selsup.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WindowsCertUtil {

    public static void listCertificatesWithCertUtil() {
        try {
            System.out.println("=== ЗАПУСК CERTUTIL ===");
            
            Process process = Runtime.getRuntime().exec("certutil -user -store My");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Федеральное казначейство") || line.contains("FAT12")) {
                    System.out.println("🔍 " + line);
                }
                if (line.contains("Субъект") || line.contains("Поставщик") || line.contains("Контейнер")) {
                    System.out.println("   " + line);
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            System.out.println("Ошибка certutil: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        listCertificatesWithCertUtil();
    }
}