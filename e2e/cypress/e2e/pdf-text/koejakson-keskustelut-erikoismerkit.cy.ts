import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL } from '../../support/commands/credentials'
import {
  HEART,
  expectPdfTextProblem,
  expectPdfTextStateUnchanged,
  pdfTextState
} from '../../support/pdf-text'

const cases = [
  {
    form: 'aloituskeskustelu',
    setup: 'db:ensureKoulutussopimusHyvaksytty',
    fields: [
      {
        property: 'koejaksonSuorituspaikka',
        field: 'koejakson-suorituspaikka'
      },
      {
        property: 'koejaksonToinenSuorituspaikka',
        field: 'koejakson-toinen-suorituspaikka'
      },
      {
        property: 'koejaksonOsaamistavoitteet',
        field: 'koejakso-osaamistavoitteet'
      }
    ]
  },
  {
    form: 'valiarviointi',
    setup: 'db:ensureAloituskeskusteluHyvaksytty',
    fields: [
      { property: 'vahvuudet', field: 'vahvuudet' },
      {
        property: 'kehittamistoimenpiteet',
        field: 'selvitys-kehittamistoimenpiteista'
      },
      { property: 'muuKategoria', field: 'muu' }
    ]
  },
  {
    form: 'loppukeskustelu',
    setup: 'db:ensureValiarviointiHyvaksytty',
    fields: [{ property: 'jatkotoimenpiteet', field: 'selvitys-jatkotoimista' }]
  }
] as const

describe('Koejakson keskustelujen PDF-erikoismerkit', () => {
  before(() => cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true }))
  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
  })
  after(() => cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL }))

  cases.forEach(({ form, setup, fields }) => {
    it(`${form}: hylkää tukemattomat kentät ja tallentaa korjatun tekstin`, () => {
      cy.task(setup, {
        erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
        kouluttajaEmail: KOULUTTAJA_EMAIL
      })
      cy.apiRequest({
        method: 'GET',
        url: '/api/erikoistuva-laakari/koejakso'
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body[form]).to.be.null
        const resident = body.koulutussopimus
        const end = new Date()
        end.setFullYear(end.getFullYear() + 1)
        const valid = {
          erikoistuvanNimi: resident.erikoistuvanNimi,
          erikoistuvanErikoisala: resident.erikoistuvanErikoisala,
          erikoistuvanYliopisto: resident.erikoistuvanYliopisto,
          lahikouluttaja: { id: Cypress.env('kouluttajaId') },
          lahiesimies: { id: Cypress.env('kouluttajaId') },
          ...(form === 'aloituskeskustelu'
            ? {
                erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
                lahetetty: true,
                koejaksonAlkamispaiva: new Date().toISOString().slice(0, 10),
                koejaksonPaattymispaiva: end.toISOString().slice(0, 10),
                suoritettuKokoaikatyossa: true
              }
            : form === 'valiarviointi'
            ? {
                edistyminenTavoitteidenMukaista: true
              }
            : {
                esitetaanKoejaksonHyvaksymista: false,
                koejaksonPaattymispaiva: end.toISOString().slice(0, 10)
              }),
          ...Object.fromEntries(
            fields.map(({ property }) => [property, 'Hyvä kehitys – Jyväskylä'])
          )
        }
        const url = `/api/erikoistuva-laakari/koejakso/${form}`
        pdfTextState().as('beforePdfTextFailure', { type: 'static' })
        fields.forEach(({ property, field }) => {
          cy.apiRequest({
            method: 'POST',
            url,
            failOnStatusCode: false,
            body: { ...valid, [property]: `Hyvä kehitys ${HEART}` }
          }).then(({ status, body }) =>
            expectPdfTextProblem(status, body, field, `koejakson${form}`)
          )
          expectPdfTextStateUnchanged()
        })
        cy.apiRequest({ method: 'POST', url, body: valid }).then(({ status, body }) => {
          expect(status).to.eq(201)
          expect(body.id).to.be.a('number')
          fields.forEach(({ property }) => expect(body[property]).to.eq('Hyvä kehitys – Jyväskylä'))
        })
        cy.apiRequest({
          method: 'GET',
          url: '/api/erikoistuva-laakari/koejakso'
        }).then(({ body }) => {
          fields.forEach(({ property }) =>
            expect(body[form][property]).to.eq('Hyvä kehitys – Jyväskylä')
          )
        })
      })
    })
  })
})
