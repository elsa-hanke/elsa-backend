# Special-character regression tests

These specs target `release/v2.4.X` with the customer-report and follow-up
special-character fixes applied. The normal Cypress spec pattern includes them.

| Spec | Coverage |
| --- | --- |
| `koulutussopimus-erikoismerkit.cy.ts` | Trainee create/draft update fields, trainer fields, responsible-person contacts/corrections, browser validation feedback and retry, legacy training-place text rejected during PDF approval. |
| `vastuuhenkilon-arvio-erikoismerkit.cy.ts` | Trainee contacts on create/update, attachment filenames, officer HTML summaries and notes, approval/return corrections, responsible-person rejection reasons, legacy encoded text rejected during PDF approval. |
| `koejakson-keskustelut-erikoismerkit.cy.ts` | Every newly validated text field in aloituskeskustelu, valiarviointi and loppukeskustelu, followed by a successful corrected save. |

Requests use the real backend. `cy.intercept()` only observes browser traffic;
there are no stubbed validation responses. Direct API requests cover contact
values that the browser's email/phone format checks would otherwise reject
before they reach backend character validation.

The HTML cases include literal hearts, hexadecimal/decimal entities, a named
entity, valid Finnish text and formatting, escaped literal entity text, and an
encoded Word/Wingdings bullet. Encoded HTML is submitted through the API to
prevent TinyMCE from normalising away the exact regression input.

`db:pdfTextState` captures form rows, trainer/place rows, user contacts and
PDF/attachment records. Each rejected request must leave this snapshot unchanged.
`db:setLegacyPdfText` changes only the selected test resident's training place.
`db:createLegacyPdfReview` inserts an officer-approved review with unsupported
HTML before the backend first loads it. Updating an already loaded review with
SQL would leave Hibernate's second-level cache stale; reloading the browser does
not clear that cache. The test asserts that the API sees the exact legacy summary
before approving. After the expected PDF rejection, it returns the review,
resubmits as the trainee, and corrects the summary as the officer through the real
API before retrying approval. No cached review is repaired with direct SQL.

Setup and cleanup use the existing shared test users and koejakso DB tasks.
The final-review PDF cases assert that external archiving is disabled, as in the
existing approval test setup.

To select only these specs in a configured E2E environment:

```bash
cd e2e
yarn cy:run --spec 'cypress/e2e/pdf-text/*.cy.ts'
```
