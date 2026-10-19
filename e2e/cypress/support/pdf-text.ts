import { E2E_ERIKOISTUVA_EMAIL, VASTUUHENKILO_EMAIL } from './commands/credentials'

export const HEART = '💗'
export const PDF_TEXT_ERROR = 'error.dataillegal.pdf-tiedostossa-tukemattomia-merkkeja'
export const REVIEW_API = '/api/erikoistuva-laakari/koejakso/vastuuhenkilonarvio'
export const REVIEW_CONTACTS = {
  erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
  erikoistuvanPuhelinnumero: '+358401234567'
}
export const RESPONSIBLE_CONTACTS = {
  vastuuhenkilonSahkoposti: VASTUUHENKILO_EMAIL,
  vastuuhenkilonPuhelinnumero: '+358401234569'
}

type Problem = {
  message: string
  field: string
  unsupportedCharacters: string[]
  pdfSource: string
}

export function expectPdfTextProblem(
  status: number | undefined,
  body: Problem,
  field: string,
  source: string,
  characters: string[] = ['💗 (U+1F497)']
) {
  expect(status).to.eq(400)
  expect(body).to.include({
    message: PDF_TEXT_ERROR,
    field,
    pdfSource: source
  })
  expect(body.unsupportedCharacters).to.deep.eq(characters)
}

export function expectPdfTextToast(label: string) {
  cy.contains('.toast-body', 'sisältää merkkejä')
    .should('be.visible')
    .and('contain.text', label)
    .and('contain.text', '💗 (U+1F497)')
    .and('not.contain.text', '{field}')
    .and('not.contain.text', '{unsupportedCharacters}')
}

export function dismissPdfTextToast() {
  cy.contains('.toast-body', 'sisältää merkkejä').closest('.toast').find('button.close').click()
}

export function pdfTextState() {
  return cy.task<Record<string, unknown>>('db:pdfTextState', {
    email: E2E_ERIKOISTUVA_EMAIL,
    kayttajaIds: ['kouluttajaId', 'vastuuhenkiloId', 'virkailijaId']
      .map((key) => Number(Cypress.env(key)))
      .filter((id) => Number.isFinite(id) && id > 0)
  })
}

export function expectPdfTextStateUnchanged(alias = 'beforePdfTextFailure') {
  cy.get<Record<string, unknown>>(`@${alias}`).then((before) => {
    pdfTextState().should('deep.equal', before)
  })
}

export function createReview() {
  return cy
    .apiRequest({
      method: 'POST',
      url: REVIEW_API,
      form: true,
      body: { vastuuhenkilonArvioJson: JSON.stringify(REVIEW_CONTACTS) }
    })
    .then(({ status, body }) => {
      expect(status).to.eq(201)
      expect(body.id).to.be.a('number')
      return body.id as number
    })
}
