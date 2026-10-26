-- Dedicated synthetic PostgreSQL only; runs inside the fixture transaction.
INSERT INTO process_indicator(process_id,indicator_id,code,name)
SELECT 'f1060000-0000-0000-0011-000000000001', ('f1060000-0000-0000-0001-'||lpad(n::text,12,'0'))::uuid,
       'PERF-'||lpad(n::text,3,'0'), 'Indicador sintetico '||n FROM generate_series(1,500) n;

INSERT INTO process_function(id,code,name,active)
VALUES ('f1060000-0000-0000-0009-000000000001','PERFORMANCE_READER','Consulta sintetica T106',true);
INSERT INTO function_operation(function_id,operation_code)
SELECT 'f1060000-0000-0000-0009-000000000001', operation
FROM unnest(ARRAY['ACCESS_READ_SELF','CAREER_READ','PROCESS_READ','PROGRESS_READ','ARCHIVE_READ']) operation;
INSERT INTO access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at)
SELECT ('f1060000-0000-0000-0007-'||lpad(n::text,12,'0'))::uuid, 'perf'||lpad(n::text,3,'0'),
       'Usuario sintetico de carga '||n, 'HUMAN',true,0,now() FROM generate_series(1,100) n;
INSERT INTO functional_assignment(id,identity_id,function_id,career_id,process_id,valid_from,valid_to,granted_by,granted_at,revision)
SELECT ('f1060000-0000-0000-0009-'||lpad((n+2000)::text,12,'0'))::uuid,
       ('f1060000-0000-0000-0007-'||lpad(n::text,12,'0'))::uuid,
       'f1060000-0000-0000-0009-000000000001','11111111-1111-1111-1111-111111111111',
       'f1060000-0000-0000-0011-000000000001','2020-01-01','2030-01-01',
       'f1060000-0000-0000-0007-000000000001',now(),0 FROM generate_series(1,100) n;

INSERT INTO evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision)
SELECT ('f1060000-0000-0000-0003-'||lpad(n::text,12,'0'))::uuid,'f1060000-0000-0000-0011-000000000001',
       'f1060000-0000-0000-0007-000000000001',now(),
       ('f1060000-0000-0000-0003-'||lpad(n::text,12,'0'))::uuid,1 FROM generate_series(1,10000) n;
INSERT INTO evidence_version(id,evidence_id,revision_number,status,title,description,provenance,covered_from,covered_to,author_id,created_at)
SELECT ('f1060000-0000-0000-0004-'||lpad(n::text,12,'0'))::uuid,
       ('f1060000-0000-0000-0003-'||lpad(n::text,12,'0'))::uuid,1,'DRAFT','Evidencia sintetica '||n,
       'Version sintetica para carga reproducible','fixture://performance','2026-01-01','2026-12-31',
       'f1060000-0000-0000-0007-000000000001',now() FROM generate_series(1,10000) n;
INSERT INTO evidence_version_indicator(version_id,indicator_id,indicator_code,indicator_title,position)
SELECT ('f1060000-0000-0000-0004-'||lpad(n::text,12,'0'))::uuid,
       ('f1060000-0000-0000-0001-'||lpad(((n-1)%500+1)::text,12,'0'))::uuid,
       'PERF-'||lpad(((n-1)%500+1)::text,3,'0'),'Indicador sintetico '||((n-1)%500+1),1 FROM generate_series(1,10000) n;
INSERT INTO source_coverage(id,version_id,required_source_id,coverage_note,declared_by,declared_at)
SELECT ('f1060000-0000-0000-0006-'||lpad(n::text,12,'0'))::uuid,
       ('f1060000-0000-0000-0004-'||lpad(n::text,12,'0'))::uuid,
       ('f1060000-0000-0000-0002-'||lpad(((n-1)%500+1)::text,12,'0'))::uuid,
       'Cobertura explicita sintetica','f1060000-0000-0000-0007-000000000001',now() FROM generate_series(1,10000) n;
INSERT INTO evidence_external_reference(id,version_id,uri,description,position)
SELECT ('f1060000-0000-0000-0005-'||lpad(n::text,12,'0'))::uuid,
       ('f1060000-0000-0000-0004-'||lpad(n::text,12,'0'))::uuid,
       'https://performance.invalid/synthetic/'||n,'Referencia sintetica sin disponibilidad remota',1 FROM generate_series(1,10000) n;
UPDATE evidence_version SET status='SUBMITTED',submitted_at=now(),lock_revision=1
WHERE id::text LIKE 'f1060000-0000-0000-0004-%';
