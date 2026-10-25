-- V011: evidence immutability reserved since the given migration sequence.
-- Submitted evidence versions become immutable; files and successors of 004
-- reuse this guard, so there is no out-of-order migration afterwards.
CREATE OR REPLACE FUNCTION reject_immutable_evidence_mutation() RETURNS trigger AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        IF (OLD.status = 'SUBMITTED') THEN
            RAISE EXCEPTION 'submitted evidence version is immutable';
        END IF;
        RETURN OLD;
    END IF;
    IF (OLD.status = 'SUBMITTED' AND NEW.status <> 'SUBMITTED') THEN
        RAISE EXCEPTION 'submitted evidence version is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER evidence_version_immutable_guard
    BEFORE UPDATE OR DELETE ON evidence_version
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_evidence_mutation();

-- Also prevent mutation of version children when the version is submitted
CREATE OR REPLACE FUNCTION reject_mutation_on_submitted_version_child() RETURNS trigger AS $$
DECLARE
    v_status varchar(20);
BEGIN
    SELECT status INTO v_status FROM evidence_version WHERE id = COALESCE(NEW.version_id, OLD.version_id);
    IF (v_status = 'SUBMITTED') THEN
        RAISE EXCEPTION 'submitted evidence version is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER evidence_version_indicator_immutable_guard
    BEFORE INSERT OR UPDATE OR DELETE ON evidence_version_indicator
    FOR EACH ROW EXECUTE FUNCTION reject_mutation_on_submitted_version_child();

CREATE TRIGGER evidence_version_file_immutable_guard
    BEFORE INSERT OR UPDATE OR DELETE ON evidence_version_file
    FOR EACH ROW EXECUTE FUNCTION reject_mutation_on_submitted_version_child();

CREATE TRIGGER evidence_external_reference_immutable_guard
    BEFORE INSERT OR UPDATE OR DELETE ON evidence_external_reference
    FOR EACH ROW EXECUTE FUNCTION reject_mutation_on_submitted_version_child();

-- Completing the evidence tree: a process evidence with submitted versions
-- cannot be deleted during archival release until recovery contracts run.
CREATE OR REPLACE FUNCTION reject_evidence_with_submitted_versions() RETURNS trigger AS $$
DECLARE
    submitted integer;
BEGIN
    SELECT count(*) INTO submitted FROM evidence_version v WHERE v.evidence_id = OLD.id AND v.status = 'SUBMITTED';
    IF (submitted > 0) THEN
        RAISE EXCEPTION 'evidence contains submitted versions';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER evidence_submitted_content_guard
    BEFORE DELETE ON evidence
    FOR EACH ROW EXECUTE FUNCTION reject_evidence_with_submitted_versions();
