package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Загружает единственную начальную схему практики 11 из classpath. */
final class InitialSchema {
    static final String RESOURCE = "ru/npi/kbju/infrastructure/db/migration/V001__nutrition_plans.sql";

    private InitialSchema() {
    }

    /** Создаёт таблицу идемпотентным SQL из ресурса проекта. */
    static void apply(Connection connection) throws SQLException, IOException {
        Objects.requireNonNull(connection, "Соединение обязательно");
        try (var stream = openResource()) {
            if (stream == null) {
                throw new IOException("Начальная схема не найдена: " + RESOURCE);
            }
            var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private static InputStream openResource() {
        var fromModule = InitialSchema.class.getResourceAsStream("/" + RESOURCE);
        return fromModule != null
                ? fromModule
                : InitialSchema.class.getClassLoader().getResourceAsStream(RESOURCE);
    }
}
