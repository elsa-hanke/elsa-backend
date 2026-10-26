import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL
} from '../commands/credentials'
import { downloadPdfPages, expectTextInOrder } from '../pdf'
import {
  assertArchiveAbsent,
  assertArchiveDelivered,
  assertFreshArchiveStack
} from '../async-archive'
import {
  acceptRequirements,
  fillContactInformation,
  fillLicensingInformation,
  openYekGraduationRequest,
  setupYekGraduationRequest,
  submitGraduationRequest
} from '../../e2e/valmistumispyynto-yek/valmistumispyynto-yek.helpers'

const MAIN = 'main[role="main"]'
const MODAL = '#confirm-send'
const REVIEW_API = '/api/virkailija/valmistumispyynnon-tarkistus'
const APPROVAL_API = '/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta'
const NOTES = 'E2E: Tarkistettu työskentelyjaksot ja todistukset.'
const COVER_NOTE = 'E2E: Tarkistus valmis, valmistumisen voi hyväksyä.'
const SUMMARY = [
  'E2E: Koulutus ja suoritukset on tarkistettu.',
  'E2E: Valmistumisen edellytykset täyttyvät.'
]
const SUMMARY_HTML = SUMMARY.map((paragraph) => `<p>${paragraph}</p>`).join('')
const expectSummary = (html: unknown) => {
  const paragraphs = Cypress.$('<div>')
    .html(String(html))
    .find('p')
    .map((_, p) => p.textContent?.trim())
    .get()
  expect(paragraphs).to.deep.eq(SUMMARY)
}
const CHECKS = [
  'terveyskeskustyoTarkistettu',
  'yliopistosairaalanUlkopuolinenTyoTarkistettu',
  'yliopistosairaalatyoTarkistettu',
  'kokonaistyoaikaTarkistettu'
]
const DATE = /^\d{4}-\d{2}-\d{2}$/

type Graduation = {
  id: number
  opintooikeusId: number
  erikoistujanNimi: string
  erikoistujanKuittausaika: string
  tila: string | null
  arkistoitava?: boolean | null
  virkailijanKuittausaika: string | null
  virkailijanSaate: string | null
  vastuuhenkiloHyvaksyjaKuittausaika: string | null
  yhteenvetoAsiakirjaId: number | null
  liitteetAsiakirjaId: number | null
}
type Review = Record<string, unknown> & {
  valmistumispyynto: Graduation
}

const field = (label: string, root = MAIN) =>
  cy.get(root).contains('label', label).closest('.form-group')
const button = (label: string, root = MAIN) =>
  cy.get(root).contains('button', new RegExp(`^\\s*${label}\\s*$`))
const reviewButton = () => button('Hyväksy ja lähetä')
const summaryBody = () =>
  field('Opintohallinnon virkailijan yhteenveto')
    .find('iframe.tox-edit-area__iframe')
    .should('be.visible')
    .its('0.contentDocument.body')
    .should('not.be.empty')
    .then((body: HTMLBodyElement) => cy.wrap(Cypress.$(body)))
const finnishDate = (date: string) => {
  const [year, month, day] = date.split('-')
  return `${Number(day)}.${Number(month)}.${year}`
}

