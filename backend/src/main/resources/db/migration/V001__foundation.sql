SET TIME ZONE 'UTC';

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE sec_schema_metadata (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    application_timezone text NOT NULL DEFAULT 'UTC' CHECK (application_timezone = 'UTC'),
    id_generation text NOT NULL DEFAULT 'APPLICATION_UUID' CHECK (id_generation = 'APPLICATION_UUID')
);

INSERT INTO sec_schema_metadata(singleton) VALUES (true);

COMMENT ON TABLE sec_schema_metadata IS 'Convenciones globales; Flyway mantiene su historial en flyway_schema_history.';
