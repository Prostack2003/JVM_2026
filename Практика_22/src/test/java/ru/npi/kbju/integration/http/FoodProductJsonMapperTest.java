package ru.npi.kbju.integration.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import ru.npi.kbju.application.catalog.DirectoryLoadException;

class FoodProductJsonMapperTest {
    private final FoodProductJsonMapper mapper = new FoodProductJsonMapper();

    @Test
    void mapsCompleteContractAfterCheckingEveryField() {
        var result = mapper.parse("""
                {"version":1,"products":[
                  {"code":"FOOD-901","name":"Кефир","energyKcal":53,
                   "proteinGrams":3.0,"fatGrams":2.5,"carbohydrateGrams":4.0}
                ],"futureField":"ignored by compatibility policy"}
                """);

        assertEquals(1, result.size());
        assertEquals("FOOD-901", result.getFirst().code());
        assertEquals(53, result.getFirst().energyKcal());
    }

    @Test
    void distinguishesMalformedJsonFromContractViolation() {
        var malformed = assertThrows(
                DirectoryLoadException.class,
                () -> mapper.parse("{\"version\":1,\"products\":["));
        var wrongType = assertThrows(
                DirectoryLoadException.class,
                () -> mapper.parse("{\"version\":1,\"products\":{}}"));

        assertEquals(DirectoryLoadException.Kind.MALFORMED_JSON, malformed.kind());
        assertEquals(DirectoryLoadException.Kind.CONTRACT, wrongType.kind());
    }

    @Test
    void rejectsMissingFieldWrongTypeAndDomainViolation() {
        assertContractFailure("""
                {"version":1,"products":[
                  {"code":"FOOD-902","energyKcal":53,"proteinGrams":3,
                   "fatGrams":2,"carbohydrateGrams":4}
                ]}
                """);
        assertContractFailure("""
                {"version":1,"products":[
                  {"code":902,"name":"Кефир","energyKcal":53,"proteinGrams":3,
                   "fatGrams":2,"carbohydrateGrams":4}
                ]}
                """);
        assertContractFailure("""
                {"version":1,"products":[
                  {"code":"FOOD-902","name":"Кефир","energyKcal":-1,"proteinGrams":3,
                   "fatGrams":2,"carbohydrateGrams":4}
                ]}
                """);
    }

    @Test
    void rejectsDuplicateCodesBeforePublishingList() {
        assertContractFailure("""
                {"version":1,"products":[
                  {"code":"FOOD-903","name":"A","energyKcal":1,"proteinGrams":1,"fatGrams":1,"carbohydrateGrams":1},
                  {"code":"FOOD-903","name":"B","energyKcal":2,"proteinGrams":2,"fatGrams":2,"carbohydrateGrams":2}
                ]}
                """);
    }

    private void assertContractFailure(String json) {
        var failure = assertThrows(DirectoryLoadException.class, () -> mapper.parse(json));
        assertEquals(DirectoryLoadException.Kind.CONTRACT, failure.kind());
    }
}
