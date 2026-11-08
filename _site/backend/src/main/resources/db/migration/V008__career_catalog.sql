CREATE TABLE academic_unit (
    id uuid PRIMARY KEY, code varchar(100) NOT NULL UNIQUE CHECK (code=upper(btrim(code)) AND char_length(code)>0),
    name varchar(300) NOT NULL CHECK (char_length(btrim(name))>0), status varchar(20) NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),
    revision bigint NOT NULL CHECK(revision>=1), created_by uuid NOT NULL, created_at timestamptz NOT NULL
);
CREATE TABLE career (
    id uuid PRIMARY KEY, academic_unit_id uuid NOT NULL REFERENCES academic_unit(id) ON DELETE RESTRICT,
    code varchar(100) NOT NULL UNIQUE CHECK(code=upper(btrim(code)) AND char_length(code)>0),
    name varchar(300) NOT NULL CHECK(char_length(btrim(name))>0), status varchar(20) NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),
    valid_from date NOT NULL, valid_until date, classification_pending boolean NOT NULL DEFAULT false,
    revision bigint NOT NULL CHECK(revision>=1), created_by uuid NOT NULL, created_at timestamptz NOT NULL,
    CHECK(valid_until IS NULL OR valid_from<=valid_until)
);
CREATE TABLE academic_unit_history (
    unit_id uuid NOT NULL REFERENCES academic_unit(id) ON DELETE RESTRICT,revision bigint NOT NULL CHECK(revision>=1),snapshot_json jsonb NOT NULL,
    changed_by uuid NOT NULL,changed_at timestamptz NOT NULL,reason varchar(4000) NOT NULL,PRIMARY KEY(unit_id,revision)
);
CREATE TABLE career_history (
    career_id uuid NOT NULL REFERENCES career(id) ON DELETE RESTRICT,revision bigint NOT NULL CHECK(revision>=1),snapshot_json jsonb NOT NULL,
    changed_by uuid NOT NULL,changed_at timestamptz NOT NULL,reason varchar(4000) NOT NULL,PRIMARY KEY(career_id,revision)
);
CREATE INDEX ix_career_unit_status ON career(academic_unit_id,status,code,id);
CREATE INDEX ix_career_status_code ON career(status,code,id);

-- Preserve opaque legacy IDs without inventing academic units, names or institutional authority.
INSERT INTO academic_unit VALUES('00000000-0000-0000-0000-000000000008','LEGACY-UNCLASSIFIED','Unidad pendiente de clasificación','DISABLED',1,'00000000-0000-0000-0000-000000000000',now());
INSERT INTO career(id,academic_unit_id,code,name,status,valid_from,classification_pending,revision,created_by,created_at)
SELECT id,'00000000-0000-0000-0000-000000000008','LEGACY-'||upper(id::text),'Carrera pendiente de clasificación','DISABLED','1900-01-01',true,1,'00000000-0000-0000-0000-000000000000',now()
FROM (SELECT career_id AS id FROM self_assessment_process UNION SELECT career_id FROM functional_assignment UNION SELECT career_id FROM conflict_exception) refs;
INSERT INTO academic_unit_history SELECT id,revision,jsonb_build_object('id',id,'data',jsonb_build_object('code',code,'name',name),'status',status,'revision',revision,'createdBy',created_by,'createdAt',created_at),created_by,created_at,'LEGACY_REFERENCE_RECONCILIATION' FROM academic_unit;
INSERT INTO career_history SELECT id,revision,jsonb_build_object('id',id,'data',jsonb_build_object('code',code,'name',name,'academicUnitId',academic_unit_id,'validFrom',valid_from,'validUntil',valid_until),'status',status,'revision',revision,'createdBy',created_by,'createdAt',created_at,'classificationPending',classification_pending),created_by,created_at,'LEGACY_REFERENCE_RECONCILIATION' FROM career;
ALTER TABLE self_assessment_process ADD CONSTRAINT fk_process_career FOREIGN KEY(career_id) REFERENCES career(id) ON DELETE RESTRICT;
ALTER TABLE functional_assignment ADD CONSTRAINT fk_functional_assignment_career FOREIGN KEY(career_id) REFERENCES career(id) ON DELETE RESTRICT;
ALTER TABLE conflict_exception ADD CONSTRAINT fk_conflict_exception_career FOREIGN KEY(career_id) REFERENCES career(id) ON DELETE RESTRICT;
CREATE FUNCTION reject_catalog_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'catalog history is append-only' USING ERRCODE='23514'; END; $$;
CREATE TRIGGER career_history_guard BEFORE UPDATE OR DELETE ON career_history FOR EACH ROW EXECUTE FUNCTION reject_catalog_history_mutation();
CREATE TRIGGER unit_history_guard BEFORE UPDATE OR DELETE ON academic_unit_history FOR EACH ROW EXECUTE FUNCTION reject_catalog_history_mutation();
CREATE TRIGGER career_no_delete BEFORE DELETE ON career FOR EACH ROW EXECUTE FUNCTION reject_catalog_history_mutation();
CREATE TRIGGER unit_no_delete BEFORE DELETE ON academic_unit FOR EACH ROW EXECUTE FUNCTION reject_catalog_history_mutation();

