create table if not exists closure_policy (
    id uuid primary key,
    code varchar(50) not null unique,
    strict boolean not null,
    active boolean not null default true
);
create table if not exists integral_closure (
    id uuid primary key,
    process_id uuid not null,
    policy_id uuid,
    state varchar(50) not null,
    blockers jsonb,
    closed_at timestamptz,
    created_at timestamptz not null default now()
);
create table if not exists archive_record (
    id uuid primary key,
    process_id uuid not null,
    snapshot_hash varchar(128) not null,
    archived_at timestamptz not null default now()
);
