import {
  E2E_ERIKOISTUVA_EMAIL,
  VASTUUHENKILO_EMAIL,
  VIRKAILIJA_EMAIL
} from '../../support/commands/credentials'
import { setupYekGraduationRequest } from '../valmistumispyynto-yek/valmistumispyynto-yek.helpers'
import { downloadPdfPages } from '../../support/pdf'

const MAIN = 'main[role="main"]'
const NOTE = 'E2E: Todistukset ja koulutusjakson tiedot tarkistettu.'
const CORRECTION = 'E2E: Lisää tarkennettu koulutustodistus ja lähetä uudelleen.'
const CORRECTED_FILE = 'e2e-tk-tarkennettu-todistus.pdf'
const FILES = ['e2e-tk-tyotodistus.pdf', 'e2e-tk-koulutustodistus.pdf', 'e2e-tk-arviointi.pdf']
const DATE = /^\d{4}-\d{2}-\d{2}$/
const REVIEW = 'ODOTTAA_VIRKAILIJAN_TARKISTUSTA'
const APPROVAL = 'ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA'
const RETURNED = 'PALAUTETTU_KORJATTAVAKSI'
const APPROVED = 'HYVAKSYTTY'
type Actor = 'virkailija' | 'vastuuhenkilo'
type RecordData = {
  id: number
  opintooikeusId: number
  tila: string
  erikoistuvanNimi: string
  terveyskeskuskoulutusjaksonKesto: number
  laillistamispaiva: string
  laillistamispaivanLiitteenNimi: string
  virkailijanKuittausaika: string | null
  vastuuhenkilonKuittausaika: string | null
  virkailijanKorjausehdotus: string | null
  vastuuhenkilonKorjausehdotus: string | null
  lisatiedotVirkailijalta: string | null
  tyoskentelyjaksot: Array<{
    id: number
    alkamispaiva: string
    paattymispaiva: string
    osaaikaprosentti: number
    tyoskentelypaikka: { nimi: string; tyyppi: string }
    asiakirjat: Array<{ id: number; nimi: string; tyyppi: string }>
  }>
}
type Fixture = { opintooikeusId: number; tyoskentelyjaksoId: number }
const button = (label: string, root = MAIN) =>
  cy.get(root).contains('button', new RegExp(`^\\s*${label}\\s*$`))
const closeModal = (id: string) => {
  button('Peruuta', id).click()
  cy.get('body').find(`${id}:visible`).should('have.length', 0)
}

