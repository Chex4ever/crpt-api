package ru.selsup.crypto;

import java.util.List;
import java.util.Map;
// или: import ru.selsup.crypto.gui.GuiCertificateSelector;

public class CryptoUtils {
    private final StorageDiscoverer discoverer;
    private final CertificateSelector selector;
    private final CryptoSigner signer;

    public CryptoUtils(CertificateSelector selector) {
        this.discoverer = new StorageDiscoverer();
        this.selector = selector;
        this.signer = new CryptoSigner();
    }

    public void run() {
        try {
            // Поиск сертификатов
            selector.showMessage("Поиск сертификатов...");
            Map<String, List<CertificateInfo>> allCertificates = discoverer.discoverAllContainers();

            // Собираем все сертификаты в один список
            List<CertificateInfo> allCerts = allCertificates.values().stream()
                    .flatMap(List::stream)
                    .toList();

            if (allCerts.isEmpty()) {
                return;
            }
            CertificateInfo selectedCert = allCerts.get(0);
            if (allCerts.size() > 1) {
                // Выбор сертификата
                selectedCert = selector.selectCertificate(allCerts);
                if (selectedCert == null) {
                    return;
                }
            }

//            // Практический тест алгоритмов
//            testAlgorithmsPractical(selectedCert);

            byte[] testData = "Тестовые данные".getBytes("UTF-8");

            // Пробуем подписать
            try {
                byte[] signature = signer.sign(testData, selectedCert);
                selector.showMessage("✓ Подпись создана успешно!");

            } catch (CryptoException e) {
                selector.showMessage("Пробуем альтернативные алгоритмы...");
                byte[] signature = signer.sign(testData, selectedCert);
                selector.showMessage("✓ Подпись создана через fallback!");
            }

        } catch (Exception e) {
            selector.showError("Ошибка приложения: " + e.getMessage());
        }
    }
    public static String chooseDefaultAlgorithm() {
        // Вернем первый элемент из списка алгоритмов в качестве алгоритма по умолчанию
        return CryptoAlgorithms.GOST3411_2012_256_WITH_GOST3410_2012_256.getAlgorithmName();
    }

}