# E2E Tests

End-to-end tests for the Elsa backend, powered by [Cypress](https://www.cypress.io/).

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
