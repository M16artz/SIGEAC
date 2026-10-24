-- V011 is already historical. Harden submitted snapshots without rewriting it.
CREATE OR REPLACE FUNCTION reject_immutable_evidence_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status <> 'DRAFT' OR NEW.submitted_at IS NOT NULL THEN
            RAISE EXCEPTION 'evidence must be created as a draft';
        END IF;
        RETURN NEW;
    END IF;
    IF OLD.status = 'SUBMITTED' THEN
        RAISE EXCEPTION 'submitted evidence version is immutable';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF NEW.status = 'SUBMITTED' THEN
        IF NEW.submitted_at IS NULL OR NEW.lock_revision <> OLD.lock_revision + 1
           OR (to_jsonb(NEW) - ARRAY['status','submitted_at','lock_revision'])
              IS DISTINCT FROM (to_jsonb(OLD) - ARRAY['status','submitted_at','lock_revision']) THEN
            RAISE EXCEPTION 'submission may only freeze the current draft snapshot';
        END IF;
    ELSIF NEW.submitted_at IS NOT NULL THEN
        RAISE EXCEPTION 'draft cannot have a submission date';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER evidence_version_draft_insert_guard BEFORE INSERT ON evidence_version
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_evidence_mutation();

CREATE FUNCTION require_editable_evidence_parent() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    old_parent uuid;
    new_parent uuid;
    parent record;
BEGIN
    IF TG_OP <> 'INSERT' THEN old_parent := OLD.version_id; END IF;
    IF TG_OP <> 'DELETE' THEN new_parent := NEW.version_id; END IF;
    -- Child writers and submission take the same row lock. Lock both parents
    -- in a fixed order when moving a row, including its original parent.
    FOR parent IN SELECT id, status FROM evidence_version
            WHERE id = old_parent OR id = new_parent ORDER BY id FOR UPDATE LOOP
        IF parent.status <> 'DRAFT' THEN
            RAISE EXCEPTION 'submitted evidence children are immutable';
        END IF;
    END LOOP;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER version_indicator_submitted_guard BEFORE INSERT OR UPDATE OR DELETE ON evidence_version_indicator
    FOR EACH ROW EXECUTE FUNCTION require_editable_evidence_parent();
CREATE TRIGGER version_file_submitted_guard BEFORE INSERT OR UPDATE OR DELETE ON evidence_version_file
    FOR EACH ROW EXECUTE FUNCTION require_editable_evidence_parent();
CREATE TRIGGER external_reference_submitted_guard BEFORE INSERT OR UPDATE OR DELETE ON evidence_external_reference
    FOR EACH ROW EXECUTE FUNCTION require_editable_evidence_parent();
CREATE TRIGGER source_coverage_submitted_guard BEFORE INSERT OR UPDATE OR DELETE ON source_coverage
    FOR EACH ROW EXECUTE FUNCTION require_editable_evidence_parent();
CREATE TRIGGER structured_value_submitted_guard BEFORE INSERT OR UPDATE OR DELETE ON evidence_structured_value
    FOR EACH ROW EXECUTE FUNCTION require_editable_evidence_parent();

CREATE OR REPLACE FUNCTION reject_evidence_with_submitted_versions() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM evidence_version WHERE evidence_id=OLD.id AND status='SUBMITTED') THEN
        IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'evidence with submitted versions is immutable'; END IF;
        IF (to_jsonb(NEW) - 'latest_revision') IS DISTINCT FROM (to_jsonb(OLD) - 'latest_revision') THEN
            RAISE EXCEPTION 'submitted evidence identity is immutable';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER evidence_submitted_identity_guard BEFORE UPDATE ON evidence
    FOR EACH ROW EXECUTE FUNCTION reject_evidence_with_submitted_versions();
