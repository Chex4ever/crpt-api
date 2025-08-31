package ru.selsup.trueapi;

import java.time.Instant;

public interface TokenStorage {
    void saveToken(String token, Instant expirationTime);

    String getToken();

    boolean isValid();

    void clear();

    Instant getExpirationTime();
}