declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Opens a Vue Multiselect dropdown and picks the first non-group,
       * non-disabled option.
       * @param container - Cypress chainable pointing to the multiselect wrapper element.
       */
      selectFirstMultiselectOption(container: Cypress.Chainable<JQuery>): void
    }
  }
}

// ── selectFirstMultiselectOption ─────────────────────────────────────────────
// Avaa multiselect-pudotusvalikko ja valitsee ensimmäisen ei-ryhmäotsikko-vaihtoehdon.
// Käyttää `.multiselect--active`-skoopausta välttääkseen Cypressin ketjutusongelman,
// joka aiheuttaa haun väärästä kontekstista. Toimii sekä ryhmitetyillä että
// ryhmittämättömillä monivalintavalikoilla.
Cypress.Commands.add(
  'selectFirstMultiselectOption',
  (container: Cypress.Chainable<JQuery>) => {
    container.find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option:not(.multiselect__option--group):not(.multiselect__option--disabled)')
      .first()
      .click({ force: true })
  }
)

export {}

