create table if not exists improvement_plan (
    id uuid primary key,
    process_id uuid not null,
    status varchar(50) not null,
    created_at timestamptz not null default now()
);
create table if not exists improvement_action (
    id uuid primary key,
    plan_id uuid not null,
    finding_id uuid,
    status varchar(50) not null,
    deadline date,
    version integer not null default 1,
    created_at timestamptz not null default now()
);
create table if not exists improvement_progress (
    id uuid primary key,
    action_id uuid not null,
    actor_id uuid not null,
    note text,
    evidence_ref varchar(200),
    created_at timestamptz not null default now()
);
create table if not exists improvement_verification (
    id uuid primary key,
    action_id uuid not null,
    actor_id uuid not null,
    result varchar(50) not null,
    created_at timestamptz not null default now()
);
