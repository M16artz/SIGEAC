-- V017 is historical scaffolding; manual judgements are introduced separately.
create table manual_academic_evaluation (
    id uuid primary key,
    process_id uuid not null references self_assessment_process(id),
    indicator_id uuid not null,
    evaluator_id uuid not null references access_identity(id),
    revision bigint not null check (revision >= 1),
    predecessor_id uuid unique,
    evaluated_at timestamptz not null,
    payload_json text not null check (jsonb_typeof(payload_json::jsonb) = 'object'),
    unique (process_id, indicator_id, evaluator_id, revision),
    unique (id, process_id, indicator_id, evaluator_id),
    foreign key (predecessor_id, process_id, indicator_id, evaluator_id)
        references manual_academic_evaluation(id, process_id, indicator_id, evaluator_id),
    check ((revision = 1 and predecessor_id is null) or (revision > 1 and predecessor_id is not null))
);
create index ix_manual_evaluation_history on manual_academic_evaluation(process_id, indicator_id, evaluated_at desc, id desc);
create function reject_manual_evaluation_mutation() returns trigger language plpgsql as $$
begin
    raise exception 'MANUAL_EVALUATION_IMMUTABLE';
end;
$$;
create trigger manual_evaluation_immutable before update or delete on manual_academic_evaluation
    for each row execute function reject_manual_evaluation_mutation();
