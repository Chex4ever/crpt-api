package ru.selsup.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PowerShellCertificateScanner {

    public static void listCertsWithPowerShell() {
        try {
            String command = "powershell -Command \"Get-ChildItem -Path Cert:\\CurrentUser\\My | " +
                           "Where-Object { $_.Issuer -like '*казначейство*' } | " +
                           "Select-Object Subject, Issuer, Thumbprint, SerialNumber | Format-List\"";
            
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            
            System.out.println("=== СЕРТИФИКАТЫ ЧЕРЕЗ POWERSHELL ===");
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            System.out.println("Ошибка PowerShell: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        listCertsWithPowerShell();
    }
}