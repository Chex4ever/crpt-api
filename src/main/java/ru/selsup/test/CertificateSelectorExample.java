package ru.selsup.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.cert.X509Certificate;

public class CertificateSelectorExample extends JFrame {

    private JTextArea resultArea;

    public CertificateSelectorExample() {
        setTitle("Пример выбора сертификата");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createUI();
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Кнопка выбора сертификата
        JButton selectButton = new JButton("Выбрать сертификат");
        selectButton.addActionListener(this::selectCertificate);

        // Область для вывода результата
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        mainPanel.add(selectButton, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(mainPanel);
    }

    private void selectCertificate(ActionEvent e) {
        // Показываем диалог выбора сертификата
        X509Certificate selectedCert = CertificateSelectorDialog.showCertificateDialog(this);


        if (selectedCert != null) {
            // Отображаем информацию о выбранном сертификате
            String certInfo = String.format(
                    "=== ВЫБРАН СЕРТИФИКАТ ===\n\n" +
                            "Владелец: %s\n\n" +
                            "Издатель: %s\n\n" +
                            "Серийный номер: %s\n\n" +
                            "Действует с: %s\n\n" +
                            "Действует по: %s\n\n" +
                            "Алгоритм подписи: %s\n\n" +
                            "Версия: %s",
                    selectedCert.getSubjectDN(),
                    selectedCert.getIssuerDN(),
                    selectedCert.getSerialNumber(),
                    selectedCert.getNotBefore(),
                    selectedCert.getNotAfter(),
                    selectedCert.getSigAlgName(),
                    selectedCert.getVersion()
            );

            resultArea.setText(certInfo);

            // Можно использовать сертификат для подписи
            signWithCertificate(selectedCert);

        } else {
            resultArea.setText("Выбор сертификата отменен");
        }
    }

    private void signWithCertificate(X509Certificate certificate) {
        // Здесь ваша логика подписи
        System.out.println("Начинаем процесс подписи с выбранным сертификатом...");

        try {
            // Получаем приватный ключ (это упрощенный пример)
            // В реальности нужно получить ключ из KeyStore
            // PrivateKey privateKey = keyStore.getKey(alias, null);

            // Ваша логика подписи здесь
            // byte[] signature = signData(dataToSign, privateKey);

            resultArea.append("\n\n=== ПОДПИСЬ ===\n");
            resultArea.append("Процесс подписи запущен для выбранного сертификата\n");

        } catch (Exception e) {
            resultArea.append("\n\nОшибка при подписи: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new CertificateSelectorExample().setVisible(true);
        });
    }
}