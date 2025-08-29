package ru.selsup.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class CryptoSigner {
    public static final Logger logger = LoggerFactory.getLogger(CryptoSigner.class);
    // Метод с перебором алгоритмов
    public String sign(String data, CertificateInfo certInfo, String algorithm) throws CryptoException {

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] result = sign(dataBytes, certInfo, algorithm);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new CryptoException("Ошибка при подписи алгоритмом "+ algorithm);
        }
    }

    public String sign(String data, CertificateInfo certInfo) throws CryptoException {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] result = sign(dataBytes, certInfo);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new CryptoException("Ошибка при подписи алгоритмом по-умолчанию");
        }
    }

    public byte[] sign(byte[] data, CertificateInfo certInfo) throws CryptoException {
        List<String> algorithmsToTry = Arrays.stream(CryptoAlgorithms.values()).map(CryptoAlgorithms::getAlgorithmName).toList();
        Exception lastError = null;
        for (String algorithm : algorithmsToTry) {
            try {
                return sign(data, certInfo, algorithm);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw new CryptoException("Не удалось подписать ни одним алгоритмом", lastError);
    }

    public byte[] sign(byte[] data, CertificateInfo certInfo, String algorithm) throws CryptoException {
        try {
            KeyStore ks = KeyStore.getInstance(certInfo.getStoreType(), "JCP");
            ks.load(null, null);

            PrivateKey privateKey = (PrivateKey) ks.getKey(certInfo.getAlias(), null);
            if (privateKey == null) {
                throw new CryptoException("Не удалось получить приватный ключ");
            }
            String signatureAlgorithm = algorithm != null ? algorithm :
                    CryptoAlgorithms.GOST3411_2012_256_WITH_GOST3410_2012_256.getAlgorithmName();

            Signature signature = Signature.getInstance(signatureAlgorithm, "JCP");
            signature.initSign(privateKey);
            signature.update(data);
            logger.info("Подписано алгоритмом {}", signatureAlgorithm);
            return signature.sign();

        } catch (Exception e) {
            throw new CryptoException("Ошибка подписи: " + e.getMessage(), e);
        }
    }
//    private static String getSignatureAlgorithmForCertificate(CertificateInfo certInfo) {
//        try {
//            KeyStore ks = KeyStore.getInstance(certInfo.getStoreType(), "JCSP");
//            ks.load(null, null);
//
//            Key key = ks.getKey(certInfo.getAlias(), null);
//            if (key != null) {
//                return getSignatureAlgorithmForKey(key);
//            }
//
//            // Если не удалось получить ключ, пробуем по умолчанию
//            java.security.cert.Certificate cert = ks.getCertificate(certInfo.getAlias());
//            if (cert != null) {
//                return getSignatureAlgorithmForKey(cert.getPublicKey());
//            }
//
//        } catch (Exception e) {
//            // Не удалось определить
//        }
//
//        // Возвращаем самый распространенный алгоритм
//        return "GOST3411withGOST3410";
//    }
//    private static String getSignatureAlgorithmForKey(Key key) {
//        String algorithm = key.getAlgorithm();
//
//        if ("ECGOST3410".equalsIgnoreCase(algorithm)) {
//            return "GOST3411withECGOST3410";
//        } else if (algorithm.contains("2012")) {
//            if (algorithm.contains("256")) {
//                return "GOST3411-2012-256withGOST3410-2012-256";
//            } else if (algorithm.contains("512")) {
//                return "GOST3411-2012-512withGOST3410-2012-512";
//            }
//        }
//
//        return "GOST3411withGOST3410";
//    }
}