import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL } from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-sign'
const API = '/api/virkailija/koejakso/vastuuhenkilonarvio'
const VASTUUHENKILON_API = '/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio'
const ERIKOISTUVAN_API = '/api/erikoistuva-laakari/koejakso'
const YHTEENVETO_OTSIKKO = 'Opintohallinnon virkailijan yhteenveto'
const YHTEENVETO_KAPPALEET = [
  'E2E: Koejakson työskentelyjaksot ja työtodistukset on tarkistettu.',
  'E2E: Aloituskeskustelu, väliarviointi ja loppukeskustelu on hyväksytty.'
]
const LISATIEDOT = 'E2E: Vastuuhenkilö voi tehdä koejakson lopullisen arvioinnin.'
const VIRKAILIJA_NIMI = 'Daniel Siekkinen'
const PUHELINNUMERO = '+358401234567'
const TARKISTETTU_TEKSTI = 'Koejakso on tarkistettu ja odottaa vastuuhenkilön hyväksyntää'

type Hyvaksyja = {
  id: number | null
  sopimusHyvaksytty: boolean
  kuittausaika: string | null
}

type VastuuhenkilonArvio = {
  id: number
  erikoistuvanSahkoposti: string
  erikoistuvanPuhelinnumero: string
  erikoistuvanKuittausaika: string
  virkailijanYhteenveto: string | null
  lisatiedotVirkailijalta: string | null
  virkailijanKorjausehdotus: string | null
  vastuuhenkilonKorjausehdotus: string | null
  koejaksoHyvaksytty: boolean | null
  virkailija: Hyvaksyja | null
  vastuuhenkilo: Hyvaksyja | null
}

const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy ja lähetä\s*$/)
const yhteenvetokentta = () =>
  cy.get(SIVU).contains('label', YHTEENVETO_OTSIKKO).closest('.form-group')
const lisatietokentta = () =>
  cy.get(VAHVISTUS).contains('label', 'Lisätiedot vastuuhenkilölle').parent().find('textarea')

const yhteenvetoEditor = () =>
  yhteenvetokentta()
    .find('iframe.tox-edit-area__iframe')
    .should('be.visible')
    .its('0.contentDocument.body')
    .should('not.be.empty')
    // cy.wrap keeps a raw iframe body as a DOM element; paragraph checks need jQuery.
    .then((body: HTMLBodyElement) => cy.wrap(Cypress.$(body)))

const tarkistaYhteenvedonKappaleet = (elementti: JQuery<HTMLElement>) => {
  const kappaleet = elementti
    .find('p')
    .map((_, p) => p.textContent?.trim())
    .get()
  expect(kappaleet).to.deep.eq(YHTEENVETO_KAPPALEET)
}

