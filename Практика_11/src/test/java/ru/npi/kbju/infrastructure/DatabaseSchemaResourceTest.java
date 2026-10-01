package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Фиксирует обязательные ограничения начальной схемы в ресурсе проекта. */
class DatabaseSchemaResourceTest {
    @Test
    void schemaContainsPrimaryKeyUniqueNameAndDomainChecks() throws IOException {
        try (var stream = getClass().getClassLoader()
                .getResourceAsStream(InitialSchema.RESOURCE)) {
            assertNotNull(stream, "Ресурс начальной схемы должен находиться в classpath");
            var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(sql.contains("PRIMARY KEY AUTOINCREMENT"));
            assertTrue(sql.contains("name_key TEXT NOT NULL UNIQUE"));
            assertTrue(sql.contains("CHECK (length(trim(name)) BETWEEN 1 AND 60)"));
            assertTrue(sql.contains("'NEEDS_REVIEW'"));
        }
    }
}
