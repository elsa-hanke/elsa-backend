#
#   PLEASE REMOVE THIS AFTER FEATURE IS COMPLETE
#


# Expired opintooikeus data cleanup plan

## Goal and agreed scope

Add a scheduled cleanup that finds study rights whose `viimeinenKatselupaiva` is
before the current date, clears user-entered content associated with those rights,
and records that cleanup has completed.

Agreed behavior:

- Keep the `opintooikeus` row and all linked rows; scrub content in place rather
  than deleting records.
- Remove stored document binaries from ordinary documents while retaining their
  rows and references.
- Leave Envers audit history and immutable archive-snapshot documents untouched.
- Do not alter user-global profile data or data belonging to another, still
  accessible opintooikeus.
- Preserve structural/administrative data (for example IDs, dates, statuses,
  enums, and numeric progress) unless the field inventory identifies it as
  free-text content.

The existing access-date convention in `OpintooikeusServiceImpl` uses
`viimeinenKatselupaiva`; the cleanup candidate rule should therefore be
`viimeinenKatselupaiva < today`, evaluated using the injected `Clock`. A right
whose access date has been extended before processing must not be cleaned.

## 1. Inventory the data to scrub

Before implementation, make a field-by-field inventory of all records directly
or indirectly associated with an opintooikeus. Start with the entity
relationships and repositories; do not assume that `Opintooikeus`'s mapped
collections cover the full graph.

At minimum, inspect:

- Direct opintooikeus relationships: `Koulutussuunnitelma`, `Tyoskentelyjakso`,
  `Teoriakoulutus`, `Opintosuoritus`, and the related approval/authorization
  records.
- The records with an opintooikeus foreign key, including diary entries and
  follow-up periods, documents, graduation requests and reviews, and the
  different koejakso forms.
- Indirect children and references of those records, such as work-place details,
  education periods, assessments, performance entries, and documents attached
  to a work period or training plan.

For each text-like field, decide and document whether it is user-entered content,
generated text, or structural metadata. Clear sensitive/user-entered text.
Where a field is non-nullable, use a neutral empty value or placeholder that
satisfies its database and validation constraints. For ordinary document data,
replace the required binary payload with an empty byte array (or another
schema-valid empty representation); scrub identifying free-text metadata such
as a user-provided document name where applicable. Skip
`Asiakirja.archiveSnapshot == true` documents entirely, including their metadata
and binary data, because those snapshots _are_ immutable under the agreed scope.

Treat shared references carefully: do not clear a row or document if it is also
used by a non-expired opintooikeus. Do not update `User`, `Kayttaja`, or other
user-global fields as part of this job.

### Traced relationship graph and field classification

The following paths are present in the domain model. The directly linked
records are located by their `opintooikeus` relationship; indirect children
must be fetched through their owning record or an explicit repository query.

```text
Opintooikeus
├── Koulutussuunnitelma
│   ├── KoulutussuunnitelmaAsiakirja / MotivaatiokirjeAsiakirja
│   └── Koulutusjakso
│       ├── Tyoskentelyjakso [many-to-many]
├── Tyoskentelyjakso
│   ├── Tyoskentelypaikka [one-to-one, owned]
│   ├── Keskeytysaika
│   ├── Suoritemerkinta
│   ├── Suoritusarviointi
│   │   ├── SuoritusarvioinninKommentti
│   │   ├── SuoritusarvioinninArvioitavaKokonaisuus
│   │   ├── SuoritusarvioinninArviointityokalunVastaus
│   │   └── Asiakirja
│   └── Asiakirja
├── Teoriakoulutus ── Asiakirja
│                    └── Paivakirjamerkinta [optional theory-training link]
├── Paivakirjamerkinta
├── Seurantajakso [direct; links to Koulutusjakso]
├── Opintosuoritus ── OpintosuoritusOsakokonaisuus
├── Kouluttajavaltuutus [no candidate text fields]
├── KoejaksonAloituskeskustelu
├── KoejaksonKoulutussopimus
│   ├── KoulutussopimuksenKouluttaja
│   └── KoulutussopimuksenKoulutuspaikka
├── KoejaksonValiarviointi
├── KoejaksonKehittamistoimenpiteet
├── KoejaksonLoppukeskustelu
├── KoejaksonVastuuhenkilonArvio ── Asiakirja
├── Valmistumispyynto
│   ├── ValmistumispyynnonTarkistus
│   └── yhteenveto-, liite- ja erikoistujan-tiedot-asiakirjat
└── TerveyskeskuskoulutusjaksonHyvaksynta
```

In the table, **clear candidate** means the field is clearly free-form
user-entered content based on its domain role and write path. **Preserve**
identifies structural, enumerated, imported, or user-global data. Required text
fields are called out because the database column cannot be set to `null`.