// Separate actor tests: setup requests advance earlier stages only. Every review/approval
// protected by these tests goes through the actual form and an unstubbed backend response.
export function valmistumispyynnonHyvaksynta(archiveDegree?: 'EL' | 'EHL' | 'YEK') {
  const asyncArchive = archiveDegree !== undefined
  for (const yek of asyncArchive ? [archiveDegree === 'YEK'] : [false, true]) {
    describe(`${
      archiveDegree ?? (yek ? 'YEK' : 'EL')
    } valmistumispyynnön tarkistus ja hyväksyntä käyttöliittymässä`, () => {
      const traineeApi = `/api/${
        yek ? 'yek-koulutettava' : 'erikoistuva-laakari'
      }/valmistumispyynto`
      const traineePage = yek ? '/yekvalmistumispyynto' : '/valmistumispyynto'
      const reviewPage = (id: number) => `/valmistumispyynnon-tarkistus${yek ? '-yek' : ''}/${id}`
      const approvalPage = (id: number) =>
        `/valmistumispyynnon-hyvaksynta${yek ? '-yek' : ''}/${id}`
      const approveLabel = yek ? 'Hyväksy valmistumispyyntö' : 'Hyväksy'
      const summaryLabel = yek
        ? 'YEK-koulutuksen valmistumisen yhteenveto'
        : 'Erikoistumiskoulutuksen valmistumisen yhteenveto'
      const checkFields = yek ? CHECKS : [...CHECKS, 'teoriakoulutusTarkistettu']
      let original: Graduation
      let traineeCookies: Cypress.Cookie[]

      const loginClerk = () =>
        cy
          .task<string | null>('tokens:get', 'virkailijaToken', { log: false })
          .then((token) => cy.loginAsVirkailija(token ?? undefined))
      const loginApprover = () =>
        cy
          .task<string | null>('tokens:get', 'vastuuhenkiloToken', { log: false })
          .then((token) => cy.loginAsVastuuhenkilo(token ?? undefined))
      const sameRequest = (saved: Graduation) => {
        expect(saved).to.include({
          id: original.id,
          opintooikeusId: original.opintooikeusId,
          erikoistujanNimi: original.erikoistujanNimi,
          erikoistujanKuittausaika: original.erikoistujanKuittausaika
        })
      }
      // PUT review responses contain persisted fields; role-specific GETs compute tila.
      const reviewSaved = (saved: Graduation) => {
        sameRequest(saved)
        expect(saved.virkailijanKuittausaika).to.match(DATE)
        expect(saved).to.include({
          virkailijanSaate: COVER_NOTE,
          vastuuhenkiloHyvaksyjaKuittausaika: null,
          yhteenvetoAsiakirjaId: null,
          liitteetAsiakirjaId: null
        })
      }
      const pendingApproval = (saved: Graduation) => {
        reviewSaved(saved)
        expect(saved.tila).to.eq('ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA')
      }
      // tila and archiving eligibility are computed for the actor reading the record.
      const persistedRequest = ({ tila, arkistoitava, ...persisted }: Graduation) => persisted
      const openActor = (path: string, api: string, alias: string) => {
        cy.intercept({ method: 'GET', url: `**${api}/${original.id}`, times: 1 }).as(alias)
        cy.visit(path)
        return cy.wait(`@${alias}`).then(({ response }) => {
          expect(response?.statusCode).to.eq(200)
          return response?.body as Review
        })
      }
      const traineeView = (expected: Graduation) => {
        // YEK setup logs out of the original cached EL session. Restore the live
        // trainee session captured after setup instead of reusing that invalid cache.
        cy.clearAllCookies()
        cy.clearAllLocalStorage()
        cy.clearAllSessionStorage()
        cy.then(() => {
          expect(traineeCookies, 'authenticated trainee cookies captured during setup').not.to.be
            .empty
          for (const cookie of traineeCookies) {
            cy.setCookie(cookie.name, cookie.value, {
              domain: cookie.domain,
              path: cookie.path,
              secure: cookie.secure,
              httpOnly: cookie.httpOnly,
              sameSite: cookie.sameSite,
              expiry: cookie.expiry
            })
          }
        })
        cy.apiRequest({ method: 'GET', url: '/api/kayttaja' }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.activeAuthority).to.eq(
            yek ? 'ROLE_YEK_KOULUTETTAVA' : 'ROLE_ERIKOISTUVA_LAAKARI'
          )
        })
        cy.intercept({ method: 'GET', url: `**${traineeApi}`, times: 1 }).as('traineeResult')
        cy.visit(traineePage)
        cy.wait('@traineeResult').then(({ response }) => {
          expect(response?.statusCode).to.eq(200)
          sameRequest(response?.body)
          expect(response?.body).to.include({
            tila: expected.tila,
            virkailijanKuittausaika: expected.virkailijanKuittausaika,
            vastuuhenkiloHyvaksyjaKuittausaika: expected.vastuuhenkiloHyvaksyjaKuittausaika,
            yhteenvetoAsiakirjaId: expected.yhteenvetoAsiakirjaId,
            liitteetAsiakirjaId: expected.liitteetAsiakirjaId
          })
        })
        cy.get(MAIN).contains('Opintohallinto on tarkistanut').should('be.visible')
        cy.get(MAIN).should('not.contain.text', NOTES)
      }
      const approved = (saved: Graduation) => {
        sameRequest(saved)
        expect(saved.tila).to.eq('HYVAKSYTTY')
        expect(saved.virkailijanKuittausaika).to.eq(original.virkailijanKuittausaika)
        expect(saved.vastuuhenkiloHyvaksyjaKuittausaika).to.match(DATE)
        expect(saved.yhteenvetoAsiakirjaId).to.be.a('number').and.be.greaterThan(0)
        expect(saved.liitteetAsiakirjaId).to.be.a('number').and.be.greaterThan(0)
        expect(saved.liitteetAsiakirjaId).not.to.eq(saved.yhteenvetoAsiakirjaId)
      }

      beforeEach(() => {
        if (asyncArchive) assertFreshArchiveStack()
        if (yek) {
          setupYekGraduationRequest()
          openYekGraduationRequest()
          acceptRequirements()
          fillContactInformation('+358401234567')
          fillLicensingInformation()
          submitGraduationRequest('POST', 'newYekRequest')
        } else {
          cy.prepareKoejaksoE2e({
            cleanupSupportUsers: true,
            seedVirkailija: true,
            storeTokens: true
          })
          if (archiveDegree === 'EHL') {
            cy.task(
              'archive:prepareEhl',
              { email: E2E_ERIKOISTUVA_EMAIL, approverEmail: VASTUUHENKILO_EMAIL },
              { log: false }
            )
          }
          cy.task('db:ensureLoppukeskusteluHyvaksytty', {
            erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
            kouluttajaEmail: KOULUTTAJA_EMAIL
          })
          cy.submitValmistumispyyntoViaUi()
        }
        cy.apiRequest({ method: 'GET', url: traineeApi }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.id).to.be.a('number')
          original = body
          expect(original.virkailijanKuittausaika).to.eq(null)
          expect(original.vastuuhenkiloHyvaksyjaKuittausaika).to.eq(null)
        })
        cy.getCookies().then((cookies) => {
          traineeCookies = cookies
        })
        if (!yek) {
          loginApprover()
          cy.then(() =>
            cy.apiRequest({
              method: 'PUT',
              url: `/api/vastuuhenkilo/valmistumispyynnon-arviointi/${original.id}`,
              body: { osaaminenRiittavaValmistumiseen: true, korjausehdotus: null }
            })
          )
            .its('body.tila')
            .should('eq', 'ODOTTAA_VIRKAILIJAN_TARKASTUSTA')
        }
        loginClerk()
      })

      afterEach(() => {
        if (asyncArchive) return
        cy.task('db:cleanupValmistumispyynto', { email: E2E_ERIKOISTUVA_EMAIL })
      })

      after(() => {
        if (asyncArchive) return
        cy.resetKoejaksoE2eState()
      })

      if (!asyncArchive)
        it('virkailija tarkistaa lomakkeen ja lähettää sen vastuuhenkilölle vasta vahvistuksesta', () => {
          let before: Review
          let saved: Review
          let sentFields: Record<string, FormDataEntryValue> | undefined
          cy.then(() => {
            cy.intercept('PUT', `**${REVIEW_API}/${original.id}`).as('reviewPut')
            cy.intercept({ method: 'GET', url: `**${REVIEW_API}/${original.id}`, times: 1 }).as(
              'reviewBefore'
            )
            cy.visit(reviewPage(original.id), {
              onBeforeLoad(win) {
                // Inspect original FormData; Cypress may decode multipart bytes in request.body.
                const send = win.XMLHttpRequest.prototype.send
                cy.stub(win.XMLHttpRequest.prototype, 'send').callsFake(function (
                  this: XMLHttpRequest,
                  body?: Document | XMLHttpRequestBodyInit | null
                ) {
                  if (body instanceof win.FormData && body.has('terveyskeskustyoTarkistettu')) {
                    sentFields = {}
                    body.forEach((value, key) => {
                      sentFields![key] = value
                    })
                  }
                  return send.call(this, body)
                })
              }
            })
          })
          cy.wait('@reviewBefore').then(({ response }) => {
            expect(response?.statusCode).to.eq(200)
            before = response?.body
            sameRequest(before.valmistumispyynto)
            expect(before.valmistumispyynto.tila).to.eq('ODOTTAA_VIRKAILIJAN_TARKASTUSTA')
          })
          cy.get(MAIN)
            .contains('h1', yek ? /^YEK valmistumispyyntö$/ : /^Valmistumispyyntö$/)
            .should('be.visible')
          reviewButton().click()
          cy.get('body').find(`${MODAL}:visible`).should('have.length', 0)
          cy.get(MAIN)
            .find('form .form-group input[type="checkbox"].is-invalid')
            .should('have.length', checkFields.length)
          cy.get('@reviewPut.all').should('have.length', 0)

          // Only the required review groups: the optional previous-degree checkboxes stay untouched.
          cy.get(MAIN)
            .find('form .form-group input[type="checkbox"]')
            .should('have.length', checkFields.length)
            .each((checkbox) => cy.wrap(checkbox).check({ force: true }))
          field('Kommentit muille virkailijoille').find('textarea').type(NOTES)
          if (!yek) {
            summaryBody().should('have.attr', 'contenteditable', 'true')
            summaryBody().type(SUMMARY.join('{enter}')).blur()
          }
          reviewButton().click()
          cy.get(MODAL).should('be.visible').and('contain.text', 'Vahvista lomakkeen lähetys')
          field('Lisätiedot vastuuhenkilölle', MODAL).find('textarea').type(COVER_NOTE)
          cy.get('@reviewPut.all').should('have.length', 0)
          button('Peruuta', MODAL).click()
          cy.get('body').find(`${MODAL}:visible`).should('have.length', 0)
          field('Kommentit muille virkailijoille').find('textarea').should('have.value', NOTES)
          cy.get('@reviewPut.all').should('have.length', 0)
          cy.then(() => cy.apiRequest({ method: 'GET', url: `${REVIEW_API}/${original.id}` })).then(
            ({ body }) => {
              expect(body.valmistumispyynto).to.deep.eq(before.valmistumispyynto)
              for (const key of [
                ...checkFields,
                'kommentitVirkailijoille',
                'virkailijanYhteenveto'
              ]) {
                expect(body[key], `unchanged ${key} after cancellation`).to.deep.eq(before[key])
              }
            }
          )
          reviewButton().click()
          field('Lisätiedot vastuuhenkilölle', MODAL)
            .find('textarea')
            .should('have.value', COVER_NOTE)
          button('Hyväksy ja lähetä', MODAL).click()
          cy.wait('@reviewPut').then(({ request, response }) => {
            expect(response?.statusCode).to.eq(200)
            expect(request.headers['content-type']).to.include('multipart/form-data')
            expect(sentFields).to.include({
              id: String(original.id),
              keskenerainen: 'false',
              kommentitVirkailijoille: NOTES,
              lisatiedotVastuuhenkilolle: COVER_NOTE
            })
            for (const key of checkFields) expect(sentFields?.[key], key).to.eq('true')
            saved = response?.body
            for (const key of checkFields) expect(saved[key], key).to.eq(true)
            expect(saved.kommentitVirkailijoille).to.eq(NOTES)
            if (!yek) {
              expectSummary(sentFields?.virkailijanYhteenveto)
              expectSummary(saved.virkailijanYhteenveto)
            }
            reviewSaved(saved.valmistumispyynto)
          })
          cy.location('pathname').should('eq', '/valmistumispyynnot')
          cy.get('@reviewPut.all').should('have.length', 1)
          cy.then(() => openActor(reviewPage(original.id), REVIEW_API, 'reviewReopened')).then(
            (body) => {
              pendingApproval(body.valmistumispyynto)
              expect(persistedRequest(body.valmistumispyynto)).to.deep.eq(
                persistedRequest(saved.valmistumispyynto)
              )
              for (const key of [
                ...checkFields,
                'kommentitVirkailijoille',
                'virkailijanYhteenveto'
              ])
                expect(body[key], key).to.deep.eq(saved[key])
              saved = body
            }
          )
          reviewButton().should('not.exist')
          cy.get(MAIN).contains(NOTES).should('be.visible')
          if (!yek)
            field('Opintohallinnon virkailijan yhteenveto')
              .find('.editor-readonly p')
              .should('have.length', 2)
              .each((paragraph, index) => expect(paragraph.text()).to.eq(SUMMARY[index]))
          loginApprover()
          cy.then(() => openActor(approvalPage(original.id), APPROVAL_API, 'receivedReview')).then(
            (body) => pendingApproval(body.valmistumispyynto)
          )
          cy.get(MAIN).contains(COVER_NOTE).should('be.visible')
          cy.get(MAIN).should('not.contain.text', NOTES)
          button(approveLabel).should('be.visible').and('not.be.disabled')
          cy.then(() => traineeView(saved.valmistumispyynto))
          cy.get(MAIN)
            .contains('Vastuuhenkilö ei ole vielä hyväksynyt valmistumispyyntöä')
            .should('be.visible')
        })

      const approveThroughUi = (failure?: 'network' | 'http500') => {
        const submittedBodies: unknown[] = []
        let saved: Graduation
        let expectedContacts: { sahkoposti: string; puhelinnumero: string | null }
        const university = yek ? 'ITA_SUOMEN_YLIOPISTO' : 'HELSINGIN_YLIOPISTO'
        if (asyncArchive)
          cy.then(() => assertArchiveAbsent('valmistuminen', university, original.id))
        // Clerk review is setup in this actor-specific test; the preceding test protects its UI.
        cy.then(() =>
          cy.apiRequest({
            method: 'PUT',
            url: `${REVIEW_API}/${original.id}`,
            form: true,
            body: {
              ...Object.fromEntries(checkFields.map((key) => [key, true])),
              keskenerainen: false,
              kommentitVirkailijoille: NOTES,
              lisatiedotVastuuhenkilolle: COVER_NOTE,
              ...(yek ? {} : { virkailijanYhteenveto: SUMMARY_HTML })
            }
          })
        ).then(({ status, body }) => {
          expect(status).to.eq(200)
          reviewSaved(body.valmistumispyynto)
          original = body.valmistumispyynto
        })
        loginApprover()
        cy.apiRequest({ method: 'GET', url: '/api/kayttaja' }).then(({ body }) => {
          expectedContacts = { sahkoposti: body.email, puhelinnumero: body.phoneNumber }
        })
        cy.then(() => {
          cy.intercept('PUT', `**${APPROVAL_API}/${original.id}`, (request) => {
            submittedBodies.push(request.body)
            if (failure && submittedBodies.length === 1) {
              // Fail before the request reaches the server. The retry must use the real backend.
              request.alias = 'failedApproval'
              if (failure === 'network') request.destroy()
              else
                request.reply({
                  statusCode: 500,
                  headers: { 'content-type': 'application/problem+json' },
                  body: { status: 500, title: 'Internal Server Error' }
                })
            } else {
              request.alias = 'successfulApproval'
            }
          }).as('approvalPut')
          openActor(approvalPage(original.id), APPROVAL_API, 'approvalBefore').then((body) => {
            pendingApproval(body.valmistumispyynto)
            expect(persistedRequest(body.valmistumispyynto)).to.deep.eq(persistedRequest(original))
            original = body.valmistumispyynto
          })
        })
        cy.get(MAIN).contains(COVER_NOTE).should('be.visible')
        if (!yek) {
          field('Sähköpostiosoite').find('input').clear().type(VASTUUHENKILO_EMAIL)
          field('Matkapuhelinnumero').find('input').clear().type('+358501234567')
          cy.then(() => {
            expectedContacts = { sahkoposti: VASTUUHENKILO_EMAIL, puhelinnumero: '+358501234567' }
          })
        }
        button(approveLabel).should('be.visible').and('not.be.disabled').click()
        cy.get(MODAL)
          .should('be.visible')
          .and('contain.text', 'Vahvista lomakkeen lähetys')
          .and(
            'contain.text',
            yek
              ? 'Hyväksymällä vahvistat koulutettavan valmistumisen.'
              : 'Vahvistuksen jälkeen valmistumispyyntö on hyväksytty kaikkien osapuolien toimesta.'
          )
        cy.get('@approvalPut.all').should('have.length', 0)
        button('Peruuta', MODAL).click()
        cy.get('body').find(`${MODAL}:visible`).should('have.length', 0)
        cy.get('@approvalPut.all').should('have.length', 0)
        cy.then(() => cy.apiRequest({ method: 'GET', url: `${APPROVAL_API}/${original.id}` })).then(
          ({ body }) => expect(body.valmistumispyynto).to.deep.eq(original)
        )
        if (!yek) field('Matkapuhelinnumero').find('input').should('have.value', '+358501234567')
        if (failure) {
          button(approveLabel).click()
          button(approveLabel, MODAL).click()
          cy.wait('@failedApproval').then(({ request, response, error }) => {
            expect(request.body).to.deep.eq({ id: original.id, ...expectedContacts })
            if (failure === 'network')
              expect(error, 'network failure without HTTP response').to.exist
            else expect(response?.statusCode).to.eq(500)
          })
          cy.contains('.toast-body', 'Valmistumispyynnön hyväksynnän lähetys epäonnistui')
            .should('be.visible')
            .closest('.toast')
            .find('button.close')
            .click()
          cy.then(() => cy.location('pathname').should('eq', approvalPage(original.id)))
          cy.get('body').find(`${MODAL}:visible`).should('have.length', 0)
          button(approveLabel).should('be.visible').and('not.be.disabled')
          cy.get(MAIN).contains(COVER_NOTE).should('be.visible')
          if (!yek) {
            field('Sähköpostiosoite').find('input').should('have.value', VASTUUHENKILO_EMAIL)
            field('Matkapuhelinnumero').find('input').should('have.value', '+358501234567')
          }
          // A fresh read must still show the pending request and no generated approval documents.
          cy.then(() =>
            cy.apiRequest({ method: 'GET', url: `${APPROVAL_API}/${original.id}` })
          ).then(({ status, body }) => {
            expect(status).to.eq(200)
            pendingApproval(body.valmistumispyynto)
            expect(body.valmistumispyynto).to.deep.eq(original)
            expect(submittedBodies).to.have.length(1)
          })
          cy.apiRequest({ method: 'GET', url: '/api/kayttaja' })
            .its('body.activeAuthority')
            .should('eq', 'ROLE_VASTUUHENKILO')
        }
        button(approveLabel).click()
        button(approveLabel, MODAL).click()
        cy.wait('@successfulApproval', { responseTimeout: 120000 }).then(
          ({ request, response }) => {
            expect(request.body).to.deep.eq({ id: original.id, ...expectedContacts })
            expect(response?.statusCode).to.eq(200)
            saved = response?.body.valmistumispyynto
            approved(saved)
            if (asyncArchive)
              assertArchiveDelivered({
                prefix: 'valmistuminen',
                university,
                caseId: original.id,
                caseType: 'VALMISTUMINEN',
                types: yek
                  ? ['YHTEENVETO', 'LIITE']
                  : ['YHTEENVETO', 'LIITE', 'LAILLISTAMISTODISTUS'],
                metadata: {
                  valmistumispyyntoId: original.id,
                  opintooikeusId: original.opintooikeusId,
                  yek,
                  approver: 'Mia Ålands',
                  reviewer: 'Daniel Siekkinen',
                  ...(archiveDegree === 'EHL' ? { specialtyId: 54 } : {}),
                  approvalDate: saved.vastuuhenkiloHyvaksyjaKuittausaika,
                  reviewDate: saved.virkailijanKuittausaika
                },
                sourceIds: {
                  YHTEENVETO: saved.yhteenvetoAsiakirjaId!,
                  LIITE: saved.liitteetAsiakirjaId!
                }
              })
          }
        )
        cy.location('pathname').should('eq', '/valmistumispyynnot')
        cy.get('@approvalPut.all').should('have.length', failure ? 2 : 1)
        cy.then(() => {
          const expected = { id: original.id, ...expectedContacts }
          expect(submittedBodies).to.deep.eq(failure ? [expected, expected] : [expected])
        })
        cy.then(() => openActor(approvalPage(original.id), APPROVAL_API, 'approvalReopened')).then(
          (body) => {
            approved(body.valmistumispyynto)
            expect(persistedRequest(body.valmistumispyynto)).to.deep.eq(persistedRequest(saved))
          }
        )
        cy.get(MAIN)
          .contains('.alert-success', 'Valmistumispyyntö on hyväksytty vastuuhenkilön toimesta.')
          .should('be.visible')
        button(approveLabel).should('not.exist')
        cy.get(MAIN).contains('button', 'Palauta').should('not.exist')
        cy.get(MAIN).contains('button', summaryLabel).should('be.visible')
        cy.get(MAIN).contains('button', 'Valmistumispyynnön liitteet').should('be.visible')
        cy.then(() => {
          downloadPdfPages(
            `/api/vastuuhenkilo/valmistumispyynto/${original.id}/asiakirja/${saved.yhteenvetoAsiakirjaId}`
          ).then((pages) => {
            expect(pages.length).to.be.greaterThan(0)
            expectTextInOrder(
              pages.join(' '),
              summaryLabel,
              yek ? 'Koulutettava lääkäri' : 'Erikoistuva lääkäri',
              'Työskentelyjaksot',
              'Muut tarkistukset',
              'Tarkistanut'
            )
            if (!yek) for (const paragraph of SUMMARY) expect(pages.join(' ')).to.include(paragraph)
          })
          downloadPdfPages(
            `/api/vastuuhenkilo/valmistumispyynto/${original.id}/asiakirja/${saved.liitteetAsiakirjaId}`
          )
            .its('length')
            .should('be.greaterThan', 0)
          traineeView(saved)
        })
        cy.then(() =>
          cy
            .get(MAIN)
            .contains(
              `Vastuuhenkilö on hyväksynyt valmistumispyynnön ${finnishDate(
                saved.vastuuhenkiloHyvaksyjaKuittausaika!
              )}`
            )
            .should('be.visible')
        )
        cy.get(MAIN).contains('button', 'Lähetä').should('not.exist')
        // Download through both actual trainee buttons as well, protecting their URL/ID mapping.
        for (const summary of [true, false]) {
          cy.then(() => {
            const id = summary ? saved.yhteenvetoAsiakirjaId : saved.liitteetAsiakirjaId
            cy.intercept(
              'GET',
              `**/api/${yek ? 'yek-koulutettava' : 'erikoistuva-laakari'}/asiakirjat/${id}`
            ).as('traineePdf')
            cy.get(MAIN)
              .contains('button', summary ? summaryLabel : 'Valmistumispyynnön liitteet')
              .click()
            cy.wait('@traineePdf').then(({ response }) => {
              expect(response?.statusCode).to.eq(200)
              expect(response?.headers['content-type']).to.include('application/pdf')
            })
          })
        }
      }

      it('vastuuhenkilö hyväksyy vahvistuksesta ja tallennettu päätös sekä PDF:t näkyvät myös koulutettavalle', () => {
        approveThroughUi()
      })

      for (const failure of asyncArchive ? [] : (['network', 'http500'] as const)) {
        it(`${
          failure === 'network' ? 'verkkovirhe' : 'HTTP 500'
        } säilyttää hyväksyntälomakkeen ja sallii yhden onnistuneen uudelleenyrityksen`, () => {
          approveThroughUi(failure)
        })
      }
    })
  }
}
