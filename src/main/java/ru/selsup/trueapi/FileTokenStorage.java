package ru.selsup.trueapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class FileTokenStorage implements TokenStorage {
    private final Path tokenFile;
    private final ObjectMapper objectMapper;

    public FileTokenStorage(String filePath) {
        this.tokenFile = Paths.get(filePath);
        this.objectMapper = createObjectMapper();
        initializeFile();
    }

    private void initializeFile() {
        try {
            if (!Files.exists(tokenFile)) {
                Files.createDirectories(tokenFile.getParent());
                Files.writeString(tokenFile, "{}");
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать файл с токеном", e);
        }
    }

    @Override
    public void saveToken(String token, Instant expirationTime) {
        try {
            TokenData tokenData = new TokenData(token, expirationTime);
            String json = objectMapper.writeValueAsString(tokenData);
            Files.writeString(tokenFile, json);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить токен в файл", e);
        }
    }

    @Override
    public String getToken() {
        try {
            TokenData tokenData = readTokenData();
            return tokenData != null ? tokenData.token : null;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать токен из файла", e);
        }
    }

    @Override
    public boolean isValid() {
        try {
            TokenData tokenData = readTokenData();
            return tokenData != null && 
                   tokenData.token != null &&
                   Instant.now().isBefore(tokenData.expirationTime);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            Files.writeString(tokenFile, "{}");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось отчистить файл с токеном", e);
        }
    }

    @Override
    public Instant getExpirationTime() {
        try {
            TokenData tokenData = readTokenData();
            return tokenData != null ? tokenData.expirationTime : null;
        } catch (IOException e) {
            return null;
        }
    }

    private TokenData readTokenData() throws IOException {
        if (!Files.exists(tokenFile)) {
            return null;
        }
        
        String json = Files.readString(tokenFile);
        return objectMapper.readValue(json, TokenData.class);
    }

    private static class TokenData {
        public String token;
        public Instant expirationTime;

        public TokenData() {} // Для Jackson

        public TokenData(String token, Instant expirationTime) {
            this.token = token;
            this.expirationTime = expirationTime;
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}