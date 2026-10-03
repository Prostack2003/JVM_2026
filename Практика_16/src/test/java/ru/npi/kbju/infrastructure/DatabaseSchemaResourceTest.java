package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Фиксирует назначение двух опубликованных SQL-миграций. */
class DatabaseSchemaResourceTest {
    @Test
    void v001KeepsInitialTableAndV002AddsOnlyImportCapability() throws Exception {
        var v001 = SchemaMigrator.readResource(SchemaMigrator.V001_RESOURCE);
        var v002 = SchemaMigrator.readResource(SchemaMigrator.V002_RESOURCE);

        assertTrue(v001.contains("PRIMARY KEY AUTOINCREMENT"));
        assertTrue(v001.contains("name_key TEXT NOT NULL UNIQUE"));
        assertTrue(v001.contains("CHECK (length(trim(name)) BETWEEN 1 AND 60)"));
        assertTrue(v001.contains("'NEEDS_REVIEW'"));
        assertTrue(v002.contains("ADD COLUMN import_code TEXT"));
        assertTrue(v002.contains("UNIQUE INDEX uq_nutrition_plan_import_code"));
        assertTrue(v002.contains("WHERE import_code IS NOT NULL"));
    }
}