| Entity / path | Fields and meaning | Classification / handling |
|---|---|---|
| `Koulutussuunnitelma` (one-to-one from `Opintooikeus`) | `motivaatiokirje`, `opiskeluJaTyohistoria`, `vahvuudet`, `tulevaisuudenVisiointi`, `osaamisenKartuttaminen`, `elamankentta`: authored narrative; privacy flags exist for each field | **Clear candidate** for all six text values. Preserve privacy flags, `muokkauspaiva`, plan/document links. |
| `Koulutusjakso` (many-to-one from the plan) | `nimi`: required, user-provided title; `muutOsaamistavoitteet`: user-provided additional goals | **Clear candidate** for both. `nimi` is non-null and must become `""`, not `null`. Preserve dates, lock state, selected competency links. |
| `Seurantajakso` (direct many-to-one; links to training periods) | `omaArviointi`, `lisahuomioita`, `seuraavanJaksonTavoitteet`, `huolenaiheet`, `kouluttajanArvio`, `erikoisalanTyoskentelyvalmiudet`, `jatkotoimetJaRaportointi`, `seurantakeskustelunYhteisetMerkinnat`, `korjausehdotus`: narrative and feedback | **Clear candidate** for all listed strings. `omaArviointi` is non-null and must become `""`. Preserve dates, approval state, educator link, training-period links. |
| `Paivakirjamerkinta` (direct many-to-one; optional link to `Teoriakoulutus`) | `oppimistapahtumanNimi`, `muunAiheenNimi`, `reflektio`: event title/custom topic/reflection; topic categories are separate lookup relations | **Clear candidate** for all three strings. `oppimistapahtumanNimi` is required by both schema and entity; `muunAiheenNimi` is nullable in Liquibase but required by the entity column mapping. Use `""` for both. Preserve date, privacy flag, category and theory-training links. |
| `Tyoskentelypaikka` (one-to-one child of a work period) | `nimi`: work-place name supplied with the work record; `muuTyyppi`: free text when enum type is `MUU`; `tyyppi` and `kunta` are enum/reference data | **Clear candidate** for `nimi` and `muuTyyppi`; `nimi` is non-null, so use `""`. Preserve `tyyppi` and municipality reference. The entity is modeled as owned by one work period. |
| `Teoriakoulutus` (direct many-to-one) | `koulutuksenNimi` and `koulutuksenPaikka`: user-entered training name and place, passed through the save DTO and PDF validation | **Clear candidate** for both. Both columns are non-null; use `""`. Preserve dates, accepted hours, and certificate links. |
| `Suoritemerkinta` (many-to-one from work period) | `lisatiedot`: optional narrative note; rating/level/date and selected `Suorite` are structured progress data | **Clear candidate** for `lisatiedot`; preserve the rating, dates, and reference links. |
| `Suoritusarviointi` (many-to-one from work period) | `arvioitavaTapahtuma`, `lisatiedot`, `sanallinenItsearviointi`, `sanallinenArviointi`, `muuPeruste`: description, comments, narrative assessment and custom basis | **Clear candidate** for the listed text; preserve numeric levels, dates, lock/completion flags, evaluator, assessment-scale and competency links. |
| `SuoritusarvioinninKommentti` (child of an assessment) | `teksti`: comment body; column is non-null | **Clear candidate**; use `""` to satisfy the constraint. Preserve author and timestamps. |
| `SuoritusarvioinninArviointityokalunVastaus` (child of an assessment) | `tekstiVastaus`: typed response; selected option is a separate lookup relation | **Clear candidate** for typed response; preserve selected-option link and tool/question references. |
| `Asiakirja` / `AsiakirjaData` (direct and indirect links) | `nimi`: required upload name; `tyyppi`: file/type metadata; `data`: required byte payload stored in DB | **Clear candidate** for `nimi` and binary `data` on ordinary documents (`nimi=""`, `data=byteArrayOf()`). Preserve type, date and references. Skip `archiveSnapshot=true` documents and their data entirely. |
| `KoejaksonAloituskeskustelu` | `koejaksonSuorituspaikka`, `koejaksonToinenSuorituspaikka`, `koejaksonOsaamistavoitteet`, `korjausehdotus`: places, authored goals and corrections | **Clear candidate**; preserve dates, hours, approvals and people links. |
| `KoejaksonValiarviointi` | `muuKategoria`, `vahvuudet`, `kehittamistoimenpiteet`, `korjausehdotus`: custom category, strengths, development narrative and corrections | **Clear candidate**; preserve selected category enums, decisions, dates and people links. |
| `KoejaksonKehittamistoimenpiteet` | `korjausehdotus`: correction narrative | **Clear candidate**; preserve sufficiency/approval flags, dates and people links. |
| `KoejaksonLoppukeskustelu` | `jatkotoimenpiteet`, `korjausehdotus`: further actions and corrections | **Clear candidate**; preserve decisions, dates and people links. |
| `KoejaksonVastuuhenkilonArvio` | `perusteluHylkaamiselle`, `lisatiedotVirkailijalta`, `virkailijanYhteenveto`, `virkailijanKorjausehdotus`, `vastuuhenkilonKorjausehdotus`: rejection reason, official narrative and corrections | **Clear candidate**; preserve decisions, signatures/acknowledgement dates, people and document links. |
| `Valmistumispyynto` | `selvitysVanhentuneistaSuorituksista`, `vastuuhenkiloOsaamisenArvioijaKorjausehdotus`, `virkailijanKorjausehdotus`, `virkailijanSaate`, `vastuuhenkiloHyvaksyjaKorjausehdotus`: explanation, corrections and cover note | **Clear candidate**, including completed records per user confirmation; preserve workflow dates, reviewer and document links. |
| `ValmistumispyynnonTarkistus` | `virkailijanYhteenveto`, `kommentitVirkailijoille`: official summary and comments | **Clear candidate**; preserve structured checks, achievement dates and request link. |
| `TerveyskeskuskoulutusjaksonHyvaksynta` | `virkailijanKorjausehdotus`, `vastuuhenkilonKorjausehdotus`, `lisatiedotVirkailijalta`: corrections and official additional information | **Clear candidate**; preserve submission/approval flags, dates and people links. |
| `KoejaksonKoulutussopimus` | `korjausehdotus`, `vastuuhenkilonKorjausehdotus`: corrections | **Clear candidate**, including completed agreements; preserve signature/approval dates, flags, people and child links. |
| `KoulutussopimuksenKouluttaja` | `toimipaikka`, `lahiosoite`, `postitoimipaikka`: agreement-specific workplace/address | **Clear candidate**, per user confirmation; preserve educator link, acknowledgement date and agreement status. Global name, title, email and phone are derived by the mapper and are not cleared. |
| `KoulutussopimuksenKoulutuspaikka` | `nimi`: agreement-specific training-place name | **Clear candidate**; database requires `""` rather than `null`, although the entity does not express this constraint. Preserve university reference, agreement flag and parent link. |
| `Opintosuoritus` and `OpintosuoritusOsakokonaisuus` (direct child and its children) | Course names, codes, grades, dates and credits are imported study records, not user-authored narrative; code/grade data can be required | **Preserve** under the current free-text cleanup scope. They are structured/imported educational records; changing them would erase achievement history and may not be permanent if re-imported. |
| `Opintooikeus` itself | `yliopistoOpintooikeusId` is the university source identifier; `opiskelijatunnus` is a person-specific student identifier | **Preserve both** under the agreed content-only scope: `yliopistoOpintooikeusId` remains the source identity/integration key, and the user confirmed that `opiskelijatunnus` should remain intact. |
| Lookup/reference entities, audit data, and user-global profile | University, municipality, specialty, course and assessment labels; `User`/`Kayttaja` contact/profile; Envers revisions | **Preserve**. Do not mutate shared lookup rows, global profiles, or historical audit revisions. |

