package ru.selsup.crypto;

public class SigningException extends Exception {
    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }

    public SigningException(String message) {
        super(message);
    }
}