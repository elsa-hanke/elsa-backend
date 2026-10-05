# Upload validation fixtures

These synthetic files test upload messages without customer data. The protected PDFs
are derived from the existing `../test.pdf`. Run `generate.py` only when regenerating
fixtures; Cypress uses the checked-in files and needs no Python dependency.

| File | Expected result |
| --- | --- |
| `password-required.pdf` | Backend rejects: opening password required (`test-password`). |
| `copy-restricted.pdf` | Accepted: empty opening password, copying/editing restricted. |
| `invalid-content.pdf` | Backend rejects: non-PDF content labelled as PDF. |
| `truncated.pdf` | Backend rejects: incomplete PDF, padded above the UI minimum. |
| `no-pages.pdf` | Backend rejects: readable PDF structure but no pages. |
| `empty.pdf` | Browser rejects: zero bytes, distinct from too small. |
| `too-small.pdf` | Browser rejects: valid PDF below the 10 KiB minimum. |
| `unsupported.txt` | Browser rejects: unsupported file type. |

The spec reuses `../test.pdf` under descriptive names for duplicates and constructs
`too-large.pdf` and `total-size-*.pdf` in memory, avoiding large committed files.
Long filenames are outside the scope of these Cypress tests.