Ownership checks need special care. `Koulutusjakso` is owned by its
`Koulutussuunnitelma`, but has many-to-many links to work periods and is also
linked from follow-up periods. `KoulutusjaksoServiceImpl.save` calls `filter`
to check that submitted work periods and competency goals belong to the same
opintooikeus, but discards the filtered results before mapping and saving; the
call therefore does not enforce that check. `SeurantajaksoServiceImpl.create`
also maps the submitted training-period links directly, then queries
same-opintooikeus training periods only when setting lock state. Do not rely on
these service paths to prove there are no cross-opintooikeus references.

Diary entries optionally reference theory-training rows; check their owner
before following that link. In cleanup, query each target entity by its
authoritative owner (`opintooikeus` or its owned parent), and inspect
many-to-many/optional references for links from other, still-accessible rights
before clearing shared content. Do not mutate the linked work period, training
period, or theory-training row merely because it appears in the relationship
collection. Agreement children and work-place rows are modeled as owned by
their parent records.

Before enabling cleanup, inspect existing many-to-many and optional references
for cross-opintooikeus links. For any such link, either correct the existing
association or skip/report the shared row; never clear it based only on the
expired right's object graph. Add a regression test for the ineffective
`filter` calls if the save-path validation is corrected as part of this work.

The inventory is now traced from the domain model and relevant write paths.
The user confirmed that `opiskelijatunnus` remains intact; free-text
comments/corrections are scrubbed even on completed approvals and signed
agreements; and agreement-specific educator workplace/address fields are
scrubbed while the educator link and agreement status remain. The remaining
review is to validate the ownership checks against existing data and preserve
all structural/workflow fields.

### A1 result: exact empty-value contract

Checked the currently included Liquibase changesets in `master.xml` order,
including later additions, renamed columns and nullability changes, against
the current entity column mappings. This is a repository-schema review, not a
query of a deployed database. No schema or cleanup-code changes are part of A1.

The table below is the field allowlist for the cleanup methods. Set optional
text to `null` only when both the migration schema and entity mapping allow it;
use `""` where either layer requires a non-null value. Retain every unlisted
field and relationship, except the completion marker and technical timestamps
whose permitted persistence behavior is documented in A2 below.

| Entity | Set to `null` | Set to `""` or empty binary |
|---|---|---|
| `Koulutussuunnitelma` | `motivaatiokirje`, `opiskeluJaTyohistoria`, `vahvuudet`, `tulevaisuudenVisiointi`, `osaamisenKartuttaminen`, `elamankentta` | None |
| `Koulutusjakso` | `muutOsaamistavoitteet` | `nimi = ""` |
| `Seurantajakso` | `lisahuomioita`, `seuraavanJaksonTavoitteet`, `huolenaiheet`, `kouluttajanArvio`, `erikoisalanTyoskentelyvalmiudet`, `jatkotoimetJaRaportointi`, `seurantakeskustelunYhteisetMerkinnat`, `korjausehdotus` | `omaArviointi = ""` |
| `Paivakirjamerkinta` | `reflektio` | `oppimistapahtumanNimi = ""`, `muunAiheenNimi = ""` |
| `Tyoskentelypaikka` | `muuTyyppi` | `nimi = ""` |
| `Teoriakoulutus` | None | `koulutuksenNimi = ""`, `koulutuksenPaikka = ""` |
| `Suoritemerkinta` | `lisatiedot` | None |
| `Suoritusarviointi` | `arvioitavaTapahtuma`, `lisatiedot`, `sanallinenItsearviointi`, `sanallinenArviointi`, `muuPeruste` | None |
| `SuoritusarvioinninKommentti` | None | `teksti = ""` |
| `SuoritusarvioinninArviointityokalunVastaus` | `tekstiVastaus` | None |
| `KoejaksonAloituskeskustelu` | `koejaksonSuorituspaikka`, `koejaksonToinenSuorituspaikka`, `koejaksonOsaamistavoitteet`, `korjausehdotus` | None |
| `KoejaksonValiarviointi` | `muuKategoria`, `vahvuudet`, `kehittamistoimenpiteet`, `korjausehdotus` | None |
| `KoejaksonKehittamistoimenpiteet` | `korjausehdotus` | None |
| `KoejaksonLoppukeskustelu` | `jatkotoimenpiteet`, `korjausehdotus` | None |
| `KoejaksonVastuuhenkilonArvio` | `perusteluHylkaamiselle`, `lisatiedotVirkailijalta`, `virkailijanYhteenveto`, `virkailijanKorjausehdotus`, `vastuuhenkilonKorjausehdotus` | None |
| `KoejaksonKoulutussopimus` | `korjausehdotus`, `vastuuhenkilonKorjausehdotus` | None |
| `KoulutussopimuksenKouluttaja` | `toimipaikka`, `lahiosoite`, `postitoimipaikka` | None |
| `KoulutussopimuksenKoulutuspaikka` | None | `nimi = ""` |
| `Valmistumispyynto` | `selvitysVanhentuneistaSuorituksista`, `vastuuhenkiloOsaamisenArvioijaKorjausehdotus`, `virkailijanKorjausehdotus`, `virkailijanSaate`, `vastuuhenkiloHyvaksyjaKorjausehdotus` | None |
| `ValmistumispyynnonTarkistus` | `virkailijanYhteenveto`, `kommentitVirkailijoille` | None |
| `TerveyskeskuskoulutusjaksonHyvaksynta` | `virkailijanKorjausehdotus`, `vastuuhenkilonKorjausehdotus`, `lisatiedotVirkailijalta` | None |
| Ordinary `Asiakirja` | None | `nimi = ""` |
| Ordinary document's `AsiakirjaData` | None | `data = byteArrayOf()` |

