-- V018__findings_reports.sql is historical scaffolding; the P13–P14 records are introduced separately.
create table governance_finding (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    registered_by uuid not null references access_identity(id),
    registered_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);
create index ix_governance_finding on governance_finding(process_id, registered_at desc, id desc);

create table governance_finding_consolidation (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    revision bigint not null check(revision>=1),
    predecessor_id uuid unique,
    consolidated_by uuid not null references access_identity(id),
    consolidated_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    unique(process_id,revision),
    unique(id,process_id),
    foreign key(predecessor_id,process_id) references governance_finding_consolidation(id,process_id),
    check((revision=1 and predecessor_id is null) or (revision>1 and predecessor_id is not null))
);
create table governance_consolidation_finding (
    consolidation_id uuid not null references governance_finding_consolidation(id) on delete cascade,
    finding_id uuid not null references governance_finding(id) on delete restrict,
    primary key(consolidation_id, finding_id)
);

create table governance_report_version (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    version_number bigint not null check(version_number>=1),
    predecessor_version_id uuid unique,
    emitted_by uuid not null references access_identity(id),
    emitted_at timestamptz not null,
    content_sha256 varchar(64) not null check(content_sha256 ~ '[a-f0-9]{64}'),
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    unique(process_id,version_number),
    unique(id,process_id),
    foreign key(predecessor_version_id,process_id) references governance_report_version(id,process_id)
);
create index ix_governance_report_versions on governance_report_version(process_id, version_number asc);

create table governance_report_processing (
    version_id uuid primary key references governance_report_version(id),
    process_id uuid not null,
    state varchar(30) not null check(state in ('PRELIMINARY','UNDER_OBSERVATION','FINAL','APPROVED','RETURNED')),
    revision bigint not null check(revision>=0),
    updated_at timestamptz not null
);

create table governance_report_observation (
    id uuid primary key,
    version_id uuid not null references governance_report_version(id),
    observed_by uuid not null references access_identity(id),
    observed_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);

create table governance_report_reply (
    id uuid primary key,
    observation_id uuid not null unique references governance_report_observation(id),
    replied_by uuid not null references access_identity(id),
    replied_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);

create table governance_report_socialization (
    id uuid primary key,
    version_id uuid not null references governance_report_version(id),
    socialized_by uuid not null references access_identity(id),
    recorded_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);

create table governance_report_decision (
    id uuid primary key,
    version_id uuid not null references governance_report_version(id),
    decided_by uuid not null references access_identity(id),
    authority_assignment_id uuid not null references functional_assignment(id),
    decided_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    unique(version_id)
);

create function reject_governance_report_mutation() returns trigger language plpgsql as $$
begin
    raise exception 'GOVERNANCE_REPORT_IMMUTABLE';
end;
$$;

create trigger governance_finding_immutable before update or delete on governance_finding
    for each row execute function reject_governance_report_mutation();
create trigger governance_consolidation_immutable before update or delete on governance_finding_consolidation
    for each row execute function reject_governance_report_mutation();
create trigger governance_consolidation_finding_immutable before update or delete on governance_consolidation_finding
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_version_immutable before update or delete on governance_report_version
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_observation_immutable before update or delete on governance_report_observation
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_reply_immutable before update or delete on governance_report_reply
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_socialization_immutable before update or delete on governance_report_socialization
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_decision_immutable before update or delete on governance_report_decision
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_processing_no_delete before delete on governance_report_processing
    for each row execute function reject_governance_report_mutation();
create trigger governance_report_processing_no_displacement before update on governance_report_processing
    for each row when (old.version_id is distinct from new.version_id or old.process_id is distinct from new.process_id)
    execute function reject_governance_report_mutation();

INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'FINDING_READ' FROM process_function
    WHERE code IN ('FUNCTIONAL_ADMIN','COORDINATOR','RESPONSIBLE','REVIEWER','EVALUATOR','PROCESS_AUTHORITY') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'FINDING_WRITE' FROM process_function WHERE code IN ('COORDINATOR','REVIEWER') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'ACADEMIC_CONSOLIDATE' FROM process_function WHERE code IN ('COORDINATOR') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'REPORT_READ' FROM process_function
    WHERE code IN ('FUNCTIONAL_ADMIN','COORDINATOR','RESPONSIBLE','REVIEWER','PROCESS_AUTHORITY') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'REPORT_WRITE' FROM process_function WHERE code IN ('COORDINATOR','REVIEWER') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'REPORT_SOCIALIZE' FROM process_function WHERE code IN ('COORDINATOR') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'REPORT_OBSERVE' FROM process_function WHERE code IN ('REVIEWER') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'REPORT_APPROVE' FROM process_function WHERE code IN ('PROCESS_AUTHORITY') ON CONFLICT DO NOTHING;
INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'REPORT_REVIEW' FROM process_function WHERE code IN ('COORDINATOR','REVIEWER') ON CONFLICT DO NOTHING;
