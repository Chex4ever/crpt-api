package ru.selsup.crypto;

import java.io.File;
import java.security.KeyStore;
import java.util.*;

public class StorageDiscoverer {
    private final List<String> mainStoreTypes = Arrays.asList("HDIMAGE", "REGISTRY", "FAT12_K", "CNG");

    public Map<String, List<CertificateInfo>> discoverAllContainers() {
        Map<String, List<CertificateInfo>> result = new HashMap<>();

        // Проверяем основные хранилища
        for (String storeType : mainStoreTypes) {
            discoverInStoreType(storeType, result);
        }

        // Проверяем FAT12_* для всех дисков
        File[] roots = File.listRoots();
        char[] driveLetters = Arrays.stream(roots)
                .map(File::getAbsolutePath)
                .filter(path -> !path.isEmpty())
                .map(path -> Character.toUpperCase(path.charAt(0)))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString()
                .toCharArray();
        for (char drive : driveLetters) {
            discoverInStoreType("FAT12_" + drive, result);
        }

        return result;
    }

    private void discoverInStoreType(String storeType, Map<String, List<CertificateInfo>> result) {
        try {
            KeyStore ks = KeyStore.getInstance(storeType, "JCSP");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            List<CertificateInfo> certificates = new ArrayList<>();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                CertificateInfo certInfo = extractCertificateInfo(ks, alias, storeType);
                if (certInfo != null) {
                    certificates.add(certInfo);
                }
            }

            if (!certificates.isEmpty()) {
                result.put(storeType, certificates);
            }

        } catch (Exception e) {
            // Хранилище недоступно - пропускаем
        }
    }

    private CertificateInfo extractCertificateInfo(KeyStore ks, String alias, String storeType) {
        try {
            java.security.cert.Certificate cert = ks.getCertificate(alias);
            if (cert instanceof java.security.cert.X509Certificate x509) {
                return new CertificateInfo(
                        alias,
                        x509.getSubjectX500Principal().getName(),
                        x509.getIssuerX500Principal().getName(),
                        x509.getNotBefore(),
                        x509.getNotAfter(),
                        storeType
                );
            }
        } catch (Exception e) {
            // Не удалось извлечь информацию
        }
        return null;
    }
}