Constraint evidence and exceptions:

- `20210929104412_added_entity_Koulutussuunnitelma.xml` permits `null` for all
  six plan narratives; privacy flags remain required.
- `20210527120142_modify_koejakso_null_constraints.xml` makes the start
  discussion's place/goals nullable, and
  `20211119173244_modify_entity_constraints_Suoritusarviointi.xml` makes
  `arvioitava_tapahtuma` nullable. The original table declarations alone would
  give the wrong result for these fields.
- `20210310162139_added_entity_KoejaksonKoulutussopimus.xml` requires the child
  training-place `nimi`; the current entity mapping lacks `nullable=false`.
  Keep the schema as-is and use `""` during cleanup.
- `20211111121659_added_entity_Paivakirjamerkinta.xml` allows `null` for
  `muun_aiheen_nimi`, but the entity specifies `nullable=false`. Use `""`
  without changing either schema or entity in this task.
- The initial training-period, follow-up, theory-training, work-place, comment,
  document and document-data migrations require their non-null fields shown
  above. No minimum-content-length constraint was found for the allowlisted
  fields; empty values still need real persistence coverage in their later tasks.
- `20261018090000_archive_document_snapshots.xml` protects both document
  metadata and binary rows with PostgreSQL triggers. Exclude archive snapshots
  and any binary row used by a snapshot before mutation; never disable triggers.

### A2 result: persistence and workflow side effects

A2 is complete as source-code analysis and agreed implementation requirements,
not as runtime verification. Cleanup implementation and the safeguards below
are still pending; completing A2 does not make production cleanup safe to enable.

**Agreed decisions:** allow existing persistence callbacks to update technical
modification dates. Preserve original event, submission, signature, return and
approval dates. Retained cleaned records must stay permanently read-only,
including after an extension of `viimeinenKatselupaiva`.

#### Technical dates and audit history

| Entities/metadata | Effect of managed JPA updates | Cleanup requirement |
|---|---|---|
| `Koulutussuunnitelma`; all six `Koejakson*` forms; `Valmistumispyynto`; `ValmistumispyynnonTarkistus`; `TerveyskeskuskoulutusjaksonHyvaksynta` | Existing `@PreUpdate` sets `muokkauspaiva` to the JVM system-zone date | Allow the callback; the field-level method itself must not assign dates |
| `Seurantajakso` | `AuditingEntityListener` and `@LastModifiedDate` update `tallennettu` | Allow Spring auditing; preserve `@CreatedDate luotu` |
| `Koulutusjakso`; `SuoritusarvioinninKommentti` | No entity listener/callback updates `tallennettu` or `muokkausaika` in these mappings | Preserve these values; do not imitate normal edit handling |
| Remaining allowlisted entities | No automatic modification-date callback found | Change only A1 fields; preserve every other date |
| Envers revision metadata | `AuditRevisionListener` assigns actor and `LocalDateTime.now()` | An unauthenticated scheduled transaction uses `SYSTEM_ACCOUNT` (`system`) |

Callback evidence is in the corresponding domain classes under
`src/main/kotlin/fi/elsapalvelu/elsa/domain/`. `DatabaseConfiguration` enables
Spring auditing with `SpringSecurityAuditorAware`, which also falls back to
`system` when no current login exists. There is no custom `DateTimeProvider`
configured there. Existing callbacks/revision timestamps use the system clock,
not the injected eligibility `Clock`: tests must not assume changing that
injected clock changes audit dates. Verify the configured runtime timezone and
date conversion, especially around midnight; scheduling in Helsinki alone
does not set the JVM or database timezone.

All A1 entities are `@Audited` except `AsiakirjaData` and
`SuoritusarvioinninArviointityokalunVastaus`. Updating audited entities should
create new revisions with cleared values, retaining earlier revisions intact.
`@NotAudited` on a parent's collection does not disable a child's own auditing.
Document metadata is audited, but the binary is not: older metadata revisions
must not be described as recoverable copies of the cleared binary. Typed-answer
cleanup likewise has no automatic Envers history. Do not add new audit scope or
rewrite old revisions in this feature.

Use managed-entity persistence, not native/bulk updates that bypass callbacks,
Envers and Hibernate cache handling. `CacheConfiguration` configures entity
caches and many target mappings use `READ_WRITE`. Verify fresh reads after
commit, outside the original persistence context, including a warmed cache;
do not rely only on assertions against the mutated in-memory entity.

#### Text-dependent workflow behavior

| Source | Verified consequence of A1 empty values |
|---|---|
| `SeurantajaksoTila.haeTila` | Approved records still return `HYVAKSYTTY`; other records lose their returned state and, with both educator assessment and shared notes null, become `ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA` |
| `SeurantajaksoRepository.findAvoinByKouluttajaUserId` | An unapproved returned record can newly match the open-list predicate when correction and educator assessment become null; ordering uses the updated `tallennettu` |
| `SeurantajaksoServiceImpl.update` | The trainee condition accepts null shared notes or a non-null correction, without checking `hyvaksytty` in that branch. Cleanup can therefore make even an approved record pass that condition |
| `KoejaksoTila` | Final approvals remain approved, but removing corrections changes returned/incomplete/pending states depending on role and prior approvals. Responsible-person assessment views include both non-null and nonblank predicates; entity and DTO overloads differ |
| Six koejakso repositories | Start-discussion, middle/development/final-discussion, agreement and responsible-person assessment queries use correction presence for visibility/list inclusion. Clearing it can remove records from role-specific lists |
| `KoejaksonVaiheetServiceImpl` | Combined lists use these queries and derived states; sorting separates `ODOTTAA_HYVAKSYNTAA` and uses modification dates. Cleanup can change list position as well as membership |
| `TerveyskeskuskoulutusjaksoTila` and its approval repository | Removing corrections changes returned states; responsible-person detail lookup can stop matching when visibility depended on the correction rather than official approval |
| `Valmistumispyynto` status helpers | Return/pending/approved states use retained acknowledgement/return dates and inspection presence, rather than the cleared correction text. Verify each role and `isAvoin` branch, but do not reset those dates |

