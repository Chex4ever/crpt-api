# Java Client for CRPT (Честный ЗНАК) API
Потокобезопасный Java-клиент для работы с API ГИС МТ (Честный ЗНАК). Библиотека предоставляет удобный способ взаимодействия с системой маркировки, поддерживает ограничение частоты запросов (rate limiting), автоматическую аутентификацию с использованием ГОСТ-сертификатов и подписание документов по стандарту CAdES.

## ⚙️ Основные возможности
- Создание документов: Отправка документов для ввода товаров в оборот.
- Потокобезопасность: Клиент полностью безопасен для использования в многопоточных средах.
- Ограничение запросов (Rate Limiting): Встроенный механизм для соблюдения лимитов API Честного Знака.
- Аутентификация по ГОСТ-сертификатам: Автоматическое получение и обновление JWT-токена с использованием КриптоПро JCSP.
- Подписание документов (CAdES): Подписание JSON-документов по стандарту CAdES-BES.
- Поиск участников ОБОРОТА: Проверка организаций по ИНН.
- Гибкая конфигурация: Настройка через файл application.properties или программно.

## 🚀 Быстрый старт
### 1. Требования
-  Java 17 или выше
- КриптоПро JCP и КриптоПро CSP
- Действующий ГОСТ-сертификат, установленный в одно из поддерживаемых хранилищ (HDIMAGE, REGISTRY и т.д.)
- Учетная запись в ГИС МТ (Честный ЗНАК) с настроенными правами доступа.

### 2. Добавление в проект
Склонируйте репозиторий и соберите проект с помощью Maven или Gradle. Или же скопируйте исходные файлы в свой проект.

```bash
git clone https://github.com/Chex4ever/crpt-api
````
### 3. Настройка конфигурации
Создайте файл src/main/resources/application.properties:

```properties
# Базовые URL API Честного Знака
crpt.api.base-url-v3=https://ismp.crpt.ru/api/v3/
crpt.api.base-url-v4=https://ismp.crpt.ru/api/v4/

# Конечные точки API
crpt.api.auth-get-data-endpoint=/auth/cert/key
crpt.api.auth-get-token-endpoint=/auth/cert/
crpt.api.create-document-endpoint=/lk/documents/create

# Настройки таймаутов (в миллисекундах)
crpt.api.connection-timeout=10000
crpt.api.read-timeout=30000
crpt.api.authenticate-timeout=15000
crpt.api.create-document-timeout=30000

# Настройки Rate Limiting (например, не более 10 запросов в секунду)
crpt.api.request-limit=10
crpt.api.time-unit=SECONDS
```
### 4. Пример использования
```java
   package ru.selsup.trueapi;

import ru.selsup.crypto.JcspSigningService;
import ru.selsup.crypto.StorageDiscoverer;
import ru.selsup.trueapi.model.Document;
import ru.selsup.trueapi.model.DocumentType;
import ru.selsup.trueapi.model.ProductGroup;

import java.util.concurrent.CompletableFuture;

public class Example {
public static void main(String[] args) throws Exception {
// 1. Загрузка конфигурации
ConfigLoader configLoader = new ConfigLoader();
Config config = configLoader.loadConfig();

        // 2. Инициализация сервиса подписания
        StorageDiscoverer discoverer = new StorageDiscoverer();
        JcspSigningService signer = new JcspSigningService(discoverer);

        // 3. Инициализация хранилища токенов
        TokenStorage tokenStorage = TokenStorageFactory.createStorage(
                TokenStorageFactory.StorageType.FILE, 
                "tokens/auth_token.json"
        );

        // 4. Создание клиента API
        CrptApi api = new CrptApi(signer, config, tokenStorage);

        // 5. Аутентификация (произойдет автоматически при первом вызове, но можно и явно)
        api.authenticate().get();

        // 6. Создание и отправка документа
        Document document = new Document();
        document.setType(DocumentType.LP_INTRODUCE_GOODS);
        document.setProductGroup(ProductGroup.MILK);

        // ... Заполнение остальных полей документа ...

        CompletableFuture<String> responseFuture = api.sendDocument(document);
        
        // Обработка ответа
        responseFuture.thenAccept(response -> {
            System.out.println("Документ успешно создан: " + response);
        }).exceptionally(throwable -> {
            System.err.println("Ошибка при создании документа: " + throwable.getMessage());
            return null;
        }).get(); // или используйте неблокирующую обработку
    }
}
```
## 📚 Ключевые компоненты
- CrptApi: Основной класс для взаимодействия с API. Потокобезопасен, реализует rate limiting.
- JcspSigningService: Сервис для подписания данных с использованием ГОСТ-сертификатов через КриптоПро JCSP.
- Config / ConfigLoader: Классы для гибкой настройки клиента через properties-файл.
- TokenStorage: Интерфейс для хранения JWT-токенов. Реализация FileTokenStorage сохраняет токены в файл.
- StorageDiscoverer: Утилита для поиска доступных ГОСТ-сертификатов в системе.

## 🔧 Детали реализации
### Rate Limiting
Реализован с помощью ReentrantLock и Condition. Счетчик запросов автоматически сбрасывается через заданные промежутки времени. При превышении лимита потоки блокируются до следующего периода.
### Аутентификация
Процесс аутентификации состоит из трех этапов:
1. Запрос данных для подписи - получение UUID и случайной строки от сервера.
2. Подписание данных - создание подписи с помощью ГОСТ-сертификата.
3. Обмен на токен - отправка подписанных данных для получения JWT.
Токен кэшируется и автоматически обновляется по истечении срока действия.

### Подписание документов
Документы подписываются в формате CAdES-BES с использованием алгоритмов ГОСТ 34.10-2012. Подпись включает цепочку сертификатов и временную метку.

## 🛠️ Разработчикам
### Расширение функционала
Для добавления новых методов API:
1. Создайте соответствующий модель данных.
2. Добавьте метод в CrptApi, используя существующие шаблоны для обработки запросов.
3. Не забудьте использовать acquireRequestPermit() для соблюдения rate limiting.

### Создание кастомного TokenStorage
Реализуйте интерфейс TokenStorage для хранения токенов в БД, Redis и т.д.:

```java
public class DatabaseTokenStorage implements TokenStorage {
// ... ваша реализация ...
}
```
## 📄 Логирование
Проект использует SLF4J. Для вывода логов добавьте одну из реализаций (например, Logback) в зависимости вашего проекта.

## ⚖️ Лицензия
Этот проект распространяется под лицензией MIT. Подробнее см. в файле LICENSE.

## 🤝 Вклад в проект
Вклады приветствуются! Не стесняйтесь открывать issues и pull requests.
---
Примечание: Данный клиент является неофициальной реализацией и не поддерживается ФНС России или оператором ГИС МТ.