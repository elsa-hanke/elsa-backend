declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Makes an API request with XSRF token automatically included.
       * @param options Partial<Cypress.RequestOptions>
       */
      apiRequest(options: Partial<Cypress.RequestOptions>): Chainable<any>
    }
  }
}

// ── apiRequest ───────────────────────────────────────────────────────────────
Cypress.Commands.add('apiRequest', (options: Partial<Cypress.RequestOptions>) => {
  return cy.getCookie('XSRF-TOKEN').then((cookie) =>
    cy.request({
      ...options,
      headers: {
        'X-XSRF-TOKEN': decodeURIComponent(cookie?.value ?? ''),
        ...(options.headers ?? {}),
      },
    })
  )
})

export {}