Sources above live in `service/dto/enumeration/`, `service/impl/`,
`repository/koejakso/`, `repository/seuranta/` and `repository/valmistuminen/`
under `src/main/kotlin/fi/elsapalvelu/elsa/`; graduation status helpers are in
`domain/valmistuminen/Valmistumispyynto.kt`.

Do not route cleanup through normal workflow update methods. For example,
koejakso handlers interpret blank submitted correction text as approval and
can change acknowledgement dates, send emails or create authorizations;
follow-up updates also send emails. Cleanup methods must only mutate A1
fields, while the orchestrator persists them directly. No notifications,
signing, PDF generation, archive submission or workflow transitions should be
invoked as a consequence of cleanup.

#### Permanent read-only handling and production gates

Expiry alone is not a sufficient safeguard. `OpintooikeusRepository`'s valid
list queries check the access date, but its selected-right methods do not;
`findOneByIdAndErikoistuvaLaakariIdAndBetweenDate` checks only the grant date.
`OpintooikeusServiceImpl` also returns impersonated IDs directly.
`ElsaSwitchUserFilter` checks university/field/authorization relationships,
not the study right's last access date. These are specific reviewed paths,
not a claim that every endpoint currently permits expired-right writes.

Implement the following independently verifiable safeguards before enabling:

- Use the completion marker as a permanent scrubbed-content state, independent
  of date extensions. Imports/extensions must not reset it or restore content
  from historical revisions. Keep allowed structural/import updates separate
  from edits to retained cleaned records.
- Put the write guard at the shared application-module seam so both direct-ID
  and selected-right operations check the owning right. Cover creation, update,
  deletion, upload, approval/return and other workflow actions for trainee,
  educator, supervisor, responsible-person and official roles, including
  impersonation. Reject explicitly using the existing error-response convention
  before changing data or sending notifications; UI disabling is not sufficient.
- Exclude processed rights from active action queues before pagination/counting.
  Any authorized retained-record view must clearly identify cleared content and
  be read-only, rather than showing a misleading new request for approval.
  Preserve actual approval flags/dates; do not replace null content with fake
  text to keep old predicates true. Verify intended historical visibility
  separately from active-list membership.
- Coordinate content writers with cleanup on the same right-level transaction
  lock: reload/recheck the marker after acquiring it. A write that waits for
  cleanup must be rejected after cleanup commits. A marker check outside the
  lock is vulnerable to a race; job-instance ShedLock alone does not protect it.

Add persistence tests for callback dates, unchanged creation/event dates,
new system-attributed audit revisions, retained old revisions, unaudited
binary/answer cleanup and snapshot exclusions. Add workflow tests for
approved, returned and incomplete examples, each relevant role, list counts,
direct-ID writes, impersonation and date extension after cleanup. Assert
rejected actions produce no data changes, emails, signatures or archive work.
For already-empty input, do not force a write solely to refresh timestamps;
verify no unnecessary revisions when there is no actual field change.

Logging is also a persistence-adjacent risk: `LoggingAspect.logAround` logs
repository/module arguments and results at debug level, and entity `toString`
methods can include narratives. Do not log entities/DTOs in cleanup. Verify
that repository calls and failure handling do not expose original text through
automatic argument/result logging, including debug and dry-run execution.
Report IDs and counts only.

## 1a. Implement cleanup as testable per-entity functions

For every entity type whose content is scrubbed, implement a dedicated,
independently testable cleanup function (for example,
`clearSeurantajaksoContent`, `clearKoulutussuunnitelmaContent`, or
`clearAsiakirjaContent`). Keep each function responsible for that entity's
field-level cleanup only; use a separate orchestrator to find related records
and invoke the appropriate functions. Do not combine unrelated entity types
into one large cleanup function.

Implement one cleanup module with a dedicated method for each updated entity
type. These methods mutate the supplied entity in place: clear only the
inventoried content fields, preserve structural fields and relationships, and
respect archive-snapshot exclusions. Keep repository calls out of these methods
so they can be tested independently without a scheduler or database.

A separate transactional orchestrator loads the correct records, checks
ownership and shared references, invokes the per-entity methods, and coordinates
persistence. Entities loaded within its transaction remain managed by JPA;
dirty checking persists their changes at commit, so explicit repository `save`
calls are normally unnecessary. If explicit saves are used for consistency
with existing repository patterns, call them from the orchestrator after
cleanup, inside the same transaction.

Do not commit each entity type separately. All cleanup changes for one
opintooikeus and its completion marker must commit atomically; any failure must
roll back the entire transaction for that right. Implementing and verifying
methods one by one is a development workflow, not a sequence of independent
production commits.

Add focused tests for every per-entity function. For each one, assert that all
fields designated for cleanup are cleared or set to valid neutral values, while
representative structural fields remain unchanged. Include special-case tests
for required/non-null fields, shared references, and immutable snapshots where
applicable. These tests are the field-level evidence that cleanup is complete;
the service/job tests should separately verify that the right entities are
selected and the functions are invoked atomically for the correct opintooikeus.

## 2. Add an explicit completion marker

Add a non-null boolean column to `opintooikeus` through a Liquibase changeset,
defaulting existing rows to `false`. Update the `Opintooikeus` entity and any
DTOs/mappers only as needed for the read-only handling agreed in A2. Keep the
marker server-controlled; clients must never be able to reset it. Expose a
derived read-only/content-cleared indication if retained-record views need it.

Prefer a name that says what happened (for example,
`henkilotiedotTyhjennetty`) rather than a generic `archived`, which can be
confused with the retained opintooikeus row or immutable archive snapshots.
Finalize the name with the domain owners before creating the migration.

Add repository queries that return only unprocessed candidates with a
`viimeinenKatselupaiva` before a supplied date. Return IDs in stable order and
page through them, rather than loading every entity graph into memory. Handle
unexpected null dates explicitly (do not select them as expired); report their
count for investigation if the database permits them.

## 3. Implement the job and per-right cleanup

