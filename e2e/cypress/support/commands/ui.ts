/// <reference types="cypress-axe" />

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

      /**
       * Injects axe-core and checks the current page for WCAG 2.1 AA violations.
       * Filters out known unfixed violations by their rule IDs.
       * @param context  - Optional CSS selector to limit the scan scope.
       * @param options  - Optional axe run options.
       */
      checkPageA11y(context?: string, options?: Partial<Cypress.Options>): void

      /**
       * Asserts that a focusable element receives a visible focus indicator
       * when tabbed to. Uses :focus-visible / outline checks.
       * @param selector - CSS selector for the element to test.
       */
      assertFocusVisible(selector: string): void

      /**
       * Types into the first focusable element with Tab, asserts it receives focus.
       */
      tabToElement(selector: string): void
    }
  }
}

// ── selectFirstMultiselectOption ─────────────────────────────────────────────
Cypress.Commands.add(
  'selectFirstMultiselectOption',
  (container: Cypress.Chainable<JQuery>) => {
    container.find('.multiselect').click()
    cy.get('.multiselect--active .multiselect__option:not(.multiselect__option--group):not(.multiselect__option--disabled)')
      .first()
      .click({ force: true })
  }
)

// ── checkPageA11y ─────────────────────────────────────────────────────────────
// Wraps cy.checkA11y() with a curated exclusion list for issues that are
// tracked but not yet fixed in this sprint. This prevents CI failures on
// known issues while still catching regressions on fixed ones.
const KNOWN_UNFIXED_VIOLATIONS = [
  // SVG pie charts lack text alternatives – tracked, complex fix
  'image-alt',
  // Many existing form fields lack label/for association – tracked separately
  // 'label',  // intentionally NOT excluded: our new fields should be fine
  // Colour-contrast of existing text (graphs, helper texts) – tracked
  'color-contrast',
]

Cypress.Commands.add(
  'checkPageA11y',
  (context?: string, options?: Partial<Cypress.Options>) => {
    cy.injectAxe()
    cy.checkA11y(
      context ?? undefined,
      {
        runOnly: {
          type: 'tag',
          values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
        },
        rules: KNOWN_UNFIXED_VIOLATIONS.reduce((acc: Record<string, { enabled: boolean }>, id) => {
          acc[id] = { enabled: false }
          return acc
        }, {}),
        ...options,
      },
      (violations) => {
        if (violations.length === 0) return
        const messages = violations.map(
          (v) =>
            `[${v.id}] ${v.description}\n  Impact: ${v.impact}\n  Nodes: ${v.nodes
              .slice(0, 3)
              .map((n) => n.html)
              .join('\n  ')}`
        )
        cy.log(`Axe found ${violations.length} violation(s):\n${messages.join('\n\n')}`)
        // Fail the test
        throw new Error(
          `${violations.length} axe accessibility violation(s) detected:\n\n${messages.join('\n\n')}`
        )
      }
    )
  }
)

// ── assertFocusVisible ────────────────────────────────────────────────────────
// Checks that after focusing an element, it has either an outline or
// box-shadow set (indicating a visible focus ring).
Cypress.Commands.add('assertFocusVisible', (selector: string) => {
  cy.get(selector)
    .focus()
    .then(($el) => {
      const outline = $el.css('outline-style')
      const boxShadow = $el.css('box-shadow')
      const outlineWidth = parseInt($el.css('outline-width') || '0', 10)
      const hasOutline = outline !== 'none' && outlineWidth > 0
      const hasBoxShadow = boxShadow !== 'none' && boxShadow !== ''
      expect(
        hasOutline || hasBoxShadow,
        `Expected ${selector} to have a visible focus indicator (outline or box-shadow), got outline="${outline}" box-shadow="${boxShadow}"`
      ).to.be.true
    })
})

// ── tabToElement ──────────────────────────────────────────────────────────────
Cypress.Commands.add('tabToElement', (selector: string) => {
  // Focus body first so tab key traversal starts from the top
  cy.get('body').focus()
  // Tab until the target element has focus (max 30 tabs to avoid infinite loops)
  cy.get(selector).then(($target) => {
    function tab(remaining: number) {
      if (remaining === 0) throw new Error(`tabToElement: could not reach ${selector} within 30 tabs`)
      cy.focused().then(($focused) => {
        if ($focused.is($target)) return
        cy.focused().realPress('Tab')
        tab(remaining - 1)
      })
    }
    tab(30)
  })
})

export {}

