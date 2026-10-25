drop table if exists archive_record;
drop table if exists integral_closure;
drop table if exists closure_policy;

create table closure_policy (
    code varchar(100) primary key,
    strict boolean not null,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

insert into closure_policy(code,strict,active) values('INTEGRAL_STRICT_V1',true,true);

create table integral_closure (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    documentary_closure_id uuid not null references documentary_closure(id),
    policy_version varchar(100) not null references closure_policy(code),
    payload_json text not null,
    sha256 varchar(64) not null check (sha256 ~ '^[a-f0-9]{64}$'),
    pending_ids text not null default '[]',
    exception_decision_ids text not null default '[]',
    closed_by uuid not null references access_identity(id),
    closed_at timestamptz not null,
    revision integer not null default 1 check (revision >= 1)
);
create index ix_integral_closure_history on integral_closure(process_id, closed_at desc, id desc);
create unique index ux_integral_closure_documentary on integral_closure(process_id, documentary_closure_id);

create table archive_record (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    closure_snapshot_id uuid not null references integral_closure(id),
    retention_policy_version varchar(100) not null,
    archived_by uuid not null references access_identity(id),
    archived_at timestamptz not null,
    integrity_hash varchar(64) not null check (integrity_hash ~ '^[a-f0-9]{64}$'),
    integrity_status varchar(20) not null check (integrity_status in ('VERIFIED','FAILED')),
    unique(closure_snapshot_id)
);
create index ix_archive_record_history on archive_record(process_id, archived_at desc, id desc);

insert into function_operation(function_id,operation_code)
    select id,'PROCESS_READ' from process_function where code in ('FUNCTIONAL_ADMIN','COORDINATOR','PROCESS_AUTHORITY') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'ARCHIVE_READ' from process_function where code in ('FUNCTIONAL_ADMIN','COORDINATOR','PROCESS_AUTHORITY') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'ARCHIVE_WRITE' from process_function where code in ('COORDINATOR','PROCESS_AUTHORITY') on conflict do nothing;
insert into function_operation(function_id,operation_code)
    select id,'PROCESS_INTEGRAL_CLOSE' from process_function where code in ('COORDINATOR','PROCESS_AUTHORITY') on conflict do nothing;
