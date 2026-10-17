export {}

const MAIN_EXAM = 'VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU'
const ESSAYS = 'Patologia, esseet'
const SPECIMENS = 'Patologia, preparaatit'

// Backend import/reconciliation is covered by persistence ITs. This spec checks the
// customer-visible result with a controlled API response, without querying Peppi.
const subpart = (id: number, name: string, passed: boolean) => ({
  id,
  nimi_fi: name,
  nimi_sv: '',
  kurssikoodi: `fixture-part-${id}`,
  suorituspaiva: '2026-05-07',
  opintopisteet: 0,
  hyvaksytty: passed,
  arvio_fi: passed ? 'Hyväksytty' : 'Hylätty',
  arvio_sv: null,
  vanhenemispaiva: null,
})

const exam = (children: ReturnType<typeof subpart>[] | null, passed: boolean) => ({
  id: 91001,
  nimi_fi: MAIN_EXAM,
  nimi_sv: MAIN_EXAM,
  kurssikoodi: 'ELOP0001',
  tyyppi: { id: 3, nimi: 'VALTAKUNNALLINEN_KUULUSTELU' },
  suorituspaiva: '2026-05-07',
  opintopisteet: 0,
  hyvaksytty: passed,
  arvio_fi: passed ? 'Hyväksytty' : 'Hylätty',
  arvio_sv: null,
  vanhenemispaiva: null,
  yliopistoOpintooikeusId: 'fixture-study-right',
  osakokonaisuudet: children,
})

const stubExamResponse = (value: ReturnType<typeof exam>) => {
  cy.intercept('GET', '**/erikoistuva-laakari/opintosuoritukset', {
    statusCode: 200,
    body: {
      opintosuoritukset: [value],
      johtamisopinnotSuoritettu: 0,
      johtamisopinnotVaadittu: 10,
      sateilysuojakoulutuksetSuoritettu: 0,
      sateilysuojakoulutuksetVaadittu: 0,
    },
  }).as('examAccomplishments')
}

const openExamTab = () => {
  cy.wait('@examAccomplishments', { timeout: 15000 }).its('response.statusCode').should('eq', 200)
  cy.contains('h1', 'Opintosuoritukset').should('be.visible')
  cy.contains('.nav-link', 'Kuulustelu').click()
  cy.get('.tab-pane.active .opintosuoritus-table').should('be.visible')
}

const assertExamAndSubparts = () => {
  cy.get('.tab-pane.active .opintosuoritus-table').within(() => {
    cy.get('tbody tr').should('have.length', 3)
    cy.contains('tbody tr', MAIN_EXAM).within(() => {
      cy.get('td').eq(1).invoke('text').should('match', /0?7\.0?5\.2026/)
      cy.get('td').eq(2).should('contain.text', 'Hylätty').and('not.contain.text', 'Hyväksytty')
    })
    cy.get('.exam-subpart-row').should('have.length', 2)
    cy.contains('.exam-subpart-row', ESSAYS).within(() => {
      cy.get('td').eq(1).invoke('text').should('match', /0?7\.0?5\.2026/)
      cy.get('td').eq(2).should('contain.text', 'Hyväksytty').and('not.contain.text', 'Hylätty')
    })
    cy.contains('.exam-subpart-row', SPECIMENS).within(() => {
      cy.get('td').eq(1).invoke('text').should('match', /0?7\.0?5\.2026/)
      cy.get('td').eq(2).should('contain.text', 'Hylätty').and('not.contain.text', 'Hyväksytty')
    })
  })
}

describe('Kuulustelun osasuoritukset', () => {
  before(() => {
    cy.resetErikoistuvaE2eState()
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
  })

  it('näyttää pääkuulustelun ja osasuoritusten omat tilat myös sivun uudelleenlatauksen jälkeen', () => {
    stubExamResponse(exam([subpart(91003, SPECIMENS, false), subpart(91002, ESSAYS, true)], false))
    cy.visit('/opintosuoritukset')
    openExamTab()
    assertExamAndSubparts()

    cy.reload()
    openExamTab()
    assertExamAndSubparts()
  })

  for (const children of [null, [] as ReturnType<typeof subpart>[]]) {
    it(`näyttää pelkän pääkuulustelun kun osakokonaisuudet on ${children === null ? 'null' : 'tyhjä lista'}`, () => {
      stubExamResponse(exam(children, true))
      cy.visit('/opintosuoritukset')
      openExamTab()
      cy.get('.tab-pane.active .opintosuoritus-table').within(() => {
        cy.get('tbody tr').should('have.length', 1)
        cy.get('.exam-subpart-row').should('not.exist')
        cy.contains('tbody tr', MAIN_EXAM).within(() => {
          cy.get('td').eq(2).should('contain.text', 'Hyväksytty').and('not.contain.text', 'Hylätty')
        })
      })
    })
  }
})
