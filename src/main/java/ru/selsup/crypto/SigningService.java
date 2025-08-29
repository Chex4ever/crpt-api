package ru.selsup.crypto;

public interface SigningService {
    CertificateInfo selectedCert();
    String signData(String data) throws SigningException;
    byte[] signData(byte[] data) throws SigningException;
}