package ru.selsup.crypto;

import javax.swing.*;
import java.util.List;

public class GuiCertificateSelector implements CertificateSelector {
    @Override
    public CertificateInfo selectCertificate(List<CertificateInfo> certificates) {
        if (certificates.isEmpty()) {
            showError("Не найдено доступных сертификатов");
            return null;
        }

        String[] options = certificates.stream()
                .map(CertificateInfo::toString)
                .toArray(String[]::new);

        int choice = JOptionPane.showOptionDialog(
                null,
                "Выберите сертификат для подписи:",
                "Выбор сертификата",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        return choice >= 0 ? certificates.get(choice) : null;
    }

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message, "Информация", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void showError(String error) {
        JOptionPane.showMessageDialog(null, error, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}