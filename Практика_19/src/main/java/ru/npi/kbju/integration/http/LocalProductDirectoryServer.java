package ru.npi.kbju.integration.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

/** Локальный loopback-сервер с воспроизводимыми HTTP/JSON-сценариями. */
public final class LocalProductDirectoryServer implements AutoCloseable {
    /** Ответ, который вернёт следующий запрос. */
    public enum Scenario {
        VALID,
        SERVER_ERROR,
        MALFORMED_JSON,
        INVALID_PRODUCT,
        DELAYED
    }

    private static final String PATH = "/api/v1/products";
    private static final String VALID_JSON = """
            {
              "version": 1,
              "products": [
                {"code":"FOOD-001","name":"Гречневая крупа","energyKcal":343,"proteinGrams":13.3,"fatGrams":3.4,"carbohydrateGrams":71.5},
                {"code":"FOOD-002","name":"Куриная грудка","energyKcal":165,"proteinGrams":31.0,"fatGrams":3.6,"carbohydrateGrams":0.0},
                {"code":"FOOD-003","name":"Яблоко","energyKcal":52,"proteinGrams":0.3,"fatGrams":0.2,"carbohydrateGrams":14.0}
              ]
            }
            """;
    private static final String INVALID_PRODUCT_JSON = """
            {"version":1,"products":[
              {"code":"FOOD-ERR","name":"Ошибочный продукт","energyKcal":-1,"proteinGrams":1,"fatGrams":1,"carbohydrateGrams":1}
            ]}
            """;

    private final int requestedPort;
    private final AtomicReference<Scenario> scenario;
    private final AtomicInteger requestCount = new AtomicInteger();
    private HttpServer server;
    private ExecutorService executor;
    private int boundPort;

    /** Создаёт сервер на свободном порту. */
    public LocalProductDirectoryServer(Scenario initialScenario) {
        this(initialScenario, 0);
    }

    /** Создаёт сервер на указанном порту; ноль выбирает свободный. */
    public LocalProductDirectoryServer(Scenario initialScenario, int requestedPort) {
        this.scenario = new AtomicReference<>(
                Objects.requireNonNull(initialScenario, "Сценарий сервера обязателен"));
        if (requestedPort < 0 || requestedPort > 65_535) {
            throw new IllegalArgumentException("Порт должен быть от 0 до 65535");
        }
        this.requestedPort = requestedPort;
    }

    /** Занимает loopback-порт и запускает обработчик в виртуальных потоках. */
    public URI start() {
        if (server != null) {
            throw new IllegalStateException("Учебный сервер уже запущен");
        }
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", requestedPort), 0);
            server.createContext(PATH, this::handle);
            executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);
            server.start();
            boundPort = server.getAddress().getPort();
            return endpoint();
        } catch (IOException exception) {
            close();
            throw new IllegalStateException("Не удалось запустить учебный HTTP-сервер", exception);
        }
    }

    /** Переключает воспроизводимый ответ без перезапуска сервера. */
    public void use(Scenario nextScenario) {
        scenario.set(Objects.requireNonNull(nextScenario, "Сценарий сервера обязателен"));
    }

    /** Возвращает фактический порт после start. */
    public int port() {
        if (boundPort == 0) {
            throw new IllegalStateException("Сервер ещё не запущен");
        }
        return boundPort;
    }

    /** Возвращает число полученных запросов для проверки bounded retry. */
    public int requestCount() {
        return requestCount.get();
    }

    private URI endpoint() {
        return URI.create("http://127.0.0.1:" + boundPort + PATH);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            requestCount.incrementAndGet();
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"error\":\"method not allowed\"}", "application/json; charset=utf-8");
                return;
            }
            switch (scenario.get()) {
                case VALID -> send(exchange, 200, VALID_JSON, "application/json; charset=utf-8");
                case SERVER_ERROR -> send(
                        exchange, 500, "{\"error\":\"temporary failure\"}",
                        "application/json; charset=utf-8");
                case MALFORMED_JSON -> send(
                        exchange, 200, "{\"version\":1,\"products\":[",
                        "application/json; charset=utf-8");
                case INVALID_PRODUCT -> send(
                        exchange, 200, INVALID_PRODUCT_JSON,
                        "application/json; charset=utf-8");
                case DELAYED -> {
                    delay(Duration.ofMillis(350));
                    send(exchange, 200, VALID_JSON, "application/json; charset=utf-8");
                }
            }
        }
    }

    private static void delay(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void send(HttpExchange exchange, int status, String text, String contentType)
            throws IOException {
        var body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    /** Освобождает порт и дожидается обработчиков. */
    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.close();
            executor = null;
        }
    }
}