for (const yek of [false, true]) {
  describe(`${yek ? 'YEK' : 'EL'} terveyskeskuskoulutusjakson hyväksyntä käyttöliittymässä`, () => {
    const role = yek ? 'yek-koulutettava' : 'erikoistuva-laakari'
    const traineeApi = `/api/${role}/tyoskentelyjaksot/terveyskeskuskoulutusjakso`
    const traineeSubmit = `/api/${role}/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta`
    const traineePage = yek
      ? '/yektyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta-yek'
      : '/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta'
    let fixture: Fixture
    let original: RecordData
    let traineeCookies: Cypress.Cookie[]
    let corrected: boolean
    let sentForms: Array<Record<string, FormDataEntryValue>>
    const getApi = (actor: Actor) => `/api/${actor}/terveyskeskuskoulutusjakso/${original.id}`
    const putApi = (actor: Actor) =>
      `/api/${actor}/terveyskeskuskoulutusjakson-hyvaksynta/${original.id}`
    const actorPage = (actor: Actor) =>
      `/terveyskeskuskoulutusjaksot/terveyskeskuskoulutusjakson-${
        actor === 'virkailija' ? 'tarkistus' : 'hyvaksynta'
      }/${original.id}`
    const login = (actor: Actor) =>
      cy.task<string | null>('tokens:get', `${actor}Token`, { log: false }).then((token) => {
        if (actor === 'virkailija') cy.loginAsVirkailija(token ?? undefined)
        else cy.loginAsVastuuhenkilo(token ?? undefined)
      })
    const restoreTrainee = () => {
      cy.clearAllCookies()
      cy.clearAllLocalStorage()
      cy.clearAllSessionStorage()
      cy.then(() => {
        expect(traineeCookies).not.to.be.empty
        for (const cookie of traineeCookies)
          cy.setCookie(cookie.name, cookie.value, {
            domain: cookie.domain,
            path: cookie.path,
            secure: cookie.secure,
            httpOnly: cookie.httpOnly,
            sameSite: cookie.sameSite,
            expiry: cookie.expiry
          })
      })
      cy.apiRequest({ method: 'GET', url: '/api/kayttaja' })
        .its('body.activeAuthority')
        .should('eq', yek ? 'ROLE_YEK_KOULUTETTAVA' : 'ROLE_ERIKOISTUVA_LAAKARI')
    }
    const contentPreserved = (record: RecordData) => {
      // The unsaved form DTO has no study-right ID; persisted GETs must have it.
      if (record.id != null) {
        expect(record.opintooikeusId).to.eq(fixture.opintooikeusId)
        expect(record.laillistamispaiva).to.eq('2020-01-01')
        expect(record.laillistamispaivanLiitteenNimi).to.eq('e2e-tk-laillistaminen.pdf')
      }
      expect(record.terveyskeskuskoulutusjaksonKesto).to.be.greaterThan(272.25)
      expect(record.tyoskentelyjaksot).to.have.length(1)
      const period = record.tyoskentelyjaksot[0]
      expect(period).to.include({
        id: fixture.tyoskentelyjaksoId,
        alkamispaiva: '2025-01-01',
        paattymispaiva: '2025-12-31',
        osaaikaprosentti: 100
      })
      expect(period.tyoskentelypaikka).to.include({
        nimi: 'E2E terveyskeskus',
        tyyppi: 'TERVEYSKESKUS'
      })
      expect(period.asiakirjat.map((file) => file.nimi)).to.have.members(
        corrected ? [...FILES, CORRECTED_FILE] : FILES
      )
      for (const file of period.asiakirjat) {
        expect(file.id).to.be.a('number')
        expect(file.tyyppi).to.eq('application/pdf')
      }
    }
    const savedState = (record: RecordData, state: string) => {
      contentPreserved(record)
      expect(record.id).to.eq(original.id)
      expect(record.erikoistuvanNimi).to.eq(original.erikoistuvanNimi)
      expect(record.tila).to.eq(state)
      if (state === APPROVAL || state === APPROVED)
        expect(record.virkailijanKuittausaika).to.match(DATE)
      if (state === APPROVED) expect(record.vastuuhenkilonKuittausaika).to.match(DATE)
      else expect(record.vastuuhenkilonKuittausaika).to.eq(null)
    }
    const openTrainee = (alias: string) => {
      cy.intercept({ method: 'GET', url: `**${traineeApi}`, times: 1 }).as(alias)
      cy.visit(traineePage)
      return cy.wait(`@${alias}`).then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        return response?.body as RecordData
      })
    }
    const openActor = (actor: Actor, alias: string, watchPut = false) => {
      if (watchPut) cy.intercept('PUT', `**${putApi(actor)}`).as('decisionPut')
      cy.intercept({ method: 'GET', url: `**${getApi(actor)}`, times: 1 }).as(alias)
      cy.visit(actorPage(actor), {
        onBeforeLoad(win) {
          // Observe original multipart fields while allowing the actual request to proceed.
          const send = win.XMLHttpRequest.prototype.send
          cy.stub(win.XMLHttpRequest.prototype, 'send').callsFake(function (
            this: XMLHttpRequest,
            body?: Document | XMLHttpRequestBodyInit | null
          ) {
            if (body instanceof win.FormData) {
              const fields: Record<string, FormDataEntryValue> = {}
              body.forEach((value, key) => {
                fields[key] = value
              })
              sentForms.push(fields)
            }
            return send.call(this, body)
          })
        }
      })
      return cy.wait(`@${alias}`).then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        return response?.body as RecordData
      })
    }
    const readActor = (actor: Actor, state: string) =>
      cy.apiRequest({ method: 'GET', url: getApi(actor) }).then(({ status, body }) => {
        expect(status).to.eq(200)
        savedState(body, state)
        return body as RecordData
      })
    const reviewSetup = () => {
      // Earlier-stage setup only. The separate clerk test protects this action through UI.
      cy.then(() =>
        cy.apiRequest({
          method: 'PUT',
          url: putApi('virkailija'),
          form: true,
          body: { lisatiedotVirkailijalta: NOTE }
        })
      )
        .its('status')
        .should('eq', 200)
      cy.then(() => readActor('virkailija', APPROVAL))
    }
    const submitTrainee = (method: 'POST' | 'PUT', alias: string) => {
      cy.intercept(method, `**${traineeSubmit}`).as(alias)
      button('Lähetä').click()
      cy.get('#confirm-erikoistuja').should('be.visible')
      button('Lähetä', '#confirm-erikoistuja').click()
      cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200)
      cy.location('pathname').should('eq', yek ? '/yektyoskentelyjaksot' : '/tyoskentelyjaksot')
    }

    beforeEach(() => {
      corrected = false
      sentForms = []
      cy.task('db:cleanupTerveyskeskusE2e', { email: E2E_ERIKOISTUVA_EMAIL })
      if (yek) setupYekGraduationRequest()
      else
        cy.prepareKoejaksoE2e({
          cleanupSupportUsers: true,
          seedVirkailija: true,
          storeTokens: true
        })
      cy.fixture('test.pdf', 'base64')
        .then((pdfBase64) =>
          cy.task<Fixture>('db:prepareTerveyskeskusE2e', {
            email: E2E_ERIKOISTUVA_EMAIL,
            yek,
            pdfBase64,
            vastuuhenkiloEmail: VASTUUHENKILO_EMAIL,
            virkailijaEmail: VIRKAILIJA_EMAIL
          })
        )
        .then((result) => {
          fixture = result
        })
      openTrainee('newTraineeForm').then(contentPreserved)
      // Submit licensing data through the application, keeping Hibernate's cache consistent.
      cy.get(MAIN)
        .contains('label', 'Valviran laillistamispäivä')
        .closest('.form-group')
        .find('input.date-input')
        .first()
        .clear()
        .type('01.01.2020')
        .blur()
      cy.get(MAIN)
        .contains('label', 'Laillistamispäivän liitetiedosto')
        .closest('.form-group')
        .find('input[type="file"]')
        .selectFile(
          {
            contents: 'cypress/fixtures/test.pdf',
            fileName: 'e2e-tk-laillistaminen.pdf',
            mimeType: 'application/pdf'
          },
          { force: true }
        )
      submitTrainee('POST', 'newRequest')
      openTrainee('submittedRequest').then((body) => {
        original = body
        savedState(body, REVIEW)
        expect(body.virkailijanKuittausaika).to.eq(null)
      })
      cy.getCookies().then((cookies) => {
        traineeCookies = cookies
      })
      login('virkailija')
    })

    afterEach(() => {
      cy.task('db:cleanupTerveyskeskusE2e', { email: E2E_ERIKOISTUVA_EMAIL })
      cy.resetKoejaksoE2eState()
    })

    it('virkailija lähettää tarkistuksen vasta vahvistuksesta ja säilyttää tiedot uudelleen avattaessa', () => {
      cy.then(() => openActor('virkailija', 'reviewBefore', true)).then((body) =>
        savedState(body, REVIEW)
      )
      cy.get(MAIN)
        .contains(
          'h1',
          yek
            ? /^\s*YEK terveyskeskuskoulutusjakson tarkistus\s*$/
            : /^\s*Terveyskeskuskoulutusjakson tarkistus\s*$/
        )
        .should('be.visible')
      button('Hyväksy ja lähetä').click()
      cy.get('#confirm-virkailija').should('be.visible')
      cy.get('#confirm-virkailija')
        .contains('label', 'Lisätiedot vastuuhenkilölle')
        .closest('.form-group')
        .find('textarea')
        .type(NOTE)
      cy.get('@decisionPut.all').should('have.length', 0)
      closeModal('#confirm-virkailija')
      cy.then(() => readActor('virkailija', REVIEW)).then((body) =>
        expect(body.lisatiedotVirkailijalta).to.eq(null)
      )
      button('Hyväksy ja lähetä').click()
      cy.get('#confirm-virkailija')
        .contains('label', 'Lisätiedot vastuuhenkilölle')
        .closest('.form-group')
        .find('textarea')
        .should('have.value', NOTE)
      button('Hyväksy ja lähetä', '#confirm-virkailija').click()
      cy.wait('@decisionPut').then(({ request, response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(request.headers['content-type']).to.include('multipart/form-data')
        expect(sentForms).to.deep.eq([{ lisatiedotVirkailijalta: NOTE }])
      })
      cy.location('pathname').should('eq', '/terveyskeskuskoulutusjaksot')
      cy.get('@decisionPut.all').should('have.length', 1)
      cy.then(() => openActor('virkailija', 'reviewReopened')).then((body) => {
        savedState(body, APPROVAL)
        expect(body.lisatiedotVirkailijalta).to.eq(NOTE)
      })
      button('Hyväksy ja lähetä').should('not.exist')
      cy.get(MAIN).contains(NOTE).should('be.visible')
      login('vastuuhenkilo')
      cy.then(() => openActor('vastuuhenkilo', 'receivedReview')).then((body) =>
        savedState(body, APPROVAL)
      )
      cy.get(MAIN).contains(NOTE).should('be.visible')
      button('Hyväksy').should('be.visible').and('not.be.disabled')
      restoreTrainee()
      openTrainee('traineePending').then((body) => savedState(body, APPROVAL))
      cy.get(MAIN)
        .contains('Terveyskeskuskoulutusjakso on lähetetty hyväksyttäväksi.')
        .should('be.visible')
    })

    it('vastuuhenkilö hyväksyy vasta vahvistuksesta ja päätös näkyy myös koulutettavalle', () => {
      let reviewedOn: string | null
      reviewSetup()
      login('vastuuhenkilo')
      cy.then(() => openActor('vastuuhenkilo', 'approvalBefore', true)).then((body) => {
        savedState(body, APPROVAL)
        reviewedOn = body.virkailijanKuittausaika
      })
      cy.get(MAIN)
        .contains(
          'h1',
          yek
            ? /^\s*YEK terveyskeskuskoulutusjakson hyväksyntä\s*$/
            : /^\s*Terveyskeskuskoulutusjakson hyväksyntä\s*$/
        )
        .should('be.visible')
      cy.get(MAIN).contains(NOTE).should('be.visible')
      button('Hyväksy').click()
      cy.get('#confirm-vastuuhenkilo')
        .should('be.visible')
        .and('contain.text', 'Vahvistuksen jälkeen terveyskeskuskoulutusjakso on hyväksytty.')
      cy.get('@decisionPut.all').should('have.length', 0)
      closeModal('#confirm-vastuuhenkilo')
      cy.then(() => readActor('vastuuhenkilo', APPROVAL))
      button('Hyväksy').click()
      button('Lähetä', '#confirm-vastuuhenkilo').click()
      cy.wait('@decisionPut').then(({ request, response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(request.body).to.deep.eq({})
      })
      cy.location('pathname').should('eq', '/terveyskeskuskoulutusjaksot')
      cy.get('@decisionPut.all').should('have.length', 1)
      cy.then(() => openActor('vastuuhenkilo', 'approvalReopened')).then((body) => {
        savedState(body, APPROVED)
        expect(body.virkailijanKuittausaika).to.eq(reviewedOn)
        expect(body.lisatiedotVirkailijalta).to.eq(NOTE)
        downloadPdfPages(
          `/api/vastuuhenkilo/terveyskeskuskoulutusjakso/tyoskentelyjakso-liite/${body.tyoskentelyjaksot[0].asiakirjat[0].id}`
        )
          .its('length')
          .should('be.greaterThan', 0)
      })
      button('Hyväksy').should('not.exist')
      button('Palauta muokattavaksi').should('not.exist')
      cy.get(MAIN)
        .contains(
          '.alert-success',
          'Terveyskeskuskoulutusjakso on tarkistettu ja vastuuhenkilö on hyväksynyt sen.'
        )
        .should('be.visible')
      restoreTrainee()
      openTrainee('traineeApproved').then((body) => savedState(body, APPROVED))
      cy.get(MAIN)
        .contains(
          '.alert-success',
          'Terveyskeskuskoulutusjakso on tarkistettu ja vastuuhenkilö on hyväksynyt sen.'
        )
        .should('be.visible')
      button('Lähetä').should('not.exist')
    })

    for (const actor of ['virkailija', 'vastuuhenkilo'] as const) {
      it(`${actor} vaatii palautussyyn ja koulutettava korjaa liitteen sekä lähettää pyynnön uudelleen`, () => {
        let before: RecordData
        if (actor === 'vastuuhenkilo') {
          reviewSetup()
          login('vastuuhenkilo')
        }
        cy.then(() => openActor(actor, 'returnBefore', true)).then((body) => {
          savedState(body, actor === 'virkailija' ? REVIEW : APPROVAL)
          before = body
        })
        button('Palauta muokattavaksi').click()
        cy.get('#return-to-sender').should('be.visible')
        button('Palauta muokattavaksi', '#return-to-sender').click()
        cy.get('#return-to-sender')
          .contains('.invalid-feedback', 'Pakollinen tieto')
          .should('be.visible')
        cy.get('@decisionPut.all').should('have.length', 0)
        cy.get('#return-to-sender').find('textarea').type(CORRECTION)
        closeModal('#return-to-sender')
        cy.then(() => cy.apiRequest({ method: 'GET', url: getApi(actor) })).then(({ body }) =>
          expect(body).to.deep.eq(before)
        )
        button('Palauta muokattavaksi').click()
        cy.get('#return-to-sender').find('textarea').should('have.value', CORRECTION)
        button('Palauta muokattavaksi', '#return-to-sender').click()
        cy.wait('@decisionPut').then(({ request, response }) => {
          expect(response?.statusCode).to.eq(200)
          if (actor === 'vastuuhenkilo')
            expect(request.body).to.deep.eq({ korjausehdotus: CORRECTION })
          else expect(sentForms).to.deep.eq([{ korjausehdotus: CORRECTION }])
        })
        cy.location('pathname').should('eq', '/terveyskeskuskoulutusjaksot')
        cy.get('@decisionPut.all').should('have.length', 1)
        cy.then(() => openActor(actor, 'returnReopened')).then((body) => {
          savedState(body, RETURNED)
          expect(
            body[
              actor === 'virkailija' ? 'virkailijanKorjausehdotus' : 'vastuuhenkilonKorjausehdotus'
            ]
          ).to.eq(CORRECTION)
        })
        cy.get(MAIN).contains(CORRECTION).should('be.visible')
        button(actor === 'virkailija' ? 'Hyväksy ja lähetä' : 'Hyväksy').should('not.exist')
        restoreTrainee()
        openTrainee('traineeReturned').then((body) => savedState(body, RETURNED))
        cy.get(MAIN).contains('.alert-danger', CORRECTION).should('be.visible')
        cy.get('input[type="file"]').should('have.length', 1).selectFile(
          {
            contents: 'cypress/fixtures/test.pdf',
            fileName: CORRECTED_FILE,
            mimeType: 'application/pdf'
          },
          { force: true }
        )
        cy.then(() =>
          cy
            .intercept(
              'PUT',
              `**/api/${role}/tyoskentelyjaksot/${fixture.tyoskentelyjaksoId}/asiakirjat`
            )
            .as('correctedAttachment')
        )
        submitTrainee('PUT', 'resubmittedRequest')
        cy.wait('@correctedAttachment').its('response.statusCode').should('eq', 200)
        cy.then(() => {
          corrected = true
        })
        openTrainee('traineeResubmitted').then((body) => {
          savedState(body, REVIEW)
          expect(body.virkailijanKuittausaika).to.eq(null)
        })
        cy.get(MAIN).find('.alert-danger:visible').should('not.exist')
        login('virkailija')
        cy.then(() => openActor('virkailija', 'clerkReceivedCorrection')).then((body) =>
          savedState(body, REVIEW)
        )
        cy.get(MAIN).contains(CORRECTED_FILE).should('be.visible')
        button('Hyväksy ja lähetä').should('be.visible').and('not.be.disabled')
      })
    }
  })
}
