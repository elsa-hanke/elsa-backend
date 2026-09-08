import type { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'
import { E2E_ERIKOISTUVA_EMAIL, SSN_ERIKOISTUVA } from '../../support/commands/credentials'

const YEK_ROLE = 'ROLE_YEK_KOULUTETTAVA'
const API = '/api/yek-koulutettava'
const yekOpintooikeus: OpintoOikeus = {
  id: 0,
  erikoistuva_laakari_id: 0,
  erikoisala_id: 61,
  yliopisto_id: 5,
  opintoopas_id: 17,
  asetus_id: 5,
  kaytossa: true,
  myontamispaiva: '2019-01-01',
  paattymispaiva: '2099-12-31',
  viimeinen_katselupaiva: '2099-12-31',
  muokkausaika: '2022-09-07',
  osaamisen_arvioinnin_oppaan_pvm: '2022-09-07',
  yliopisto_opintooikeus_id: 'e2e-yek-documents',
  tila: 'AKTIIVINEN',
  terveyskeskuskoulutusjakso_suoritettu: false,
  muokkausoikeudet_virkailijoilla: false,
}

function assertYekOnlySession() {
  cy.request('/api/kayttaja').then(({ body }) => {
    expect(body.authorities).to.have.members([YEK_ROLE])
    expect(body.activeAuthority).to.eq(YEK_ROLE)
    expect(body.impersonated).not.to.eq(true)
  })
}

function assertSpecialistRouteForbidden(id: number) {
  // cy.request bypasses the frontend's 403/logout interceptor. This proves
  // that a second role cannot accidentally hide the original regression.
  cy.request({
    url: `/api/erikoistuva-laakari/asiakirjat/${id}`,
    failOnStatusCode: false,
    followRedirect: false,
  }).its('status').should('eq', 403)
}

function assertPreviewAndDownload(id: number, fileName: string, pathname: string) {
  // Match either role's route so the old code fails on the URL assertion,
  // rather than merely timing out waiting for a YEK request.
  cy.intercept('GET', `**/api/*/asiakirjat/${id}`).as('documentContent')
  const assertContent = () => {
    cy.wait('@documentContent').then(({ request, response }) => {
      expect(new URL(request.url).pathname).to.eq(`${API}/asiakirjat/${id}`)
      expect(response?.statusCode).to.eq(200)
      expect(response?.headers['content-type']).to.include('application/pdf')
      expect(response?.headers['content-disposition']).to.include('attachment')
    })
  }

  cy.get('@openDocument').invoke('resetHistory')
  cy.contains('.asiakirjat-table tr', fileName).find('td.file-name button').click()
  assertContent()
  cy.get('@openDocument').should('have.been.calledOnce')
  cy.get('@openDocument').its('firstCall.args.0').should('match', /^blob:/)

  cy.contains('.asiakirjat-table tr', fileName).find('td.download-btn button').click()
  assertContent()
  cy.fixture('test.pdf', 'binary').then((expected) => {
    cy.readFile(`${Cypress.config('downloadsFolder')}/${fileName}`, 'binary')
      .should('eq', expected)
  })
  cy.location('pathname').should('eq', pathname)
  assertYekOnlySession()
}

function openPeriodEditor(id: number) {
  cy.intercept('GET', `**${API}/laillistamispaiva`).as('licensing')
  cy.intercept('GET', `**${API}/ensimmainen-tyoskentelyjakso`).as('firstPeriod')
  cy.visit(`/yektyoskentelyjaksot/${id}/muokkaus`)
  cy.wait('@licensing').its('response.statusCode').should('eq', 200)
  cy.wait('@firstPeriod').its('response.statusCode').should('eq', 200)
  cy.get('input[type="file"]').should('have.length', 1).and('not.be.disabled')
}

function savePeriod(id: number) {
  cy.intercept('PUT', `**${API}/tyoskentelyjaksot`).as('savePeriod')
  cy.contains('button', 'Tallenna').click()
  cy.wait('@savePeriod').its('response.statusCode').should('eq', 200)
  cy.location('pathname').should('eq', `/yektyoskentelyjaksot/${id}`)
}

describe('YEK-only: asiakirjojen avaaminen ja lataaminen', () => {
  let databaseVerified = false

  before(() => {
    // The local replica currently uses 9060/15432. This destructive seed/reset
    // scenario is deliberately limited to the separate Docker E2E stack.
    const baseUrl = new URL(Cypress.config('baseUrl') as string)
    expect(['localhost', '127.0.0.1']).to.include(baseUrl.hostname)
    expect(baseUrl.port, 'use the isolated E2E frontend, not the replica').to.eq('8080')
    cy.task('db:assertLocalYekDocumentDatabase').then(() => {
      databaseVerified = true
    })
  })

  after(() => {
    if (databaseVerified) {
      cy.resetErikoistuvaE2eState()
    }
  })

  beforeEach(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
    cy.logout()
    // Convert, rather than add, a study right: keeping a specialist study right
    // could restore ROLE_ERIKOISTUVA_LAAKARI during the next SAML login.
    cy.task('db:seedOpintooikeus', {
      email: E2E_ERIKOISTUVA_EMAIL,
      opintoOikeus: { ...yekOpintooikeus },
      updateCurrent: true,
    })
    cy.task('db:prepareYekOnlyDocumentUser')
    cy.clearAllCookies()
    cy.clearAllLocalStorage()
    cy.loginWithSuomifi(SSN_ERIKOISTUVA)
    assertYekOnlySession()
    cy.on('window:before:load', (win) => {
      // Only prevent a new tab; document API requests remain real.
      cy.stub(win, 'open').as('openDocument')
    })
  })

  it('avaa ja lataa tallennetun asiakirjan Asiakirjat-sivulta', () => {
    const fileName = `yek-document-${Date.now()}.pdf`
    cy.visit('/yekasiakirjat')
    cy.contains('h1', 'Asiakirjat').should('be.visible')
    cy.intercept('POST', `**${API}/asiakirjat`).as('uploadDocument')
    cy.get('input[type="file"]').selectFile({
      contents: 'cypress/fixtures/test.pdf',
      fileName,
      mimeType: 'application/pdf',
    }, { force: true })
    cy.wait('@uploadDocument').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      const document = response?.body[0]
      expect(document.id).to.be.a('number')
      expect(document.nimi).to.eq(fileName)
      assertSpecialistRouteForbidden(document.id)
      // Reload to exercise a persisted document, not a browser-local File.
      cy.visit('/yekasiakirjat')
      assertPreviewAndDownload(document.id, fileName, '/yekasiakirjat')
    })
  })

  it('avaa ja lataa liitteen jakson katselussa, muokkauksessa ja tallennuksen jälkeen', () => {
    const fileName = `yek-period-${Date.now()}.pdf`
    cy.task<{ tyoskentelyjaksoId: number }>('db:seedTyoskentelyjakso', {
      email: E2E_ERIKOISTUVA_EMAIL,
    }).then(({ tyoskentelyjaksoId: id }) => {
      openPeriodEditor(id)
      cy.get('input[type="file"]').selectFile({
        contents: 'cypress/fixtures/test.pdf',
        fileName,
        mimeType: 'application/pdf',
      }, { force: true })
      savePeriod(id)

      cy.request(`${API}/tyoskentelyjaksot/${id}`).then(({ body }) => {
        const document = body.asiakirjat.find((item: { nimi: string }) => item.nimi === fileName)
        expect(document, 'saved work-period attachment').to.exist
        expect(document.id).to.be.a('number')
        assertSpecialistRouteForbidden(document.id)
        assertPreviewAndDownload(document.id, fileName, `/yektyoskentelyjaksot/${id}`)

        openPeriodEditor(id)
        assertPreviewAndDownload(document.id, fileName, `/yektyoskentelyjaksot/${id}/muokkaus`)
        savePeriod(id)
        assertPreviewAndDownload(document.id, fileName, `/yektyoskentelyjaksot/${id}`)
      })
    })
  })
})

