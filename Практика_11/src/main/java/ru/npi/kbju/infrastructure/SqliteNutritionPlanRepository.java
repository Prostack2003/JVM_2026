package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanRepository;

/** JDBC-адаптер прежнего порта, сохраняющий планы в локальном файле SQLite. */
public final class SqliteNutritionPlanRepository implements NutritionPlanRepository {
    private static final String SELECT_COLUMNS =
            "id, name, effective_from, status";
    private final Path databasePath;
    private final Connection connection;

    /** Открывает файл, создаёт родительский каталог и применяет начальную схему. */
    public SqliteNutritionPlanRepository(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "Путь базы обязателен");
        databasePath = databaseFile.toAbsolutePath().normalize();
        connection = open(databasePath);
    }

    private static Connection open(Path databasePath) {
        Connection opened = null;
        try {
            var parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            opened = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            configureConnection(opened);
            InitialSchema.apply(opened);
            return opened;
        } catch (SQLException | IOException exception) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (SQLException closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
            }
            throw new IllegalStateException(
                    "Не удалось открыть локальное хранилище SQLite", exception);
        }
    }

    /** Возвращает нормализованный фактический путь для диагностики. */
    public Path databasePath() {
        return databasePath;
    }

    private static void configureConnection(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 3000");
        }
    }

    /** Читает все планы в порядке устойчивого идентификатора. */
    @Override
    public synchronized List<NutritionPlan> findAll() {
        var plans = new ArrayList<NutritionPlan>();
        var sql = "SELECT " + SELECT_COLUMNS + " FROM nutrition_plan ORDER BY id";
        try (var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                plans.add(map(rows));
            }
            return List.copyOf(plans);
        } catch (SQLException exception) {
            throw storageFailure("Не удалось прочитать планы питания", exception);
        }
    }

    /** Ищет план через параметр идентификатора, не собирая SQL из значения. */
    @Override
    public synchronized Optional<NutritionPlan> findById(long id) {
        var sql = "SELECT " + SELECT_COLUMNS + " FROM nutrition_plan WHERE id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw storageFailure("Не удалось найти план питания", exception);
        }
    }

    /** Вставляет новый либо обновляет существующий план подготовленным запросом. */
    @Override
    public synchronized NutritionPlan save(NutritionPlan plan)
            throws DuplicatePlanException {
        Objects.requireNonNull(plan, "Сохраняемый план обязателен");
        try {
            return plan.id() == 0 ? insert(plan) : update(plan);
        } catch (SQLException exception) {
            if (isDuplicateName(exception)) {
                throw new DuplicatePlanException(plan.name());
            }
            throw storageFailure("Не удалось сохранить план питания", exception);
        }
    }

    private NutritionPlan insert(NutritionPlan plan) throws SQLException {
        var sql = "INSERT INTO nutrition_plan"
                + "(name, name_key, effective_from, status) VALUES (?, ?, ?, ?)";
        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindPlan(statement, plan);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("SQLite не добавила ожидаемую строку");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("SQLite не вернула назначенный идентификатор");
                }
                return new NutritionPlan(keys.getLong(1), plan.name(),
                        plan.effectiveFrom(), plan.status());
            }
        }
    }

    private NutritionPlan update(NutritionPlan plan) throws SQLException {
        var sql = "UPDATE nutrition_plan SET name = ?, name_key = ?, "
                + "effective_from = ?, status = ? WHERE id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            bindPlan(statement, plan);
            statement.setLong(5, plan.id());
            if (statement.executeUpdate() != 1) {
                throw new NoSuchElementException("План с ID " + plan.id() + " не найден");
            }
            return plan;
        }
    }

    /** Удаляет по параметру ID и сообщает, существовала ли строка. */
    @Override
    public synchronized boolean deleteById(long id) {
        try (var statement = connection.prepareStatement(
                "DELETE FROM nutrition_plan WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw storageFailure("Не удалось удалить план питания", exception);
        }
    }

    private static void bindPlan(PreparedStatement statement, NutritionPlan plan)
            throws SQLException {
        statement.setString(1, plan.name());
        statement.setString(2, normalizeNameKey(plan.name()));
        statement.setString(3, plan.effectiveFrom().toString());
        statement.setString(4, plan.status().name());
    }

    private static NutritionPlan map(ResultSet rows) throws SQLException {
        try {
            return new NutritionPlan(
                    rows.getLong("id"),
                    rows.getString("name"),
                    LocalDate.parse(rows.getString("effective_from")),
                    NutritionPlan.Status.valueOf(rows.getString("status")));
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Строка SQLite несовместима с моделью плана питания", exception);
        }
    }

    private static String normalizeNameKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean isDuplicateName(SQLException exception) {
        return exception.getErrorCode() == 19
                && exception.getMessage() != null
                && exception.getMessage().contains("nutrition_plan.name_key");
    }

    private static IllegalStateException storageFailure(
            String message,
            SQLException exception
    ) {
        return new IllegalStateException(message, exception);
    }

    /** Закрывает принадлежащее адаптеру JDBC-соединение. */
    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw storageFailure("Не удалось закрыть локальное хранилище", exception);
        }
    }
}
