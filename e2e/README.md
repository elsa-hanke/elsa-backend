# E2E Tests

End-to-end tests for the Elsa backend, powered by [Cypress](https://www.cypress.io/)

## Prerequisites

- Node.js ≥ 20 (required by Cypress 15)
- The application running on `http://localhost:9060`

## Setup

```bash
cd e2e
yarn install
```

## Running tests

| Command | Description |
|---------|-------------|
| `yarn cy:open` | Open the interactive Cypress test runner |
| `yarn cy:run` | Run all tests headlessly (CI mode) |
| `yarn cy:run:headed` | Run all tests in a visible browser |

From the project root you can also use:

```bash
just e2e        # headless run
just e2e-open   # interactive runner
```

## Suomi.fi logout regression

`cypress/e2e/auth/suomifi-logout.cy.ts` logs in without `cy.session()`, uses the
real navbar logout, waits for the SAML logout callback, and checks that the next
login requires a new HETU. It does not clear cookies or storage between logins.
The test also verifies that the ELSA session is no longer authenticated.

Run against the isolated E2E stack with Suomi.fi test-IdP credentials configured:

```bash
cd e2e
yarn cy:run --spec cypress/e2e/auth/suomifi-logout.cy.ts
```

The setup cleans the E2E user through database tasks; use the E2E database.

## Stored rich-text XSS regressions

`cypress/e2e/security/rich-text-*.cy.ts` adds seven browser scenarios:

- `rich-text-public.cy.ts`: raw historical announcement in the E2E database → real
  public API → anonymous login page; the stored original must remain unchanged.
  A separate intercepted, dirty response checks frontend protection independently.
- `rich-text-admin.cy.ts`: real administrator POST/PUT bypass TinyMCE; inspect the
  saved database value so read-time sanitization cannot hide unsafe persistence.
  Load dirty historical HTML into the actual TinyMCE iframe, assert that opening
  sends no writes, then edit, save through the real API and reopen. Headings,
  lists, tables, links and Finnish/Swedish characters must survive.
  Another test types into the middle of a Finnish paragraph through the actual
  contenteditable body and checks the caret, explicit undo checkpoints, redo,
  saving and reopening, covering the named-entity/v-model feedback regression.
- `rich-text-assessments.cy.ts`: dirty intercepted assessment descriptions in FI/SV
  and assessment-tool instructions render safely on real trainer pages. The pilot
  hides the language selector, so this test changes the mounted Vue i18n locale.

The small browser payload set reuses the JVM/Jest corpus in
`../src/test/resources/security/rich-text-cases.json`. GHA mounts that directory
read-only into the Cypress container. Custom Docker invocations that mount only
`e2e/` also need this corpus at `/src/test/resources/security`.

Assertions inspect browser DOM and an execution spy installed before application
loading (including execution inside the TinyMCE iframe). They do not compute
expected results with the production sanitizer or suppress browser exceptions.

Run only against the disposable E2E stack. DB tasks own announcement IDs/run
markers and separate `e2e-rich-text-*` users; they never delete ordinary content
or the other specs' users. Authenticated scenarios reuse the known test-IdP
identities through real invitation/SAML login. If an identity is already linked
to another test account, its encrypted HETU is temporarily detached and restored
by the cleanup hook, preventing automatic account merging or role grants to that
account. The original encrypted value stays in Node memory, outside Cypress logs.
Specs must run serially within each database, as in the existing GHA shard setup.
After forcibly killing Cypress before cleanup, recreate the disposable database
before another run; in-memory restoration state is lost on process termination.

The existing shard discovery includes the new specs automatically; no explicit
spec list or additional GHA job is required.

## Approval → scheduled archive job

The existing approval UI specs also verify asynchronous archive scheduling for
training contracts, positive and negative trial-period decisions, and EL/EHL/YEK
graduation. After the real Approve response, `assertArchiveScheduled` reads the
exact operation business key and checks one committed `ODOTTAA` job, approval
metadata, document order, nonempty snapshots, SHA-256 and source/snapshot equality.
One unpicked `arkistointi-job` scheduler task must reference the job UUID.

The ordinary Compose stack mounts `infra/application-e2e.yml`. It enables async
job creation for HKI/UEF while keeping the worker disabled, so no external archive
system is contacted. HKI graduation archives its configured summary only; the UEF
YEK fixture explicitly configures summary and attachments. No separate archive
specs, JAR, CI phase or per-spec stack restart is needed. Full Tampere backend
delivery through ZIP/SFTP remains covered by `TampereAsyncApprovalIT`.

The shared cleanup removes only archive data belonging to the selected fixture's
study rights before deleting the parent records. Its own connection and transaction
use `SET LOCAL session_replication_role = replica` to bypass immutable triggers in
the disposable test DB. Scheduler rows, document links, jobs, snapshots and snapshot
bytes are deleted explicitly because FK checks and cascades are disabled too.
COMMIT/ROLLBACK restores the setting before ordinary cleanup continues. Production
migrations and protections remain unchanged. Specs run serially within each shard's
DB, with one ordinary stack per shard.

EHL is a variant of the ordinary graduation spec. Its SQL fixture selects a dental
specialty and clones the active study guide. Hibernate caches are disabled in the
external E2E configuration because the fixture changes the study right after login.

## Parallel GitHub Actions runs

`.github/workflows/e2e.yml` runs three shards on separate runners. Each shard
starts its own Docker Compose stack, including PostgreSQL, so test-user cleanup
cannot interfere with the other shards. All three shards finish even if one fails;
the reusable workflow must succeed before dependent deployment jobs proceed.

`scripts/split-specs.mjs` discovers all `cypress/e2e/**/*.cy.ts` files and assigns
each file to exactly one shard. It balances estimated execution time; the number
of files per shard may differ. `scripts/spec-durations.json` contains
per-spec durations in seconds from the supplied GHA execution log (55 specs,
108 tests). Update those estimates when execution times change substantially.
New specs automatically receive the mean recorded duration until measured.

To inspect a shard without running tests:

```bash
cd e2e
node scripts/split-specs.mjs 1 3
```

The script prints the comma-separated Cypress `--spec` argument to stdout and
the selected files and estimated duration to stderr. CI passes that list to
Cypress; local commands above still run the full suite. Artifact names include
the shard number so logs and screenshots remain separate.

To change the shard count, edit only `strategy.matrix.shard` in the workflow
(for example, `[1, 2, 3, 4]`). The shard total and job names use
`strategy.job-total`, and there is no fixed parallelism limit. Each job has a
30-minute timeout. Before services start, the script runs directly on the GitHub
runner and writes its assigned specs and estimated execution time to the GitHub
Actions summary. The selected list is passed to the Cypress container through a
step output and environment variable; the summary file is not mounted into Docker.
This list does not indicate test results.

Each shard uploads an `e2e-results-N` artifact with its assignment manifest,
per-spec Cypress counts, and a completion marker. Results are saved after each
spec, retaining partial counts if a later spec or the runner fails. The
`E2E combined report` job runs after all shards, including failed shards, and
publishes a table of spec and test totals (passing, failing, pending, skipped)
in the run summary. It also uploads `e2e-combined-report` containing Markdown
and JSON reports. Missing shards or specs are explicitly marked incomplete;
the report job fails on incomplete results, failing tests, or failed shard jobs.
The test total is measured at runtime and is not fixed at the original 108.

The formatted report is displayed on the workflow run's **Summary** page;
step logs use plain text because GitHub does not render Markdown there.
The report separates PASSED/FAILED from spec coverage and includes failing
test names, expandable error details, Cypress execution time per shard, and
links to available screenshot and container-log artifacts. Test retries count
as one test; the number of attempts is shown with failure details. Older
count-only artifacts remain supported but cannot show individual test errors.

## Structure

```
e2e/
├── cypress.config.ts          # Cypress configuration
├── tsconfig.json              # TypeScript configuration
├── cypress/
│   ├── e2e/
│   │   └── auth/
│   │       └── login.cy.ts    # Suomi.fi login test
│   ├── fixtures/              # Static test data (JSON)
│   └── support/
│       ├── commands.ts        # Custom commands (e.g. cy.loginWithSuomifi)
│       └── e2e.ts             # Support entry point
└── plugins/
    └── index.ts               # Legacy plugin file (kept for reference)
```

## Custom commands

### `cy.loginWithSuomifi(email?)`

Performs the full Suomi.fi test-identity-provider login flow:

1. Visits `/kirjautuminen`
2. Clicks **Kirjaudu sisään (Suomi.fi)**
3. Selects **Testitunnistaja**
4. Enters SSN `210281-9988` and clicks **Tunnistaudu**
5. Clicks **Jatka palveluun**
6. Fills in the e-mail fields and clicks **Aloita palvelun käyttö**

```ts
cy.loginWithSuomifi('user@example.com')
```
