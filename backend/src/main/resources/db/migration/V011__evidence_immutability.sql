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
