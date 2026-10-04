package ru.npi.kbju.integration.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import ru.npi.kbju.application.catalog.DirectoryLoadException;
import ru.npi.kbju.application.catalog.ProductDirectory;
import ru.npi.kbju.domain.FoodProduct;

/** HTTP-адаптер справочника продуктов с ограничениями ожидания и размера. */
public final class HttpProductDirectory implements ProductDirectory {
    /** Стандартный предел ответа учебного справочника. */
    public static final int DEFAULT_MAX_RESPONSE_BYTES = 64 * 1_024;

    private final URI endpoint;
    private final Duration requestTimeout;
    private final int maxResponseBytes;
    private final HttpClient client;
    private final FoodProductJsonMapper jsonMapper;

    /** Создаёт адаптер с явными пределами соединения, запроса и ответа. */
    public HttpProductDirectory(
            URI endpoint,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxResponseBytes
    ) {
        this.endpoint = requireHttpEndpoint(endpoint);
        this.requestTimeout = requirePositive(requestTimeout, "Таймаут запроса");
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("Предел ответа должен быть положительным");
        }
        this.maxResponseBytes = maxResponseBytes;
        this.client = HttpClient.newBuilder()
                .connectTimeout(requirePositive(connectTimeout, "Таймаут соединения"))
                .build();
        this.jsonMapper = new FoodProductJsonMapper();
    }

    /** Создаёт адаптер с курсовыми таймаутами. */
    public static HttpProductDirectory standard(URI endpoint) {
        return new HttpProductDirectory(
                endpoint, Duration.ofSeconds(2), Duration.ofSeconds(3),
                DEFAULT_MAX_RESPONSE_BYTES);
    }

    /** Выполняет GET, проверяет HTTP и только затем передаёт тело JSON-мапперу. */
    @Override
    public List<FoodProduct> fetch() {
        var request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException exception) {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.TIMEOUT,
                    "Истёк таймаут запроса к справочнику", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.INTERRUPTED,
                    "Запрос к справочнику прерван", exception);
        } catch (IOException exception) {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.TRANSPORT,
                    "Не удалось связаться со справочником", exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw DirectoryLoadException.httpStatus(response.statusCode());
        }
        var contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.CONTRACT,
                    "Сервер вернул не JSON: Content-Type=" + contentType);
        }
        int responseBytes = response.body().getBytes(StandardCharsets.UTF_8).length;
        if (responseBytes > maxResponseBytes) {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.CONTRACT,
                    "Ответ справочника превысил " + maxResponseBytes + " байт");
        }
        return jsonMapper.parse(response.body());
    }

    private static URI requireHttpEndpoint(URI value) {
        Objects.requireNonNull(value, "Endpoint обязателен");
        if (!("http".equalsIgnoreCase(value.getScheme())
                || "https".equalsIgnoreCase(value.getScheme()))) {
            throw new IllegalArgumentException("Endpoint должен использовать HTTP или HTTPS");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String field) {
        Objects.requireNonNull(value, field + " обязателен");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " должен быть положительным");
        }
        return value;
    }
}