Follow the existing scheduled-job pattern (`AbstractTriggerableJob`,
`@Scheduled`, `@SchedulerLock`) and make the schedule use `Europe/Helsinki`.
Run daily after the study-right import has had time to apply date extensions
(for example, shortly after the current 04:00 import); make the cron setting
consistent with existing configuration conventions.

Keep the scheduler as an orchestrator and put cleanup for one opintooikeus
behind a transactional application/service method:

1. Fetch a page of candidate IDs.
2. For each ID, reload the opintooikeus in a transaction and lock it against
   concurrent updates.
3. Recheck both the completion marker and `viimeinenKatselupaiva < today` inside
   that transaction. Skip if it was already processed or its access date was
   extended.
4. Load and scrub the inventoried data, respecting shared references and
   skipping immutable snapshots.
5. Set the completion marker only after all associated changes succeed, then
   commit the scrub and marker atomically.

Process each right in its own transaction so one bad or unusually large record
does not roll back unrelated rights. If a right fails, leave its marker false so
the next run can retry it. Continue to other candidates, but report a partial or
failed run through the repository's normal job monitoring/logging rather than
logging a successful completion with no indication of failures. Logs and metrics
should identify the opintooikeus by ID, not include cleared text or document
contents.

Make the operation idempotent: a retry must be safe, and the marker must never
be set before the cleanup has completed successfully. Avoid bulk updates that
bypass entity callbacks/auditing unless their behavior is deliberately verified.

## 4. Test coverage

Add repository and service/job tests following the existing scheduler and
opintooikeus test conventions.

- Candidate query: before-today, today, future, already processed, null date,
  and deterministic paging.
- Per-entity cleanup functions: each updated entity type has focused tests
  proving every inventoried content field is cleared and structural data is
  preserved.
- Cleanup scope: every inventoried free-text field is cleared; required fields
  remain valid; structured data is preserved.
- Document behavior: ordinary binary content is emptied, rows/references remain,
  and archive snapshots (including their data) are unchanged.
- Isolation: user-global data, another active opintooikeus, and shared records
  are not scrubbed.
- Cross-owner references: seed a training-period/work-period or
  follow-up/training-period link across two opintooikeudet and verify cleanup
  never scrubs data reached only through the other right.
- Transaction/retry: an error halfway through a right rolls back both scrubbing
  and the marker; a later run can process it; a failure for one right does not
  prevent other candidates from being attempted.
- Concurrency: an opintooikeus extended between candidate selection and
  processing is rechecked and skipped; competing job instances are fenced by
  the scheduler lock.
- Envers: confirm the chosen behavior creates the expected new revisions while
  leaving prior revisions intact.

## 4a. Implement and roll out cleanup incrementally

Implement the cleanup in small, independently reviewable steps instead of
scrubbing every entity type in one change. Complete and verify one step at a
time; review its test results and cleanup scope before proceeding to the next
step. Each step should add one entity type or a tightly related group with:

1. Its approved fields and relationship/ownership checks in the inventory.
2. A dedicated cleanup function for each entity type being updated.
3. Focused unit tests proving those fields are cleared and structural data is
   preserved.
4. Integration coverage showing the entity is selected only for the target
   opintooikeus and the update is transactional.

Start with a low-risk, directly owned entity whose clearable fields and
relationships are unambiguous. For each step, inspect the affected test/data
results, confirm that only intended fields changed and structural data remains
intact, and resolve any failures before continuing. Proceed to the next entity
or related group only after the current step is verified. Treat indirect and
potentially shared records (especially assessment children, documents, and
many-to-many-linked training periods) as separate later steps that require
ownership/isolation tests before cleanup is enabled for them.

Keep the scheduled job disabled or in dry-run mode until all approved entity
steps are implemented and reviewed. Then enable the complete cleanup flow,
validate candidate counts, and monitor the rollout as described below. The
completion marker must be set only after every required step for that
opintooikeus has succeeded in the same transaction; a partially processed
opintooikeus must remain eligible for retry.

## 5. Rollout and operations

Before enabling the scheduled job in production, run the candidate query in a
read-only/dry-run mode or equivalent report and review the number and scope of
affected rights and linked records. Verify the field inventory against realistic
test data, including rights with multiple linked records and archive snapshots.

Expose useful run totals (candidates, processed, skipped after recheck, and
failed) without exposing personal content. Deploy the schema change before the
application version that reads the marker. Monitor the first production runs
and confirm that failures remain eligible for retry and that date extensions
are respected.

## Open implementation decisions

- Confirm the completion-marker name with domain owners.
- Confirm treatment of generated text and document metadata during the field
  inventory; the agreed default is to scrub user-entered/identifying text while
  preserving structural metadata.
- Confirm the daily execution time against the operational import schedule.
- Confirm the initial production candidate count and monitoring/approval process
  with the data owner before enabling destructive scrubbing.

## Implementation task checklist

Each checkbox is a separately reviewable task. Complete and verify one task
before moving on; keep production cleanup disabled until the full flow is ready.
A1-A4 are complete: field/constraint review, persistence-side-effect decisions,
the first cleanup method and its transactional persistence slice. B1-B2 also add
diary and follow-up cleanup; B3 onward remain pending. The incomplete flow must not be
enabled in production.

### A. Prepare the first cleanup slice

- [x] **A1. Finalize the field checklist.** Split the grouped koejakso and
  graduation inventory rows into exact per-entity field lists. Confirm empty
  values against Liquibase constraints, not only entity annotations.
- [x] **A2. Define persistence side effects.** Check entity callbacks and
  auditing: several `@PreUpdate` methods change `muokkauspaiva`, and follow-up
  auditing changes `tallennettu`. Document which technical modification dates
  may change while preserving original event and approval dates. Also check
  workflow predicates/derived statuses that depend on cleared text being null,
  and define safeguards against reopened editing or workflow actions. Source
  review and decisions are documented above; implementation remains in C9-C11.
