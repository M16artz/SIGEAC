create table academic_reconciliation_policy (
    id uuid primary key,process_id uuid not null references self_assessment_process(id),indicator_id uuid not null,
    revision bigint not null check(revision>=1),predecessor_id uuid unique,
    configured_by uuid not null references access_identity(id),authority_assignment_id uuid not null references functional_assignment(id),
    configured_at timestamptz not null,payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    unique(process_id,indicator_id,revision),unique(id,process_id,indicator_id),
    foreign key(predecessor_id,process_id,indicator_id) references academic_reconciliation_policy(id,process_id,indicator_id),
    check((revision=1 and predecessor_id is null) or(revision>1 and predecessor_id is not null))
);
create table academic_disagreement_record (
    id uuid primary key,process_id uuid not null references self_assessment_process(id),indicator_id uuid not null,
    revision bigint not null check(revision>=1),predecessor_id uuid unique,opened_by uuid not null references access_identity(id),
    opened_at timestamptz not null,payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    unique(process_id,indicator_id,revision),unique(id,process_id,indicator_id),
    foreign key(predecessor_id,process_id,indicator_id) references academic_disagreement_record(id,process_id,indicator_id),
    check((revision=1 and predecessor_id is null) or(revision>1 and predecessor_id is not null))
);
create table academic_reconciliation_resolution (
    id uuid primary key,process_id uuid not null,indicator_id uuid not null,disagreement_id uuid not null unique,policy_id uuid not null,
    resolved_by uuid not null references access_identity(id),authority_assignment_id uuid not null references functional_assignment(id),
    resolved_at timestamptz not null,payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    foreign key(disagreement_id,process_id,indicator_id) references academic_disagreement_record(id,process_id,indicator_id),
    foreign key(policy_id,process_id,indicator_id) references academic_reconciliation_policy(id,process_id,indicator_id)
);
create index ix_academic_current_run on quantitative_calculation(process_id,indicator_id,executed_by,executed_at desc,id desc);
create trigger academic_policy_immutable before update or delete on academic_reconciliation_policy for each row execute function reject_manual_evaluation_mutation();
create trigger academic_disagreement_immutable before update or delete on academic_disagreement_record for each row execute function reject_manual_evaluation_mutation();
create trigger academic_resolution_immutable before update or delete on academic_reconciliation_resolution for each row execute function reject_manual_evaluation_mutation();
