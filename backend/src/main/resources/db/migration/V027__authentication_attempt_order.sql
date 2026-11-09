-- Preserve a deterministic attempt order even when timestamps are identical.
CREATE SEQUENCE authentication_attempt_order_seq;
ALTER TABLE authentication_attempt ADD COLUMN attempt_order bigint;
WITH ordered AS (
    SELECT id, row_number() OVER (ORDER BY occurred_at, success, id) AS position
    FROM authentication_attempt
)
UPDATE authentication_attempt a SET attempt_order=o.position FROM ordered o WHERE a.id=o.id;
SELECT setval('authentication_attempt_order_seq', coalesce(max(attempt_order),0)+1, false)
FROM authentication_attempt;
ALTER TABLE authentication_attempt ALTER COLUMN attempt_order SET DEFAULT nextval('authentication_attempt_order_seq');
ALTER TABLE authentication_attempt ALTER COLUMN attempt_order SET NOT NULL;
ALTER SEQUENCE authentication_attempt_order_seq OWNED BY authentication_attempt.attempt_order;
CREATE INDEX ix_auth_attempt_subject_order ON authentication_attempt(subject_key, attempt_order DESC);