describe('Koejakson tarkistus virkailijan käyttöliittymässä', () => {
  let arvio: VastuuhenkilonArvio

  const tarkistaPerustiedot = (tallennettu: VastuuhenkilonArvio) => {
    expect(tallennettu).to.include({
      id: arvio.id,
      erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
      erikoistuvanPuhelinnumero: PUHELINNUMERO,
      erikoistuvanKuittausaika: arvio.erikoistuvanKuittausaika,
      virkailijanKorjausehdotus: null,
      vastuuhenkilonKorjausehdotus: null,
      koejaksoHyvaksytty: null
    })
    // Forwarding the review is not the responsible person's final approval.
    expect(tallennettu.vastuuhenkilo?.sopimusHyvaksytty ?? false).to.eq(false)
    expect(tallennettu.vastuuhenkilo?.kuittausaika ?? null).to.be.null
  }

  const tarkistaOdottaaVirkailijaa = (tallennettu: VastuuhenkilonArvio) => {
    tarkistaPerustiedot(tallennettu)
    expect(tallennettu.virkailija?.sopimusHyvaksytty ?? false).to.eq(false)
    expect(tallennettu.virkailija?.kuittausaika ?? null).to.be.null
    expect(tallennettu.virkailijanYhteenveto).to.be.null
    expect(tallennettu.lisatiedotVirkailijalta).to.be.null
  }

  const tarkistaTallennus = (
    tallennettu: VastuuhenkilonArvio,
    yhteenveto: string | null,
    lisatiedot: string | null,
    kuittausaika: string
  ) => {
    tarkistaPerustiedot(tallennettu)
    expect(tallennettu.virkailijanYhteenveto).to.eq(yhteenveto)
    expect(tallennettu.lisatiedotVirkailijalta).to.eq(lisatiedot)
    expect(tallennettu.virkailija).to.include({
      id: Cypress.expose('virkailijaId'),
      sopimusHyvaksytty: true,
      kuittausaika
    })
  }

  const avaaVirkailijanLomake = (alias: string) => {
    // A distinct one-use alias prevents a reopen from consuming an earlier response.
    cy.intercept({ method: 'GET', url: `**${API}/${arvio.id}`, times: 1 }).as(alias)
    cy.visit(`/koejakso/virkailijan-tarkistus/${arvio.id}`)
    return cy.wait(`@${alias}`).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.koulutussopimusHyvaksytty).to.eq(true)
      expect(response?.body.loppukeskustelu.lahiesimies.sopimusHyvaksytty).to.eq(true)
      return response?.body
    })
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, seedVirkailija: true, storeTokens: true })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    // Earlier approvals and the trainee's request are setup; only the clerk signs through the UI.
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    cy.task('db:ensureKoejaksoTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.apiRequest({ method: 'GET', url: ERIKOISTUVAN_API })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.loppukeskustelunTila).to.eq('HYVAKSYTTY')
        expect(body.vastuuhenkilonArvionTila).to.eq('UUSI')
        return cy.apiRequest({
          method: 'GET',
          url: '/api/erikoistuva-laakari/vastuuhenkilonarvio-lomake'
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body).to.include({
          koulutussopimusHyvaksytty: true,
          tyoskentelyjaksoLiitetty: true,
          tyoskentelyjaksonPituusRiittava: true,
          tyotodistusLiitetty: true
        })
        expect(body.vastuuhenkilo.id).to.eq(Cypress.expose('vastuuhenkiloId'))
        // This endpoint accepts the JSON as a form parameter, not a JSON request body.
        return cy.apiRequest({
          method: 'POST',
          url: `${ERIKOISTUVAN_API}/vastuuhenkilonarvio`,
          form: true,
          body: {
            vastuuhenkilonArvioJson: JSON.stringify({
              erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
              erikoistuvanPuhelinnumero: PUHELINNUMERO,
              virkailijanYhteenveto: null,
              lisatiedotVirkailijalta: null,
              virkailijanKorjausehdotus: null,
              vastuuhenkilonKorjausehdotus: null,
              koejaksoHyvaksytty: null
            })
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(201)
        expect(body.id).to.be.a('number')
        expect(body.erikoistuvanKuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        arvio = body
        tarkistaOdottaaVirkailijaa(arvio)
      })
  })

  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
  })

  const tapaukset = [
    {
      nimi: 'välittää yhteenvedon kappaleineen ja lisätiedot vastuuhenkilölle vasta vahvistuksesta',
      lisaaTekstit: true
    },
    {
      nimi: 'sallii tarkistuksen ilman vapaaehtoista yhteenvetoa ja lisätietoja',
      lisaaTekstit: false
    }
  ]

  tapaukset.forEach(({ nimi, lisaaTekstit }) => {
    it(nimi, () => {
      let lahetettyYhteenveto: string | null
      let lahetetytLisatiedot: string | null
      let virkailijanKuittausaika: string

      cy.task<string | null>('tokens:get', 'virkailijaToken', { log: false }).then((token) => cy.loginAsVirkailija(token ?? undefined))
      const tarkistuspyynto = cy.spy().as('tarkistuspyynto')
      cy.intercept('PUT', `**${API}`, (request) => {
        tarkistuspyynto(request.body)
      }).as('hyvaksyTarkistus')
      avaaVirkailijanLomake('tarkistusEnnenHyvaksyntaa').then((tallennettu) => {
        tarkistaOdottaaVirkailijaa(tallennettu)
        expect(tallennettu.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
      })
      cy.get(SIVU).contains('h1', 'Koejakson tarkistus').should('be.visible')
      yhteenvetoEditor().should('have.attr', 'contenteditable', 'true')
      if (lisaaTekstit) {
        // Type into the real editor iframe; retain paragraph breaks in the saved HTML.
        yhteenvetoEditor().type(YHTEENVETO_KAPPALEET.join('{enter}'))
      }

      hyvaksyPainike().should('be.visible').and('not.be.disabled').click()
      cy.get(VAHVISTUS)
        .should('be.visible')
        .and('contain.text', 'Vahvista lomakkeen lähetys')
        .and('contain.text', 'Lähetyksen jälkeen lomake menee vastuuhenkilölle hyväksyttäväksi.')
      if (lisaaTekstit) {
        lisatietokentta().type(LISATIEDOT)
      } else {
        lisatietokentta().should('have.value', '')
      }
      cy.get('@tarkistuspyynto').should('not.have.been.called')
      cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
      cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
      cy.get('@tarkistuspyynto').should('not.have.been.called')
      if (lisaaTekstit) {
        yhteenvetoEditor().should(($body) => tarkistaYhteenvedonKappaleet($body))
      }
      cy.apiRequest({ method: 'GET', url: `${API}/${arvio.id}` }).then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaOdottaaVirkailijaa(body)
        expect(body.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
      })

      hyvaksyPainike().should('not.be.disabled').click()
      cy.get(VAHVISTUS).should('be.visible')
      lisatietokentta().should('have.value', lisaaTekstit ? LISATIEDOT : '')
      cy.get(VAHVISTUS)
        .contains('button', /^\s*Hyväksy ja lähetä\s*$/)
        .click()
      cy.wait('@hyvaksyTarkistus').then(({ request, response }) => {
        expect(request.body.id).to.eq(arvio.id)
        expect(request.body.virkailijanKorjausehdotus).to.be.null
        lahetettyYhteenveto = request.body.virkailijanYhteenveto
        lahetetytLisatiedot = request.body.lisatiedotVirkailijalta
        if (lisaaTekstit) {
          expect(lahetettyYhteenveto).to.be.a('string')
          tarkistaYhteenvedonKappaleet(Cypress.$('<div>').html(lahetettyYhteenveto!))
          expect(lahetetytLisatiedot).to.eq(LISATIEDOT)
        } else {
          expect(
            Cypress.$('<div>')
              .html(lahetettyYhteenveto ?? '')
              .text()
              .trim()
          ).to.eq('')
          expect(lahetetytLisatiedot ?? '').to.eq('')
        }
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.virkailija.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        virkailijanKuittausaika = response?.body.virkailija.kuittausaika
        tarkistaTallennus(
          response?.body,
          lahetettyYhteenveto,
          lahetetytLisatiedot,
          virkailijanKuittausaika
        )
      })
      cy.get('@tarkistuspyynto').should('have.been.calledOnce')
      cy.location('pathname').should('eq', '/koejakso')

      avaaVirkailijanLomake('tarkistusHyvaksynnanJalkeen').then((tallennettu) => {
        tarkistaTallennus(
          tallennettu,
          lahetettyYhteenveto,
          lahetetytLisatiedot,
          virkailijanKuittausaika
        )
        expect(tallennettu.tila).to.eq('ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA')
      })
      cy.get(SIVU).contains('.alert-dark', TARKISTETTU_TEKSTI).should('be.visible')
      yhteenvetokentta().find('iframe').should('not.exist')
      hyvaksyPainike().should('not.exist')
      cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')
      cy.get(SIVU).contains('h3', 'Tarkistanut').should('be.visible')
      cy.get(SIVU).contains('p', VIRKAILIJA_NIMI).should('be.visible')
      if (lisaaTekstit) {
        yhteenvetokentta()
          .find('.editor-readonly')
          .should('be.visible')
          .should(tarkistaYhteenvedonKappaleet)
        cy.get(SIVU).contains('p', LISATIEDOT).should('be.visible')
      } else {
        yhteenvetokentta().find('.editor-readonly').should('have.text', '')
        cy.get(SIVU).contains('strong', 'Lisätiedot virkailijalta').should('not.be.visible')
      }

      cy.task<string | null>('tokens:get', 'vastuuhenkiloToken', { log: false }).then((token) => cy.loginAsVastuuhenkilo(token ?? undefined))
      cy.intercept({ method: 'GET', url: `**${VASTUUHENKILON_API}/${arvio.id}`, times: 1 }).as(
        'vastuuhenkilonSaamaArvio'
      )
      cy.visit(`/koejakso/vastuuhenkilon-arvio/${arvio.id}`)
      cy.wait('@vastuuhenkilonSaamaArvio').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        tarkistaTallennus(
          response?.body,
          lahetettyYhteenveto,
          lahetetytLisatiedot,
          virkailijanKuittausaika
        )
        // The responsible person's role sees this as awaiting their own decision.
        expect(response?.body.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
      })
      if (lisaaTekstit) {
        cy.get(SIVU)
          .contains('h5', YHTEENVETO_OTSIKKO)
          .next('div')
          .should('be.visible')
          .should(tarkistaYhteenvedonKappaleet)
        cy.get(SIVU).contains('p', LISATIEDOT).should('be.visible')
      } else {
        cy.get(SIVU).contains('h5', YHTEENVETO_OTSIKKO).should('not.be.visible')
        cy.get(SIVU).contains('strong', 'Lisätiedot virkailijalta').should('not.be.visible')
      }
      cy.get(SIVU).contains('p', VIRKAILIJA_NIMI).should('be.visible')
      cy.get(SIVU)
        .contains('button', /^\s*Hyväksy\s*$/)
        .should('be.visible')
        .and('not.be.disabled')

      cy.loginAsErikoistuva()
      cy.intercept({ method: 'GET', url: `**${ERIKOISTUVAN_API}`, times: 1 }).as(
        'erikoistuvanOdottaaVastuuhenkiloa'
      )
      cy.visit('/koejakso/vastuuhenkilon-arvio')
      cy.wait('@erikoistuvanOdottaaVastuuhenkiloa').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        tarkistaTallennus(
          response?.body.vastuuhenkilonArvio,
          lahetettyYhteenveto,
          lahetetytLisatiedot,
          virkailijanKuittausaika
        )
        expect(response?.body.vastuuhenkilonArvionTila).to.eq('ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA')
      })
      cy.get(SIVU)
        .contains('.alert-dark', 'Odottaa vastuuhenkilön arviointia.')
        .should('be.visible')
      cy.get(SIVU)
        .contains('button', /^\s*Lähetä\s*$/)
        .should('not.exist')
    })
  })
})
