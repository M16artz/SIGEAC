# I3 implementation prerequisites

Verified on 2026-10-20 against the repository source, migrations and public
OpenAPI contract.

## Required upstream capabilities

| Capability | Contract evidence | Executable backend evidence | Status |
|---|---|---|---|
| Authenticated identity | `GET /me` in `docs/technical/contracts/openapi.yaml` | `identityaccess` contains only `package-info.java`; no session/current-actor implementation or migration | BLOCKING |
| Function and career/process/indicator scope | assignment operations and schemas in the public contract | no authorization service, assignment model, repository or migration | BLOCKING |
| Active self-assessment process | process operations in the public contract | `selfassessment` contains only `package-info.java`; no process model, repository or migration | BLOCKING |
| Fixed published model version and indicators | process/indicator operations in the public contract | no evaluation-model persistence or process-to-model snapshot available to I3 | BLOCKING |
| Current responsible assignment | assignment operations in the public contract | no current-assignment query/port implementation | BLOCKING |

The database currently has only `V001__foundation.sql` and
`V002__audit.sql`. There is no upstream schema to which an I3 `V006`
migration could safely attach foreign keys.

## Gate result

T001 is complete as a verification task, but the integration gate is **not
satisfied**. Do not create substitute identity, process, model, indicator or
assignment tables in I3. Until I0 and I2 provide their migrations and public
application ports, work is limited to isolated domain/storage components and
tests using explicit doubles. In particular, V006, JPA mappings, controllers
and integrated API/UI journeys must not proceed.
