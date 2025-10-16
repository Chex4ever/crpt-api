package ru.selsup.trueapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FileTokenStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void whenSaveToken_thenCanRetrieveIt() {
        FileTokenStorage storage = new FileTokenStorage(tempDir.resolve("token.json").toString());
        String expectedToken = "test-token-123";
        Instant expirationTime = Instant.now().plusSeconds(3600);

        storage.saveToken(expectedToken, expirationTime);
        String actualToken = storage.getToken();

        assertThat(actualToken).isEqualTo(expectedToken);
    }

    @Test
    void whenTokenExpired_thenIsValidReturnsFalse() {
        FileTokenStorage storage = new FileTokenStorage(tempDir.resolve("token.json").toString());
        storage.saveToken("expired-token", Instant.now().minusSeconds(3600));

        assertThat(storage.isValid()).isFalse();
    }

    @Test
    void whenClearToken_thenTokenIsRemoved() {
        FileTokenStorage storage = new FileTokenStorage(tempDir.resolve("token.json").toString());
        storage.saveToken("test-token", Instant.now().plusSeconds(3600));

        storage.clear();

        assertThat(storage.isValid()).isFalse();
        assertThat(storage.getToken()).isNull();
    }
}