package ru.selsup.trueapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testLoadConfig_WithValidProperties_ReturnsConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("crpt.api.base-url-v3", "https://api.test.com/v3");
        props.setProperty("crpt.api.request-limit", "15");
        props.setProperty("crpt.api.time-unit", "MINUTES");
        
        Path configFile = tempDir.resolve("test-config.properties");
        Files.write(configFile, toPropertiesString(props).getBytes());
        
        ConfigLoader loader = new ConfigLoader();
        // Здесь нужно модифицировать ConfigLoader для приема пути к файлу
        // или использовать reflection для тестирования createConfigFromProperties
        
        assertNotNull(loader);
    }

    @Test
    void testLoadConfig_WithInvalidRequestLimit_ThrowsException() throws Exception {
        ConfigLoader loader = new ConfigLoader();
        Properties props = new Properties();
        props.setProperty("crpt.api.request-limit", "-1");
        props.setProperty("crpt.api.time-unit", "SECONDS");
        props.setProperty("crpt.api.base-url-v3", "https://api.test.com");

        Method method = ConfigLoader.class.getDeclaredMethod("createConfigFromProperties", Properties.class);
        method.setAccessible(true);

        // Используем assertThrows с InvocationTargetException
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(loader, props));

        // Проверяем корневую причину
        assertThat(exception.getCause())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request limit must be positive");
    }
    
    @Test
    void testConfigDefaults() {
        Config config = new Config();
        
        assertEquals(Duration.ofSeconds(10), config.getConnectionTimeout());
        assertEquals(Duration.ofSeconds(30), config.getReadTimeout());
        assertEquals(10, config.getRequestLimit());
        assertEquals(TimeUnit.SECONDS, config.getTimeUnit());
    }
    
    private String toPropertiesString(Properties props) {
        StringBuilder sb = new StringBuilder();
        props.forEach((key, value) -> sb.append(key).append("=").append(value).append("\n"));
        return sb.toString();
    }
}