create table if not exists finding (
    id uuid primary key,
    code varchar(50) not null,
    description text not null,
    status varchar(50) not null,
    created_at timestamptz not null default now()
);
create table if not exists report_version (
    id uuid primary key,
    process_id uuid not null,
    version integer not null,
    state varchar(50) not null,
    content_hash varchar(128),
    created_at timestamptz not null default now(),
    unique(process_id, version)
);
create table if not exists report_observation (
    id uuid primary key,
    report_version_id uuid not null,
    actor_id uuid not null,
    text text not null,
    created_at timestamptz not null default now()
);
create table if not exists report_decision (
    id uuid primary key,
    report_version_id uuid not null,
    actor_id uuid not null,
    authority_assignment_id uuid,
    decision varchar(50) not null,
    reason text,
    decided_at timestamptz not null default now()
);
