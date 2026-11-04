CREATE TABLE evaluation_model (
    id uuid PRIMARY KEY, name varchar(200) NOT NULL, description varchar(2000) NOT NULL DEFAULT '',
    created_by uuid NOT NULL, created_at timestamptz NOT NULL
);

CREATE TABLE model_version (
    id uuid PRIMARY KEY, model_id uuid NOT NULL REFERENCES evaluation_model(id), version_number integer NOT NULL CHECK (version_number > 0),
    status varchar(20) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','SUPERSEDED')), source varchar(2000) NOT NULL DEFAULT '',
    author_id uuid NOT NULL, approver_id uuid, predecessor_id uuid REFERENCES model_version(id), revision bigint NOT NULL DEFAULT 0 CHECK (revision >= 0),
    created_at timestamptz NOT NULL, published_at timestamptz, snapshot_json jsonb NOT NULL,
    UNIQUE(model_id, version_number), CHECK (approver_id IS NULL OR approver_id <> author_id)
);

CREATE TABLE model_criterion (id uuid PRIMARY KEY, version_id uuid NOT NULL REFERENCES model_version(id), code varchar(100) NOT NULL, name varchar(300) NOT NULL, description varchar(4000) NOT NULL DEFAULT '', position integer NOT NULL CHECK(position>0), UNIQUE(id,version_id), UNIQUE(version_id,code), UNIQUE(version_id,position));
CREATE TABLE model_standard (id uuid PRIMARY KEY, version_id uuid NOT NULL REFERENCES model_version(id), criterion_id uuid NOT NULL, code varchar(100) NOT NULL, name varchar(300) NOT NULL, description varchar(4000) NOT NULL DEFAULT '', position integer NOT NULL CHECK(position>0), UNIQUE(id,version_id), UNIQUE(version_id,code), UNIQUE(criterion_id,position), FOREIGN KEY(criterion_id,version_id) REFERENCES model_criterion(id,version_id));
CREATE TABLE model_indicator (id uuid PRIMARY KEY, version_id uuid NOT NULL REFERENCES model_version(id), standard_id uuid NOT NULL, code varchar(100) NOT NULL, name varchar(300) NOT NULL, description varchar(4000) NOT NULL DEFAULT '', position integer NOT NULL CHECK(position>0), requirements_json jsonb NOT NULL DEFAULT '{}'::jsonb, UNIQUE(version_id,code), UNIQUE(standard_id,position), FOREIGN KEY(standard_id,version_id) REFERENCES model_standard(id,version_id));
CREATE TABLE model_rating_scale (id uuid PRIMARY KEY, version_id uuid NOT NULL UNIQUE REFERENCES model_version(id), code varchar(100) NOT NULL, name varchar(300) NOT NULL);
CREATE TABLE model_scale_option (id uuid PRIMARY KEY, scale_id uuid NOT NULL REFERENCES model_rating_scale(id), code varchar(100) NOT NULL, label varchar(200) NOT NULL, position integer NOT NULL CHECK(position>0), declared_value numeric, UNIQUE(scale_id,code), UNIQUE(scale_id,position));
CREATE TABLE model_progress_formula (id uuid PRIMARY KEY, version_id uuid NOT NULL UNIQUE REFERENCES model_version(id), code varchar(100) NOT NULL, schema_version varchar(100) NOT NULL, definition_json jsonb NOT NULL);

CREATE INDEX ix_model_version_model_status ON model_version(model_id,status,version_number);
CREATE INDEX ix_model_indicator_version ON model_indicator(version_id);

CREATE FUNCTION reject_published_model_content_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target_version uuid;
BEGIN
    target_version := COALESCE(OLD.version_id, NEW.version_id);
    IF EXISTS (SELECT 1 FROM model_version WHERE id=target_version AND status <> 'DRAFT') THEN
        RAISE EXCEPTION 'published model content is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER criterion_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_criterion FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER standard_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_standard FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER indicator_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_indicator FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER scale_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_rating_scale FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER formula_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_progress_formula FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();

CREATE FUNCTION reject_published_scale_option_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target_scale uuid;
BEGIN
    target_scale := COALESCE(OLD.scale_id, NEW.scale_id);
    IF EXISTS (SELECT 1 FROM model_rating_scale s JOIN model_version v ON v.id=s.version_id WHERE s.id=target_scale AND v.status <> 'DRAFT') THEN
        RAISE EXCEPTION 'published model content is immutable';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;
CREATE TRIGGER scale_option_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_scale_option FOR EACH ROW EXECUTE FUNCTION reject_published_scale_option_mutation();