- [x] **A3. Create the cleanup module and first method.** Add only
  `clearKoulutussuunnitelmaContent`, with tests for all six narrative fields,
  null/already-empty input, unchanged relationships/flags, and repeated cleanup.
  Implemented in `cleanup/CleanupOpintoOikeus.kt` without repository or scheduler
  dependencies; A4 registers it as a Spring component for injection, while its
  unit tests still instantiate it directly. `CleanupOpintoOikeusTest` covers all six fields,
  both privacy-flag values, retained IDs/relationships/document and period
  content, unchanged modification date, null/empty/mixed input and idempotence.
  Verified with `./gradlew test --tests 'fi.elsapalvelu.elsa.cleanup.CleanupOpintoOikeusTest' --console=plain --quiet`
  (5 tests passed) and `./gradlew detektTest --console=plain --quiet` (passed).
  `./gradlew detekt detektTest --console=plain --quiet` is blocked by the
  existing empty `ScheduledDataDeletion` skeleton (`EmptyClassBlock`); no A3
  production lint issue was reported. Persistence remains part of A4.
- [x] **A4. Create the transactional orchestrator's first slice.** Load one
  opintooikeus and its training plan by ownership, invoke A3, and verify persisted
  field changes and rollback using integration tests. Do not expose or schedule
  this incomplete flow or mark a right as fully processed.
  Implemented in `cleanup/OpintooikeusCleanupOrchestrator.kt` as an internal
  Spring bean initially containing a transactional training-plan-only method. It requires the
  right to exist, loads its plan through `findOneByOpintooikeusId`, and invokes
  A3; managed JPA dirty checking persists the result without an explicit save.
  A missing plan is skipped; a missing right raises
  `EntityNotFoundException`. No endpoint, scheduler, eligibility/locking logic
  or completion marker is wired by this slice.
  `OpintooikeusCleanupOrchestratorIT` uses an isolated PostgreSQL Testcontainers
  database and committed transactions, with fresh-transaction reads. It covers
  all six fields, the modification-date callback, preserved flags/relationships/
  document binaries/training periods, another right's unchanged plan, repeated
  calls, absent plans and missing rights. A failure after flush and persistence
  context clear verifies rollback of text and modification date, then a retry.
  Verified with `./gradlew integrationTest --tests 'fi.elsapalvelu.elsa.cleanup.OpintooikeusCleanupOrchestratorIT' detektTest --console=plain --quiet`
  (5 integration tests and test lint passed). Fixture reference data uses the
  existing `YliopistoHelper.createReferenceData` convention because the
  production changelog has no `yliopisto_audit` table; target cleanup runs with
  normal callbacks and Envers enabled. `./gradlew detekt --console=plain --quiet`
  still reports only the existing empty `ScheduledDataDeletion` skeleton.

Dependencies: A3 requires A1-A2; A4 requires A3. Use the first verified slice as
the pattern for the following tasks.

### B. Add one entity type at a time

For every B task, add its dedicated method, focused field-level tests, and its
ownership-based loading/invocation to the orchestrator. Include persistence and
isolation coverage for the added type. Assert every inventoried field's empty
value and that all fields meant to be preserved remain unchanged, subject to
the documented technical timestamp behavior. Repeated cleanup must be safe.

- [x] **B1. `Paivakirjamerkinta`.** Clear event title, custom topic, and reflection.
  Added `clearPaivakirjamerkintaContent`: set `oppimistapahtumanNimi` and
  `muunAiheenNimi` to `""`, and `reflektio` to `null`. Preserve IDs, date,
  privacy flag, owner, categories and optional theory-training relationship.
  The orchestrator entry method is now `clearContent(opintooikeusId)` because
  it clears both the training plan and all diary entries in one transaction.
  It uses the existing `findAllByOpintooikeusId` diary query, not the optional
  theory-training link, and still processes entries when no training plan exists.
  Unit coverage includes both privacy values, null/empty input, retained
  relationships/content and idempotence. Integration coverage verifies multiple
  entries, linked and standalone entries, unchanged entries of another right,
  shared categories, a theory-training link pointing to another right, and
  rollback/retry of both plan and diary changes after a flush. Linked content
  is never traversed for mutation by the diary cleanup method.
  Verified with `./gradlew test --tests 'fi.elsapalvelu.elsa.cleanup.CleanupOpintoOikeusTest' integrationTest --tests 'fi.elsapalvelu.elsa.cleanup.OpintooikeusCleanupOrchestratorIT' detektTest --console=plain --quiet`
  (9 unit tests, 6 integration tests and test lint passed).
  `./gradlew detekt --console=plain --quiet` still reports only the existing
  empty `ScheduledDataDeletion` skeleton. No endpoint, scheduler or completion
  marker is connected.
- [x] **B2. `Seurantajakso`.** Clear its nine narrative/feedback fields.
  Added `clearSeurantajaksoContent`: set `omaArviointi = ""` and all eight
  optional A1 text fields to `null`, including corrections on approved records.
  The orchestrator loads all follow-ups through `findByOpintooikeusId` and
  clears them in the same transaction as the plan and diaries, even if those
  other records are absent. It never mutates linked training periods or calls
  normal workflow update methods.
  `SeurantajaksoCleanupTest` covers all nine fields, approved/unapproved records,
  unchanged structural fields and relationship identity, null/empty input and
  repeated cleanup. `SeurantajaksoCleanupIT` verifies persisted empty values,
  multiple follow-ups, another right's unchanged follow-up, a shared period
  belonging to another right, retained creation/event dates, updated
  `tallennettu`, and rollback/retry of follow-ups, plan and diary after flush.
  Approved records remain `HYVAKSYTTY`; cleared returned records derive
  `ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA`, as identified in A2.
  Permanent read-only and queue guards remain pending in C9-C10: this change
  must not be enabled for production.
  Verified with `./gradlew test --tests 'fi.elsapalvelu.elsa.cleanup.*Test' integrationTest --tests 'fi.elsapalvelu.elsa.cleanup.*IT' detektTest --console=plain --quiet`
  (13 unit tests, 9 integration tests and test lint passed).
  `./gradlew detekt --console=plain --quiet` still reports only the existing
  empty `ScheduledDataDeletion` skeleton.
