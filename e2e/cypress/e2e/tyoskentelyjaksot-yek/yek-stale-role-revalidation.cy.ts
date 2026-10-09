import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'

export {}

// ELSAINSI-73
//
// Root cause: the YEK route guard (role-specific-route.vue) only checked a
// cached `activeAuthority` from the Vuex store, which was only refreshed on
// full page load/login. If the server-side "kaytossa" opinto-oikeus changed
// while a tab stayed open (another tab switching active profile, or the
// nightly opintotieto import reconciling it), that tab kept believing it was
// still allowed on a YEK page and let the route render - only to have the
// backend reject the actual data request against its now-current state.
//
// This test reproduces that precondition directly (switch the role server-side without ever
// touching the open tab's client state - the "another tab did it" case) and
// asserts the fix: the guard re-validates when the tab becomes visible again,
// *before* the stale route gets a chance to fire a doomed request.
describe('YEK-roolin ja opinto-oikeuden synkronointi (ELSAINSI-73)', () => {
  const yekOpintooikeusId = 610520
  let seededYekOpintooikeusId: number
  let elOpintooikeusId: number

  const yekOpintooikeus: OpintoOikeus = {
    asetus_id: 5,
    erikoisala_id: 61,
    erikoistuva_laakari_id: 0,
    kaytossa: false,
    muokkausaika: '2021-01-04',
    muokkausoikeudet_virkailijoilla: true,
    myontamispaiva: '2021-01-04',
    opintoopas_id: 17,
    opiskelijatunnus: '',
    osaamisen_arvioinnin_oppaan_pvm: '2022-09-07',
    paattymispaiva: '2029-05-05',
    terveyskeskuskoulutusjakso_suoritettu: false,
    yliopisto_opintooikeus_id: 'e2e-yek-stale-role',
    tila: 'AKTIIVINEN',
    viimeinen_katselupaiva: '2029-11-05',
    yliopisto_id: 5,
    id: yekOpintooikeusId
  }

  before(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
    cy.task('db:getActiveOpintooikeusId', { email: E2E_ERIKOISTUVA_EMAIL }).then((id) => {
      expect(Number(id)).to.be.greaterThan(0)
      elOpintooikeusId = Number(id)
    })
    // Keep the EL right created by login unchanged: SQL updates to that row
    // would bypass Hibernate's cached entity and collection state. Add an
    // inactive YEK right, then select it through the role-switch API below.
    cy.task('db:seedOpintooikeus', {
      email: E2E_ERIKOISTUVA_EMAIL,
      opintoOikeus: yekOpintooikeus,
      generateId: true
    }).then((id) => {
      seededYekOpintooikeusId = Number(id)
    })
    cy.logout()
  })

  it(
    'estää YEK-sivun näyttämisen eikä yritä hakea tietoja, kun käytössä oleva ' +
      'opinto-oikeus on vaihtunut toisessa välilehdessä/taustalla sillä aikaa ' +
      'kun tämä välilehti on ollut auki',
    () => {
      cy.visit('/kirjautuminen')
      cy.contains('Kirjaudu sisään (Suomi.fi)').click()

      cy.origin('https://testi.apro.tunnistus.fi', () => {
        cy.get('body').then(($body) => {
          const exists = $body.find('[name="_eventId_proceed"]').length > 0
          if (exists) {
            cy.get('[name="_eventId_proceed"]').click()
          } else {
            cy.get('#continue-button').click()
          }
        })
      })

      cy.location('origin', { timeout: 60000 }).should(
        'eq',
        new URL(Cypress.config('baseUrl') as string).origin
      )
      // Returning to the application origin does not mean the SAML landing
      // page has finished authorizing and restoring the post-login route.
      cy.location('pathname', { timeout: 60000 }).should('eq', '/etusivu')
      cy.get('main[role="main"]').should('be.visible')
      cy.contains('a', 'Työskentelyjaksot').should('be.visible')

      // Always select YEK through the API: an already active YEK authority
      // alone does not prove that its study right is also selected.
      cy.getCookie('XSRF-TOKEN').then((cookie) => {
        cy.request({
          method: 'POST',
          url: '/api/vaihda-rooli',
          form: true,
          body: { rooli: 'ROLE_YEK_KOULUTETTAVA' },
          headers: { 'X-XSRF-TOKEN': cookie?.value ?? '' }
        })
      })

      cy.request('/api/kayttaja')
        .its('body.activeAuthority')
        .should('eq', 'ROLE_YEK_KOULUTETTAVA')
      cy.request('/api/erikoistuva-laakari')
        .its('body.opintooikeusKaytossaId')
        .should('eq', seededYekOpintooikeusId)

      // Tab opens on the YEK page while YEK is genuinely the active context -
      // this must succeed, same as the existing happy-path test.
      cy.intercept('GET', '**/yek-koulutettava/tyoskentelyjaksot-taulukko').as(
        'initialYekTaulukkoRequest'
      )
      cy.intercept('GET', '**/yek-koulutettava/etusivu/erikoistumisen-edistyminen').as(
        'initialYekEdistyminenRequest'
      )
      cy.visit('/yektyoskentelyjaksot')
      cy.contains('h1', 'Työskentelyjaksot').should('be.visible')
      // The heading renders before mounted() finishes its two sequential requests.
      // Drain the initial load before tracking requests made with a stale role.
      cy.wait('@initialYekTaulukkoRequest').its('response.statusCode').should('eq', 200)
      cy.wait('@initialYekEdistyminenRequest').its('response.statusCode').should('eq', 200)
      cy.get('main[role="main"] .spinner-border').should('not.exist')

      // Register before the switch: the fix may react to Cypress focus events
      // at any moment after the server-side switch, so later registration would race.
      // A request to the now-invalid YEK endpoint must never fire from this
      // point on - if the fix regresses, this is what would reveal it.
      cy.intercept('GET', '**/yek-koulutettava/tyoskentelyjaksot-taulukko').as(
        'staleYekTaulukkoRequest'
      )
      cy.intercept('GET', '**/yek-koulutettava/etusivu/**').as('staleYekEtusivuRequest')

      // Simulate what happened in prod: another tab switches the role to EL.
      // Done with cy.request (same backend call the navbar makes) so that NO
      // frontend code runs in this tab - its cached account state stays stale,
      // exactly like the real incident. Note: do not fake this with raw SQL on
      // jhi_user - User is in Hibernate's 2nd-level cache, so the backend would
      // keep serving the old active role.
      cy.getCookie('XSRF-TOKEN').then((cookie) => {
        cy.request({
          method: 'POST',
          url: '/api/vaihda-rooli',
          form: true,
          body: { rooli: 'ROLE_ERIKOISTUVA_LAAKARI' },
          headers: { 'X-XSRF-TOKEN': cookie?.value ?? '' }
        })
      })

      // Simulate the tab regaining visibility (e.g. laptop woken, or the user
      // switching back to it) without a manual reload - one of the triggers
      // the fix listens for (others: window focus, in-app navigation).
      cy.document().then((doc) => {
        Object.defineProperty(doc, 'hidden', { value: false, configurable: true })
        Object.defineProperty(doc, 'visibilityState', { value: 'visible', configurable: true })
        doc.dispatchEvent(new Event('visibilitychange'))
      })

      // The tab re-syncs (full reload) with the server's current role. The YEK
      // route is not available for the EL role, so the guard shows 404 instead
      // of rendering the stale YEK view and firing doomed requests.
      cy.contains('Sivua ei löytynyt', { timeout: 15000 }).should('be.visible')
      cy.request('/api/kayttaja')
        .its('body.activeAuthority')
        .should('eq', 'ROLE_ERIKOISTUVA_LAAKARI')
      cy.request('/api/erikoistuva-laakari')
        .its('body.opintooikeusKaytossaId')
        .should('eq', elOpintooikeusId)
      cy.get('@staleYekTaulukkoRequest.all').should('have.length', 0)
      cy.get('@staleYekEtusivuRequest.all').should('have.length', 0)
    }
  )
})
