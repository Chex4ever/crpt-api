package ru.selsup.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    public Config loadConfig() {
        Properties props = loadProperties();
        return createConfigFromProperties(props);
    }

    public static Config loadDefaultConfig() {
        return new ConfigLoader().loadConfig();
    }

    private Properties loadProperties() {
        return loadProperties("application.properties");
    }

    private Properties loadProperties(String configPath) {
        Properties props = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (inputStream == null) {
                logger.error("Файл {} не найден в classpath", configPath);
                throw new IllegalStateException("Файл " + configPath + " не найден в classpath");
            }
            props.load(inputStream);

            logger.info("Загружены свойства из {}", configPath);
            return props;
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла конфигурации", e);
            throw new UncheckedIOException("Ошибка при чтении файла конфигурации", e);
        }
    }

    private Config createConfigFromProperties(Properties props) {
        Config config = new Config();

        // Маппинг свойств через рефлексию (автоматически)
        mapPropertiesAutomatically(props, config);

        // Или явный маппинг (более надежно)
        mapPropertiesExplicitly(props, config);

        if (config.getRequestLimit() <= 0 || config.getTimeUnit() == null) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        return config;
    }

    // Автоматический маппинг через рефлексию
    private void mapPropertiesAutomatically(Properties props, Config config) {
        try {
            Field[] fields = Config.class.getDeclaredFields();
            for (Field field : fields) {
                String propertyName = "crpt.api." + camelToKebab(field.getName());
                String value = props.getProperty(propertyName);

                if (value != null) {
                    field.setAccessible(true);
                    setFieldValue(field, config, value);
                }
            }
        } catch (Exception e) {
            logger.warn("Ошибка автоматического маппинга свойств, используем явный маппинг", e);
        }
    }

    // Явный маппинг (более надежный)
    private void mapPropertiesExplicitly(Properties props, Config config) {
        config.setBaseUrl(props.getProperty("crpt.api.base-url"));
        config.setAuthEndpoint(props.getProperty("crpt.api.auth-endpoint"));
        config.setCreateDocumentEndpoint(props.getProperty("crpt.api.create-document-endpoint"));

        setDurationProperty(props, "crpt.api.connection-timeout", config::setConnectionTimeout);
        setDurationProperty(props, "crpt.api.read-timeout", config::setReadTimeout);
        setDurationProperty(props, "crpt.api.authenticate-timeout", config::setAuthenticateTimeout);
        setDurationProperty(props, "crpt.api.create-document-timeout", config::setCreateDocumentTimeout);

        setIntProperty(props, "crpt.api.request-limit", config::setRequestLimit);
        setTimeUnitProperty(props, "crpt.api.time-unit", config::setTimeUnit);
    }

    // Вспомогательные методы
    private String camelToKebab(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private void setFieldValue(Field field, Object target, String value) throws IllegalAccessException {
        Class<?> type = field.getType();

        if (type == String.class) {
            field.set(target, value);
        } else if (type == int.class || type == Integer.class) {
            field.set(target, Integer.parseInt(value));
        } else if (type == Duration.class) {
            field.set(target, Duration.ofMillis(Long.parseLong(value)));
        } else if (type == TimeUnit.class) {
            field.set(target, TimeUnit.valueOf(value.toUpperCase()));
        }
    }

    private void setDurationProperty(Properties props, String key, java.util.function.Consumer<Duration> setter) {
        String value = props.getProperty(key);
        if (value != null) {
            setter.accept(Duration.ofMillis(Long.parseLong(value)));
        }
    }

    private void setIntProperty(Properties props, String key, java.util.function.Consumer<Integer> setter) {
        String value = props.getProperty(key);
        if (value != null) {
            setter.accept(Integer.parseInt(value));
        }
    }

    private void setTimeUnitProperty(Properties props, String key, java.util.function.Consumer<TimeUnit> setter) {
        String value = props.getProperty(key);
        if (value != null) {
            setter.accept(TimeUnit.valueOf(value.toUpperCase()));
        }
    }
}