ALTER TABLE nutrition_plan ADD COLUMN import_code TEXT;

CREATE UNIQUE INDEX uq_nutrition_plan_import_code
    ON nutrition_plan(import_code)
    WHERE import_code IS NOT NULL;