- [ ] **B3. `Teoriakoulutus`.** Clear training name and location.
- [ ] **B4. `Tyoskentelypaikka`.** Clear name and custom type through its work-period owner.
- [ ] **B5. `Suoritemerkinta`.** Clear additional notes through its work-period owner.
- [ ] **B6. `KoejaksonAloituskeskustelu`.** Clear its place, goals, and correction fields.
- [ ] **B7. `KoejaksonValiarviointi`.** Clear custom category, strengths, development text, and correction.
- [ ] **B8. `KoejaksonKehittamistoimenpiteet`.** Clear correction text.
- [ ] **B9. `KoejaksonLoppukeskustelu`.** Clear further actions and correction text.
- [ ] **B10. `KoejaksonVastuuhenkilonArvio`.** Clear rejection reason and official/reviewer narrative fields.
- [ ] **B11. `KoejaksonKoulutussopimus`.** Clear both correction fields.
- [ ] **B12. `KoulutussopimuksenKouluttaja`.** Clear agreement-specific workplace/address fields.
- [ ] **B13. `KoulutussopimuksenKoulutuspaikka`.** Clear training-place name.
- [ ] **B14. `Valmistumispyynto`.** Clear its explanation, correction fields, and cover note.
- [ ] **B15. `ValmistumispyynnonTarkistus`.** Clear official summary and comments.
- [ ] **B16. `TerveyskeskuskoulutusjaksonHyvaksynta`.** Clear both corrections and official additional information.
- [ ] **B17. `Suoritusarviointi`.** Clear the assessment's five narrative fields.
- [ ] **B18. `SuoritusarvioinninKommentti`.** Clear comment body via the assessment owner.
- [ ] **B19. `SuoritusarvioinninArviointityokalunVastaus`.** Clear typed answer; preserve selected option.
- [ ] **B20. `Koulutusjakso`.** Clear title and additional goals; add cross-owner
  work-period and follow-up reference checks/tests before modifying it.
- [ ] **B21. `Asiakirja` and `AsiakirjaData`.** Add separate methods for document
  name and binary content. Guard immutable snapshots before either method runs;
  test standalone, attached, generated, shared, and snapshot documents without
  deleting rows or relationships.

All B tasks require A4 and the finalized inventory. B18-B19 require B17's
assessment loading. B12-B13 require B11's agreement loading. B15 requires B14's
request loading. B20-B21 include their own shared-reference checks; they must
not assume earlier tasks prove ownership.

### C. Make the complete flow safe to run

- [ ] **C1. Add the completion marker.** Confirm its name and add the entity
  property plus Liquibase columns for the live and Envers audit tables as
  required. Verify migration defaults for existing records.
- [ ] **C2. Add candidate-ID queries.** Select expired, unprocessed rights using
  an injected-clock date. Use stable keyset pagination so marking rows processed
  does not shift offsets, skip candidates, or endlessly retry failures in one run.
  Test before/today/future dates, processed rows, and page transitions.
- [ ] **C3. Add final eligibility checks and atomic completion.** Lock/reload
  each right, recheck the access date/marker, invoke every required B method,
  and set the marker in the same transaction. Ensure Spring transaction advice
  is applied through a separate bean call, not self-invocation.
- [ ] **C4. Verify complete rollback and retry.** Inject a failure after some
  entity updates; verify no partial changes or marker persist, a later retry
  succeeds, and an already-completed right is skipped.
- [ ] **C5. Verify full-graph isolation and exclusions.** Confirm global profiles,
  student/source identifiers, imported achievements, lookup data, snapshots,
  and prior audit revisions remain intact. Cover cross-owner references and
  define whether skipped shared content blocks completion or is an explicit
  approved exclusion; never silently mark required cleanup complete.
- [ ] **C6. Add the scheduled job, disabled by default.** Follow existing job and
  ShedLock patterns. Process rights through the transactional orchestrator;
  continue after a per-right failure and report an accurate partial/failed run.
- [ ] **C7. Verify operational concurrency.** Test date extension before processing,
  separate job-instance locking, and lock duration for the expected run time.
  A clock schedule alone must not be treated as proof that import has completed.
  Test normal content writes racing cleanup on the same right, with the
  permanent read-only guard rechecked under a shared transaction lock.
- [ ] **C8. Add read-only reporting and safe run totals.** Report candidate and
  linked-record counts plus processed/skipped/failed totals without text or
  binary contents; verify dry-run does not change data or markers.
  Include automatic debug argument/result and failure logging in this check.
- [ ] **C9. Enforce permanent read-only writes.** Implement A2's shared
  server-side marker guard for all content/workflow mutations and roles.
  Verify direct-ID access, impersonation, rejected side effects and extension
  after cleanup; structural imports must preserve the marker.
- [ ] **C10. Handle cleaned records in reads and queues.** Exclude processed
  rights from active lists/counts before pagination. Provide a clear read-only
  indication in authorized retained-record views without changing approvals
  or presenting cleared records as pending actions. Verify each role.
- [ ] **C11. Verify persistence side effects.** Cover A2's automatic timestamps,
  system-attributed new revisions, unchanged historical revisions/event dates,
  unaudited data, warmed-cache reads and no unnecessary writes for empty input.

C1 can be done independently of B. C2 requires C1. C3 requires all B tasks and
C1; C4-C5 require C3. C9-C10 require C1 and A2; C11 requires C3.
C6 requires C2-C5 and C9-C11. C7 requires C6 and C9; C8 requires the
job/reporting wiring. All must pass before enabling cleanup.

### D. Enable and verify

- [ ] **D1. Review the dry-run report.** Confirm affected rights and records,
  cross-owner exceptions, schedule/import coordination, and approval to enable.
- [ ] **D2. Deploy and enable.** Apply the migration before the application uses
  it; enable only after all required implementation and verification tasks pass.
- [ ] **D3. Verify the first production run.** Inspect safe totals and failures,
  confirm intended content is empty and preserved data is unchanged, and verify
  failures remain retryable and completed rights are not processed again.

D1 requires C7-C8 and the completed field inventory. D2 requires D1 and every
implementation task above; D3 follows D2.
