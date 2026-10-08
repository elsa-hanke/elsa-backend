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
// This test reproduces that precondition directly (flip the DB without ever
// touching the open tab's client state - the "another tab did it" case) and
// asserts the fix: the guard re-validates when the tab becomes visible again,
// *before* the stale route gets a chance to fire a doomed request.
describe('YEK-roolin ja opinto-oikeuden synkronointi (ELSAINSI-73)', () => {
  const yekOpintooikeusId = 610520
  const elOpintooikeusId = 610521

  const yekOpintooikeus: OpintoOikeus = {
    asetus_id: 5,
    erikoisala_id: 61,
    erikoistuva_laakari_id: 0,
    kaytossa: true,
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

  // A second, independently valid opinto-oikeus for a regular erikoisala
  // (not YEK) - this is what a nightly import or a profile switch in another
  // tab would make "kaytossa" instead of the YEK one.
  const elOpintooikeus: OpintoOikeus = {
    ...yekOpintooikeus,
    erikoisala_id: 50,
    kaytossa: false,
    yliopisto_opintooikeus_id: 'e2e-el-stale-role',
    id: elOpintooikeusId
  }

  before(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
    // First row becomes kaytossa=true (also grants ROLE_YEK_KOULUTETTAVA, see
    // db:seedOpintooikeus), second row is inserted alongside it as a valid
    // but currently inactive alternative - mirrors the real support case,
    // where the affected user had both an EL and a YEK opinto-oikeus.
    cy.task('db:seedOpintooikeus', {
      email: E2E_ERIKOISTUVA_EMAIL,
      opintoOikeus: yekOpintooikeus,
      updateCurrent: true
    })
    cy.task('db:seedOpintooikeus', {
      email: E2E_ERIKOISTUVA_EMAIL,
      opintoOikeus: elOpintooikeus,
      updateCurrent: false
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

      cy.request('/api/kayttaja').then(({ body }) => {
        if (body.activeAuthority === 'ROLE_YEK_KOULUTETTAVA') {
          return
        }

        cy.getCookie('XSRF-TOKEN').then((cookie) => {
          cy.request({
            method: 'POST',
            url: '/api/vaihda-rooli',
            form: true,
            body: { rooli: 'ROLE_YEK_KOULUTETTAVA' },
            headers: { 'X-XSRF-TOKEN': cookie?.value ?? '' }
          })
        })
      })

      cy.request('/api/kayttaja')
        .its('body.activeAuthority')
        .should('eq', 'ROLE_YEK_KOULUTETTAVA')

      // Tab opens on the YEK page while YEK is genuinely the active context -
      // this must succeed, same as the existing happy-path test.
      cy.visit('/yektyoskentelyjaksot')
      cy.contains('h1', 'Työskentelyjaksot').should('be.visible')

      // Simulate what happened in prod: the active opinto-oikeus flips on the
      // server - via another tab, or the nightly import - entirely outside
      // this tab. No frontend code runs here, so this tab's cached account
      // state is untouched, exactly like the real incident.
      cy.task('db:switchOpintooikeusKaytossa', {
        email: E2E_ERIKOISTUVA_EMAIL,
        fromId: yekOpintooikeusId,
        toId: elOpintooikeusId,
        activeAuthority: 'ROLE_ERIKOISTUVA_LAAKARI'
      })

      cy.intercept('GET', '**/api/kayttaja').as('accountRefresh')
      // A request to the now-invalid YEK endpoint must never fire from this
      // point on - if the fix regresses, this is what would reveal it.
      cy.intercept('GET', '**/yek-koulutettava/tyoskentelyjaksot-taulukko').as(
        'staleYekTaulukkoRequest'
      )

      // Simulate the tab regaining visibility (e.g. laptop woken, or the user
      // switching back to it) without a manual reload - this is the trigger
      // the fix listens for.
      cy.document().then((doc) => {
        Object.defineProperty(doc, 'hidden', { value: false, configurable: true })
        Object.defineProperty(doc, 'visibilityState', { value: 'visible', configurable: true })
        doc.dispatchEvent(new Event('visibilitychange'))
      })

      cy.wait('@accountRefresh')
        .its('response.body.activeAuthority')
        .should('eq', 'ROLE_ERIKOISTUVA_LAAKARI')

      // The guard must now block the page instead of rendering the stale
      // YEK view and letting it fire a doomed request.
      cy.contains('Sivua ei löytynyt').should('be.visible')
      cy.get('@staleYekTaulukkoRequest.all').should('have.length', 0)
    }
  )
})
