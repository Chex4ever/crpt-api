package ru.selsup.test;

import ru.CryptoPro.JCP.JCP;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CertificateSelectorDialog extends JDialog {

    private JComboBox<String> certComboBox;
    private final List<X509Certificate> certificates = new ArrayList<>();
    private X509Certificate selectedCertificate;
    private CompletableFuture<X509Certificate> resultFuture;

    public CertificateSelectorDialog(Frame owner) {
        super(owner, "Выбор сертификата", true);
        initialize();
    }

    private void initialize() {
        setSize(600, 300);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        loadCertificates();

        createUI();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (resultFuture != null && !resultFuture.isDone()) {
                    resultFuture.complete(null);
                }
            }
        });
    }

    private void loadCertificates() {
        try {
            if (java.security.Security.getProvider("JCP") == null) {
                java.security.Security.addProvider(new JCP());
            }

            KeyStore keyStore = KeyStore.getInstance("HDImage", "JCP");
            keyStore.load(null, null);

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                if (cert != null) {
                    certificates.add(cert);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки сертификатов: " + e.getMessage());
        }
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель выбора сертификата
        JPanel certPanel = new JPanel(new BorderLayout(5, 5));
        certPanel.add(new JLabel("Выберите сертификат для подписания:"), BorderLayout.NORTH);

        certComboBox = new JComboBox<>();
        for (X509Certificate cert : certificates) {
            String certInfo = String.format("%s (действует до: %s)",
                    cert.getSubjectDN().toString(),
                    cert.getNotAfter().toString().substring(0, 10));
            certComboBox.addItem(certInfo);
        }

        certPanel.add(certComboBox, BorderLayout.CENTER);

        // Кнопка для просмотра деталей сертификата
        JButton detailsButton = new JButton("Подробнее");
        detailsButton.addActionListener(this::showCertificateDetails);
        certPanel.add(detailsButton, BorderLayout.EAST);

        mainPanel.add(certPanel, BorderLayout.NORTH);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("Выбрать");
        okButton.addActionListener(this::onOk);

        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(this::onCancel);

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void onOk(ActionEvent e) {
        int selectedIndex = certComboBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < certificates.size()) {
            selectedCertificate = certificates.get(selectedIndex);
        }

        if (resultFuture != null && !resultFuture.isDone()) {
            resultFuture.complete(selectedCertificate);
        }

        dispose();
    }

    private void onCancel(ActionEvent e) {
        selectedCertificate = null;
        if (resultFuture != null && !resultFuture.isDone()) {
            resultFuture.complete(null);
        }
        dispose();
    }

    private void showCertificateDetails(ActionEvent e) {
        int selectedIndex = certComboBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < certificates.size()) {
            X509Certificate cert = certificates.get(selectedIndex);

            String details = String.format(
                    "Владелец: %s\n" +
                            "Издатель: %s\n" +
                            "Серийный номер: %s\n" +
                            "Действует с: %s\n" +
                            "Действует по: %s\n" +
                            "Алгоритм: %s",
                    cert.getSubjectDN(),
                    cert.getIssuerDN(),
                    cert.getSerialNumber(),
                    cert.getNotBefore(),
                    cert.getNotAfter(),
                    cert.getSigAlgName()
            );

            JTextArea textArea = new JTextArea(details, 10, 50);
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            JOptionPane.showMessageDialog(this, scrollPane, "Детали сертификата",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public CompletableFuture<X509Certificate> getResultFuture() {
        resultFuture = new CompletableFuture<>();
        return resultFuture;
    }

    public X509Certificate getSelectedCertificate() {
        return selectedCertificate;
    }

    // Статический метод для удобного использования
    public static X509Certificate showCertificateDialog(Component parent) {
        Frame frame = parent instanceof Frame ? (Frame) parent :
                (Frame) SwingUtilities.getWindowAncestor(parent);

        CertificateSelectorDialog dialog = new CertificateSelectorDialog(frame);
        CompletableFuture<X509Certificate> future = dialog.getResultFuture();

        dialog.setVisible(true);

        try {
            return future.get();
        } catch (Exception e) {
            return null;
        }
    }
}