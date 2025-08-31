package ru.selsup.trueapi;

public class TokenStorageFactory {

    public enum StorageType {
        FILE,
        // Добавьте другие типы позже: DATABASE, MEMORY, REDIS и т.д.
    }

    public static TokenStorage createStorage(StorageType type, String config) {
        switch (type) {
            case FILE:
                return new FileTokenStorage(config);
            default:
                throw new IllegalArgumentException("Неподдерживаемый тип храилища: " + type);
        }
    }
}