CREATE FUNCTION require_active_process_career() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c career%ROWTYPE; unit_state varchar(20);
BEGIN
    SELECT * INTO c FROM career WHERE id=NEW.career_id FOR SHARE;
    IF NOT FOUND OR c.status<>'ACTIVE' OR c.classification_pending OR CURRENT_DATE<c.valid_from OR (c.valid_until IS NOT NULL AND CURRENT_DATE>c.valid_until) THEN
        RAISE EXCEPTION 'career unavailable for new process' USING ERRCODE='23514';
    END IF;
    SELECT status INTO unit_state FROM academic_unit WHERE id=c.academic_unit_id FOR SHARE;
    IF unit_state<>'ACTIVE' OR NEW.period_start<c.valid_from OR (c.valid_until IS NOT NULL AND NEW.period_end>c.valid_until) THEN
        RAISE EXCEPTION 'career period or unit unavailable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER process_career_active BEFORE INSERT ON self_assessment_process FOR EACH ROW EXECUTE FUNCTION require_active_process_career();

CREATE FUNCTION guard_catalog_unit_state() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE unit_state varchar(20);
BEGIN
    IF TG_TABLE_NAME='career' THEN
        SELECT status INTO unit_state FROM academic_unit WHERE id=NEW.academic_unit_id FOR SHARE;
        IF NEW.status='ACTIVE' AND unit_state IS DISTINCT FROM 'ACTIVE' THEN
            RAISE EXCEPTION 'active career requires active unit' USING ERRCODE='23514';
        END IF;
    ELSIF NEW.status='DISABLED' AND EXISTS(SELECT 1 FROM career WHERE academic_unit_id=NEW.id AND status='ACTIVE') THEN
        RAISE EXCEPTION 'unit still has active careers' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER active_career_unit BEFORE INSERT OR UPDATE ON career FOR EACH ROW EXECUTE FUNCTION guard_catalog_unit_state();
CREATE TRIGGER active_unit_dependents BEFORE UPDATE ON academic_unit FOR EACH ROW EXECUTE FUNCTION guard_catalog_unit_state();

INSERT INTO function_operation(function_id,operation_code) SELECT id,'CAREER_ADMIN' FROM process_function WHERE code='FUNCTIONAL_ADMIN' ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code) SELECT id,'CAREER_READ' FROM process_function WHERE code IN ('FUNCTIONAL_ADMIN','COORDINATOR','RESPONSIBLE','REVIEWER','EVALUATOR','PROCESS_AUTHORITY') ON CONFLICT DO NOTHING;
