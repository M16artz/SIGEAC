create table quantitative_input_set (
    id uuid primary key, process_id uuid not null references self_assessment_process(id), indicator_id uuid not null,
    captured_by uuid not null references access_identity(id), revision bigint not null check(revision>=1),
    predecessor_id uuid unique, captured_at timestamptz not null,
    payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    unique(process_id,indicator_id,captured_by,revision), unique(id,process_id,indicator_id,captured_by),
    unique(id,process_id,indicator_id),
    foreign key(predecessor_id,process_id,indicator_id,captured_by) references quantitative_input_set(id,process_id,indicator_id,captured_by),
    check((revision=1 and predecessor_id is null) or (revision>1 and predecessor_id is not null))
);
create table quantitative_verification (
    input_set_id uuid primary key references quantitative_input_set(id), verified_by uuid not null references access_identity(id),
    verified_at timestamptz not null, payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object')
);
create table quantitative_calculation (
    id uuid primary key, input_set_id uuid not null unique references quantitative_verification(input_set_id),
    process_id uuid not null, indicator_id uuid not null, executed_by uuid not null references access_identity(id),
    executed_at timestamptz not null, payload_json text not null check(jsonb_typeof(payload_json::jsonb)='object'),
    foreign key(input_set_id,process_id,indicator_id) references quantitative_input_set(id,process_id,indicator_id)
);
create index ix_quantitative_history on quantitative_calculation(process_id,indicator_id,executed_at desc,id desc);
create trigger quantitative_inputs_immutable before update or delete on quantitative_input_set for each row execute function reject_manual_evaluation_mutation();
create trigger quantitative_verification_immutable before update or delete on quantitative_verification for each row execute function reject_manual_evaluation_mutation();
create trigger quantitative_calculation_immutable before update or delete on quantitative_calculation for each row execute function reject_manual_evaluation_mutation();
