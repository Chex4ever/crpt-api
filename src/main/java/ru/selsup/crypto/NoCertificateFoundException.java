package ru.selsup.crypto;

public class NoCertificateFoundException extends Exception {
    public NoCertificateFoundException(String message) {
        super(message);
    }
}