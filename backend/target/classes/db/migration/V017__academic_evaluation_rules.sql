create table if not exists academic_variable_value (
    id uuid primary key,
    indicator_id uuid not null,
    variable_key varchar(100) not null,
    variable_value varchar(500) not null,
    created_at timestamptz not null default now()
);
create table if not exists academic_calculation_run (
    id uuid primary key,
    indicator_id uuid not null,
    strategy varchar(50) not null,
    validated boolean not null,
    created_at timestamptz not null default now()
);
create table if not exists academic_disagreement (
    id uuid primary key,
    indicator_id uuid not null,
    note text,
    resolved boolean not null default false,
    created_at timestamptz not null default now()
);
