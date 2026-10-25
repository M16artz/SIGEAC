-- P15 improvement workflow. The V019 scaffolding was never wired to application code; it is
-- replaced here by the revisoned model (append-only action revisions, progress and decisions).
drop table if exists improvement_verification;
drop table if exists improvement_progress;
drop table if exists improvement_action;
drop table if exists improvement_plan;

create table improvement_plan (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    report_version_id uuid not null references governance_report_version(id),
    created_by uuid not null references access_identity(id),
    created_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);
create index ix_improvement_plan on improvement_plan(process_id, created_at asc, id asc);

create table improvement_action (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    plan_id uuid not null references improvement_plan(id),
    state varchar(30) not null check(state in ('PROPOSED','APPROVED','IN_PROGRESS','PENDING_VERIFICATION','VERIFIED')),
    revision bigint not null check(revision>=1),
    revised_by uuid not null references access_identity(id),
    revised_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);
create index ix_improvement_action on improvement_action(process_id, revised_at desc, id desc);
create index ix_improvement_action_plan on improvement_action(plan_id);

create table improvement_action_revision (
    action_id uuid not null references improvement_action(id),
    revision bigint not null check(revision>=1),
    predecessor_revision bigint,
    state varchar(30) not null check(state in ('PROPOSED','APPROVED','IN_PROGRESS','PENDING_VERIFICATION','VERIFIED')),
    changed_by uuid not null references access_identity(id),
    changed_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    primary key(action_id, revision),
    foreign key(action_id, predecessor_revision) references improvement_action_revision(action_id, revision),
    check((revision=1 and predecessor_revision is null) or (revision>1 and predecessor_revision is not null))
);

create table improvement_progress (
    id uuid primary key,
    action_id uuid not null references improvement_action(id),
    recorded_by uuid not null references access_identity(id),
    recorded_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);
create index ix_improvement_progress on improvement_progress(action_id, recorded_at asc, id asc);

create table improvement_decision (
    id uuid primary key,
    action_id uuid not null references improvement_action(id),
    action_code varchar(30) not null check(action_code in
        ('APPROVE','RETURN','START','SUBMIT_VERIFICATION','VERIFY','REQUEST_CORRECTION')),
    decided_by uuid not null references access_identity(id),
    decided_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);
create index ix_improvement_decision on improvement_decision(action_id, decided_at asc, id asc);

create or replace function reject_improvement_mutation() returns trigger language plpgsql as $$
begin
    raise exception 'IMPROVEMENT_RECORD_IMMUTABLE';
end;
$$;

create trigger improvement_plan_immutable before update or delete on improvement_plan
    for each row execute function reject_improvement_mutation();
create trigger improvement_action_revision_immutable before update or delete on improvement_action_revision
    for each row execute function reject_improvement_mutation();
create trigger improvement_progress_immutable before update or delete on improvement_progress
    for each row execute function reject_improvement_mutation();
create trigger improvement_decision_immutable before update or delete on improvement_decision
    for each row execute function reject_improvement_mutation();
create trigger improvement_action_no_delete before delete on improvement_action
    for each row execute function reject_improvement_mutation();
create trigger improvement_action_no_displacement before update on improvement_action
    for each row when (old.id is distinct from new.id or old.process_id is distinct from new.process_id
        or old.plan_id is distinct from new.plan_id or new.revision <= old.revision)
    execute function reject_improvement_mutation();

insert into function_operation(function_id,operation_code)
    select id,'IMPROVEMENT_READ' from process_function
    where code in ('FUNCTIONAL_ADMIN','COORDINATOR','RESPONSIBLE','REVIEWER','EVALUATOR') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'IMPROVEMENT_WRITE' from process_function where code in ('COORDINATOR') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'IMPROVEMENT_DECIDE' from process_function where code in ('COORDINATOR') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'IMPROVEMENT_PROGRESS' from process_function where code in ('RESPONSIBLE') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'IMPROVEMENT_ACTION_EXECUTE' from process_function where code in ('RESPONSIBLE') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'IMPROVEMENT_ACTION_VERIFY' from process_function where code in ('REVIEWER') on conflict do nothing;
