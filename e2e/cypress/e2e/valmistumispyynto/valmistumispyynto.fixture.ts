import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands/credentials'

/** Earlier workflow stage only: review and approval themselves still use the real UI. */
export function submitValmistumispyyntoViaApi(traineeApi: string) {
  // cy.session restores cookies but clears the page. Establish the application origin for fetch.
  cy.visit('/')
  return cy.fixture('test.pdf', 'base64').then((certificate: string) =>
    cy.getCookie('XSRF-TOKEN').then((cookie) => {
      expect(cookie, 'authenticated XSRF cookie').not.to.eq(null)
      return cy.window().then(async (win) => {
        const form = new win.FormData()
        form.append('erikoistujanSahkoposti', E2E_ERIKOISTUVA_EMAIL)
        form.append('erikoistujanPuhelinnumero', '+358401234567')
        form.append('laillistamispaiva', '2020-01-01')
        form.append(
          'selvitysVanhentuneistaSuorituksista',
          'E2E-selvitys vanhentuneista suorituksista.'
        )
        form.append(
          'laillistamistodistus',
          Cypress.Blob.base64StringToBlob(certificate, 'application/pdf'),
          'test.pdf'
        )
        const response = await win.fetch(traineeApi, {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'X-XSRF-TOKEN': decodeURIComponent(cookie?.value ?? '') },
          body: form
        })
        expect(response.status, 'create prerequisite graduation request').to.eq(201)
        const saved = await response.json()
        expect(saved.id).to.be.a('number')
        expect(saved.erikoistujanKuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        return saved.id as number
      })
    })
  )
}
