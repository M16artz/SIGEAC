-- Model snapshots are authoritative, including stable indicator IDs across successors.
-- Validate normative links against that exact version, without requiring an unused node projection.
DO $$
DECLARE constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'indicator_normative_reference'::regclass
          AND confrelid = 'model_node'::regclass AND contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE indicator_normative_reference DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END;
$$;

CREATE FUNCTION require_snapshot_indicator_reference() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM model_version v,
            jsonb_array_elements(COALESCE(v.snapshot_json->'configuration'->'indicators', '[]'::jsonb)) indicator
        WHERE v.id = NEW.model_version_id AND indicator->>'nodeId' = NEW.indicator_node_id::text
    ) THEN
        RAISE EXCEPTION 'normative indicator must belong to the configured model version snapshot';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER indicator_normative_snapshot_guard BEFORE INSERT ON indicator_normative_reference
    FOR EACH ROW EXECUTE FUNCTION require_snapshot_indicator_reference();
