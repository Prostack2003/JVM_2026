package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Проверяет разрешение путей, значения по умолчанию и раннюю диагностику. */
class AppConfigTest {
    @TempDir
    Path directory;

    @Test
    void missingFileUsesDocumentedDefaultsUnderExplicitRoot() {
        var root = directory.resolve("data-root");

        var config = AppConfig.load(root, directory.resolve("missing.properties"));

        assertEquals(AppConfig.Source.DOCUMENTED_DEFAULTS, config.source());
        assertEquals(root.resolve("data/nutrition-plans.db").toAbsolutePath().normalize(),
                config.databasePath());
        assertEquals(root.resolve("logs").toAbsolutePath().normalize(),
                config.logDirectory());
        assertEquals(root.resolve("backup").toAbsolutePath().normalize(),
                config.backupDirectory());
    }

    @Test
    void relativeValuesResolveFromDataRootAndDirectoriesArePrepared() throws Exception {
        var root = directory.resolve("independent-root");
        var file = write("custom.properties", """
                database.path=storage/plans.db
                log.directory=diagnostics
                backup.directory=snapshots
                """);

        var config = AppConfig.load(root, file);
        config.prepareDirectories();

        assertEquals(AppConfig.Source.EXTERNAL_FILE, config.source());
        assertEquals(root.resolve("storage/plans.db").toAbsolutePath().normalize(),
                config.databasePath());
        assertEquals(root.resolve("diagnostics").toAbsolutePath().normalize(),
                config.logDirectory());
        assertEquals(root.resolve("snapshots").toAbsolutePath().normalize(),
                config.backupDirectory());
        assertTrue(Files.isDirectory(config.databasePath().getParent()));
        assertTrue(Files.isDirectory(config.logDirectory()));
        assertTrue(Files.isDirectory(config.backupDirectory()));
        assertFalse(Files.exists(config.databasePath()));
    }

    @Test
    void absoluteDatabasePathIsUsedAsSpecified() throws Exception {
        var database = directory.resolve("absolute/plans.db").toAbsolutePath();
        var file = write("absolute.properties", "database.path=" + database + "\n");

        var config = AppConfig.load(directory.resolve("another-root"), file);

        assertEquals(database.normalize(), config.databasePath());
    }

    @Test
    void missingOptionalKeysUseDocumentedDefaults() throws Exception {
        var root = directory.resolve("root");
        var file = write("partial.properties", "database.path=custom.db\n");

        var config = AppConfig.load(root, file);

        assertEquals(root.resolve("custom.db").toAbsolutePath().normalize(),
                config.databasePath());
        assertEquals(root.resolve("logs").toAbsolutePath().normalize(),
                config.logDirectory());
        assertEquals(root.resolve("backup").toAbsolutePath().normalize(),
                config.backupDirectory());
    }

    @Test
    void blankValueNamesExactConfigurationKey() throws Exception {
        var file = write("blank.properties", "database.path=   \n");

        var failure = assertThrowsExactly(
                AppConfigurationException.class,
                () -> AppConfig.load(directory.resolve("root"), file)
        );

        assertEquals(AppConfig.DATABASE_PATH_KEY, failure.key());
        assertFalse(failure.getMessage().contains(directory.toString()));
    }

    @Test
    void invalidPathIsRejectedBeforeApplicationWorkStarts() throws Exception {
        var invalidPath = "bad" + (char) 0 + "path";
        var file = write("invalid.properties", "database.path=" + invalidPath + "\n");

        var failure = assertThrowsExactly(
                AppConfigurationException.class,
                () -> AppConfig.load(directory.resolve("root"), file)
        );

        assertEquals(AppConfig.DATABASE_PATH_KEY, failure.key());
    }

    @Test
    void unreadableConfigurationHasSeparateFailureKey() throws Exception {
        var configDirectory = Files.createDirectory(directory.resolve("config-directory"));

        var failure = assertThrowsExactly(
                AppConfigurationException.class,
                () -> AppConfig.load(directory.resolve("root"), configDirectory)
        );

        assertEquals(AppConfig.CONFIG_FILE_PROPERTY, failure.key());
    }

    @Test
    void fileInPlaceOfLogDirectoryFailsDuringPreparation() throws Exception {
        var root = Files.createDirectory(directory.resolve("blocked-root"));
        Files.writeString(root.resolve("logs"), "not a directory", StandardCharsets.UTF_8);
        var config = AppConfig.load(root, directory.resolve("missing.properties"));

        var failure = assertThrowsExactly(
                AppConfigurationException.class,
                config::prepareDirectories
        );

        assertEquals(AppConfig.LOG_DIRECTORY_KEY, failure.key());
    }

    private Path write(String name, String content) throws Exception {
        var file = directory.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
