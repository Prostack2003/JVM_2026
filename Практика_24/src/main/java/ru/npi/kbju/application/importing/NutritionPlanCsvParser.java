package ru.npi.kbju.application.importing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.npi.kbju.domain.NutritionPlanImportRow;

/** Разбирает оговорённый учебный CSV до начала пакетной записи. */
public final class NutritionPlanCsvParser {
    private static final String HEADER = "code;name;effective_from";

    private NutritionPlanCsvParser() {
    }

    /** Читает UTF-8 CSV с разделителем `;` и тремя обязательными полями. */
    public static List<NutritionPlanImportRow> parse(Path path)
            throws IOException, CsvImportException {
        Objects.requireNonNull(path, "Путь CSV обязателен");
        var lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new CsvImportException(1, "отсутствует заголовок " + HEADER);
        }
        var header = lines.get(0).strip().replaceFirst("^\\uFEFF", "");
        if (!HEADER.equals(header)) {
            throw new CsvImportException(1, "ожидается заголовок " + HEADER);
        }
        if (lines.size() == 1) {
            throw new CsvImportException(2, "пакет не содержит строк данных");
        }

        var result = new ArrayList<NutritionPlanImportRow>();
        for (int index = 1; index < lines.size(); index++) {
            var lineNumber = index + 1;
            var line = lines.get(index);
            if (line.isBlank()) {
                throw new CsvImportException(lineNumber, "пустая строка внутри пакета");
            }
            if (line.indexOf('"') >= 0) {
                throw new CsvImportException(lineNumber,
                        "кавычки и многострочные поля не поддерживаются учебным форматом");
            }
            var fields = line.split(";", -1);
            if (fields.length != 3) {
                throw new CsvImportException(lineNumber,
                        "ожидаются ровно три поля: code;name;effective_from");
            }
            try {
                result.add(new NutritionPlanImportRow(
                        fields[0], fields[1], LocalDate.parse(fields[2].strip())));
            } catch (RuntimeException exception) {
                throw new CsvImportException(
                        lineNumber, "некорректные данные: " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(result);
    }
}
