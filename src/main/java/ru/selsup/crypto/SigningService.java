package ru.selsup.crypto;

public interface SigningService {
    CertificateInfo selectedCert();

    String signData(String data, boolean detached) throws SigningException;
}