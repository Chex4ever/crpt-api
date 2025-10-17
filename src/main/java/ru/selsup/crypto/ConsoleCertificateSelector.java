package ru.selsup.crypto;

import java.util.List;
import java.util.Scanner;

public class ConsoleCertificateSelector implements CertificateSelector {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public CertificateInfo selectCertificate(List<CertificateInfo> certificates) {
        if (certificates.isEmpty()) {
            showError("Не найдено доступных сертификатов");
            return null;
        }

        System.out.println("\nДоступные сертификаты:");
        for (int i = 0; i < certificates.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, certificates.get(i));
        }

        System.out.print("\nВыберите сертификат (1-" + certificates.size() + "): ");
        try {
            int choice = Integer.parseInt(scanner.nextLine()) - 1;
            if (choice >= 0 && choice < certificates.size()) {
                return certificates.get(choice);
            }
        } catch (NumberFormatException e) {
            showError("Неверный выбор");
        }

        return null;
    }

    @Override
    public void showMessage(String message) {
        System.out.println("ℹ️ " + message);
    }

    @Override
    public void showError(String error) {
        System.err.println("❌ " + error);
    }
}