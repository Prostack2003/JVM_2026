package ru.npi.kbju.lesson;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ru.npi.kbju.application.catalog.DirectoryLoadException;
import ru.npi.kbju.application.catalog.ProductCatalogRefreshService;
import ru.npi.kbju.domain.FoodProduct;
import ru.npi.kbju.integration.http.HttpProductDirectory;
import ru.npi.kbju.integration.http.LocalProductDirectoryServer;

/** Консольный маршрут приёмки HTTP/JSON-контракта практики 15. */
public final class HttpJsonDemo {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private HttpJsonDemo() { }

    /** Проверяет успех, два обязательных отказа, предметную границу и повтор порта. */
    public static void main(String[] args) {
        System.out.println("Практика 15: HTTP и JSON на локальном сервере");
        var server = new LocalProductDirectoryServer(
                LocalProductDirectoryServer.Scenario.VALID);
        int occupiedPort;
        var worker = Executors.newVirtualThreadPerTaskExecutor();
        try (server; worker) {
            var endpoint = server.start();
            occupiedPort = server.port();
            var catalog = new ProductCatalogRefreshService(
                    HttpProductDirectory.standard(endpoint));

            var accepted = await(catalog.refreshAsync(worker));
            require(accepted.size() == 3, "Должны быть приняты три продукта");
            var confirmed = catalog.confirmedProducts();
            System.out.println("HTTP 200: справочник принят, записей=" + accepted.size());

            server.use(LocalProductDirectoryServer.Scenario.SERVER_ERROR);
            expectFailure(catalog, worker, DirectoryLoadException.Kind.HTTP_STATUS, "HTTP 500");
            require(catalog.confirmedProducts().equals(confirmed),
                    "HTTP 500 не должен менять подтверждённый каталог");

            server.use(LocalProductDirectoryServer.Scenario.MALFORMED_JSON);
            expectFailure(catalog, worker, DirectoryLoadException.Kind.MALFORMED_JSON,
                    "повреждённый JSON");
            require(catalog.confirmedProducts().equals(confirmed),
                    "Повреждённый JSON не должен менять каталог");

            server.use(LocalProductDirectoryServer.Scenario.INVALID_PRODUCT);
            expectFailure(catalog, worker, DirectoryLoadException.Kind.CONTRACT,
                    "отрицательная калорийность");
            require(catalog.confirmedProducts().equals(confirmed),
                    "Недопустимый продукт не должен менять каталог");
        }
        require(worker.isTerminated(), "Executor фоновых HTTP-запросов должен завершиться");

        try (var restarted = new LocalProductDirectoryServer(
                LocalProductDirectoryServer.Scenario.VALID, occupiedPort)) {
            var endpoint = restarted.start();
            List<FoodProduct> products = HttpProductDirectory.standard(endpoint).fetch();
            require(products.size() == 3, "Повторный сервер должен вернуть тот же контракт");
            System.out.println("Повтор: порт " + occupiedPort + " освобождён и занят новым сервером");
        }
        System.out.println("Итог: все HTTP/JSON-сценарии практики 15 подтверждены.");
    }

    private static List<FoodProduct> await(
            java.util.concurrent.CompletableFuture<List<FoodProduct>> future
    ) {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание HTTP-сценария прервано", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Ожидался успешный HTTP-сценарий", exception.getCause());
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new IllegalStateException("Истёк таймаут HTTP-сценария", exception);
        }
    }

    private static void expectFailure(
            ProductCatalogRefreshService catalog,
            java.util.concurrent.Executor executor,
            DirectoryLoadException.Kind expected,
            String label
    ) {
        try {
            catalog.refreshAsync(executor).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            throw new AssertionError("Ожидался отказ: " + label);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание отказа прервано", exception);
        } catch (ExecutionException exception) {
            require(exception.getCause() instanceof DirectoryLoadException,
                    "Отказ должен сохранить прикладной тип");
            var failure = (DirectoryLoadException) exception.getCause();
            require(failure.kind() == expected,
                    "Ожидался " + expected + ", получено " + failure.kind());
            System.out.println(label + ": " + failure.kind() + " — " + failure.getMessage());
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Истёк таймаут ожидания отказа", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
