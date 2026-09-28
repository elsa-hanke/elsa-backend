import {
  E2E_ERIKOISTUVA_EMAIL,
  ESIHENKILÖ_EMAIL,
  KOULUTTAJA_EMAIL
} from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-send'
const API = '/api/kouluttaja/koejakso/kehittamistoimenpiteet'
const VALIARVIOINTI_API = '/api/kouluttaja/koejakso/valiarviointi'
const HYVAKSYTTY_TEKSTI =
  'Kehittämistoimenpiteiden arviointi on hyväksytty kaikkien osapuolten toimesta.'
const RIITTAVAT_TEKSTI = 'Kehittämistoimenpiteet on todettu riittäviksi.'
const EI_RIITTAVAT_TEKSTI =
  'Kehittämistoimenpiteet eivät ole olleet riittäviä. Tarve keskustella erikoisalan vastuuhenkilön tai hänen valtuuttamansa henkilön kanssa.'
const TOIMENPITEET =
  'E2E: Sovittiin viikoittaisesta ohjauskeskustelusta ja vastaanoton seurannasta.'
const MUU_KATEGORIA = 'E2E: Ajankäytön suunnittelu vastaanotolla'
const KATEGORIAT = ['TYOSSASUORIUTUMINEN', 'MUU']
const ESIHENKILO_NIMI = 'Tessa Testilä'

type Hyvaksyja = {
  id: number
  sopimusHyvaksytty: boolean
  kuittausaika: string | null
}

type Kehittamistoimenpiteet = {
  id: number
  kehittamistoimenpiteetRiittavat: boolean | null
  kehittamistoimenpiteetKuvaus: string | null
  kehittamistoimenpideKategoriat: string[] | null
  muuKategoria: string | null
  korjausehdotus: string | null
  erikoistuvanKuittausaika: string | null
  lahikouluttaja: Hyvaksyja
  lahiesimies: Hyvaksyja
}

const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy ja lähetä\s*$/)
const loppukeskustelunLinkki = () =>
  cy.get(SIVU).contains('h2', 'Loppukeskustelu').parent().contains('a', 'Pyydä arviointia')

const tarkistaTaustatNakyvissa = () => {
  cy.get(SIVU).contains('p', TOIMENPITEET).should('be.visible')
  cy.get(SIVU).contains('li', 'työssä suoriutumiseen').should('be.visible')
  cy.get(SIVU).contains('li', MUU_KATEGORIA).should('be.visible')
}

const tarkistaArvioNakyvissa = (teksti: string) => {
  cy.get(SIVU).contains('p', teksti).should('be.visible')
  cy.get(SIVU).find('input[type="radio"]').should('not.exist')
}

const peruutaVahvistus = (pyyntojaEnnen: number) => {
  hyvaksyPainike().should('be.visible').and('not.be.disabled').click()
  cy.get(VAHVISTUS).should('be.visible').and('contain.text', 'Vahvista lomakkeen lähetys')
  cy.get('@hyvaksyntapyynto').should('have.callCount', pyyntojaEnnen)
  cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@hyvaksyntapyynto').should('have.callCount', pyyntojaEnnen)
  hyvaksyPainike().should('be.visible').and('not.be.disabled')
}

const vahvistaHyvaksynta = () => {
  hyvaksyPainike().click()
  cy.get(VAHVISTUS)
    .should('be.visible')
    .contains('button', /^\s*Hyväksy ja lähetä\s*$/)
    .click()
  return cy.wait('@hyvaksyKehittamistoimenpiteet')
}

describe('Kehittämistoimenpiteiden arviointi kouluttajan ja lähiesihenkilön käyttöliittymässä', () => {
  let lomake: Kehittamistoimenpiteet
  let esihenkiloId: number
  let esihenkiloToken: string

  const tarkistaTallennus = (
    tallennettu: Kehittamistoimenpiteet,
    riittavat: boolean | null,
    kouluttajanKuittausaika: string | null,
    esihenkilonKuittausaika: string | null
  ) => {
    expect(tallennettu).to.include({
      id: lomake.id,
      kehittamistoimenpiteetRiittavat: riittavat,
      erikoistuvanKuittausaika: lomake.erikoistuvanKuittausaika,
      korjausehdotus: null
    })
    expect(tallennettu.lahikouluttaja).to.include({
      id: Cypress.env('kouluttajaId'),
      sopimusHyvaksytty: kouluttajanKuittausaika !== null,
      kuittausaika: kouluttajanKuittausaika
    })
    expect(tallennettu.lahiesimies).to.include({
      id: esihenkiloId,
      sopimusHyvaksytty: esihenkilonKuittausaika !== null,
      kuittausaika: esihenkilonKuittausaika
    })
  }

  const avaaArvioijanLomake = () => {
    cy.intercept('GET', `**${API}/${lomake.id}`).as('haeKehittamistoimenpiteet')
    cy.visit(`/koejakso/kehittamistoimenpiteet/${lomake.id}`)
    return cy.wait('@haeKehittamistoimenpiteet').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      // These fields are enriched from the midterm assessment on GET, not on POST/PUT.
      expect(response?.body.kehittamistoimenpiteetKuvaus).to.eq(TOIMENPITEET)
      expect(response?.body.kehittamistoimenpideKategoriat).to.have.members(KATEGORIAT)
      expect(response?.body.muuKategoria).to.eq(MUU_KATEGORIA)
      return response?.body as Kehittamistoimenpiteet
    })
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
    cy.task<{ kayttajaId: number | string; token: string }>('db:seedKouluttaja', {
      email: ESIHENKILÖ_EMAIL,
      etunimi: 'Tessa',
      sukunimi: 'Testilä'
    }).then((result) => {
      const kayttajaId = Number(result.kayttajaId)
      expect(Number.isSafeInteger(kayttajaId), 'supervisor database ID').to.eq(true)
      expect(kayttajaId).to.be.greaterThan(0).and.not.eq(Cypress.env('kouluttajaId'))
      expect(result.token).to.be.a('string').and.not.be.empty
      esihenkiloId = kayttajaId
      esihenkiloToken = result.token
    })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:ensureAloituskeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    // Midterm approval is prerequisite setup; only development-measure approvals use the UI.
    cy.apiRequest({ method: 'GET', url: '/api/erikoistuva-laakari/koejakso' })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.aloituskeskustelunTila).to.eq('HYVAKSYTTY')
        const sopimus = body.koulutussopimus
        return cy.apiRequest({
          method: 'POST',
          url: '/api/erikoistuva-laakari/koejakso/valiarviointi',
          body: {
            erikoistuvanNimi: sopimus.erikoistuvanNimi,
            erikoistuvanErikoisala: sopimus.erikoistuvanErikoisala,
            erikoistuvanYliopisto: sopimus.erikoistuvanYliopisto,
            erikoistuvanOpiskelijatunnus: sopimus.erikoistuvanOpiskelijatunnus,
            lahikouluttaja: { id: Cypress.env('kouluttajaId'), sopimusHyvaksytty: false },
            lahiesimies: { id: esihenkiloId, sopimusHyvaksytty: false }
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(201)
        cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
        return cy.apiRequest({
          method: 'PUT',
          url: VALIARVIOINTI_API,
          body: {
            ...body,
            edistyminenTavoitteidenMukaista: false,
            kehittamistoimenpideKategoriat: KATEGORIAT,
            muuKategoria: MUU_KATEGORIA,
            kehittamistoimenpiteet: TOIMENPITEET,
            korjausehdotus: null
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.lahikouluttaja.sopimusHyvaksytty).to.eq(true)
        expect(body.lahiesimies.sopimusHyvaksytty).to.eq(false)
        cy.loginAsEsihenkilo(esihenkiloToken)
        return cy.apiRequest({ method: 'PUT', url: VALIARVIOINTI_API, body })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.lahiesimies.sopimusHyvaksytty).to.eq(true)
        cy.loginAsErikoistuva()
        return cy.apiRequest({ method: 'GET', url: '/api/erikoistuva-laakari/koejakso' })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.valiarvioinninTila).to.eq('HYVAKSYTTY')
        expect(body.valiarviointi.edistyminenTavoitteidenMukaista).to.eq(false)
        expect(body.valiarviointi.kehittamistoimenpideKategoriat).to.have.members(KATEGORIAT)
        expect(body.valiarviointi.kehittamistoimenpiteet).to.eq(TOIMENPITEET)
        expect(body.kehittamistoimenpiteidenTila).to.eq('UUSI')
        expect(body.loppukeskustelunTila).to.eq('EI_AKTIIVINEN')
        const arvio = body.valiarviointi
        return cy.apiRequest({
          method: 'POST',
          url: '/api/erikoistuva-laakari/koejakso/kehittamistoimenpiteet',
          body: {
            erikoistuvanNimi: arvio.erikoistuvanNimi,
            erikoistuvanErikoisala: arvio.erikoistuvanErikoisala,
            erikoistuvanYliopisto: arvio.erikoistuvanYliopisto,
            erikoistuvanOpiskelijatunnus: arvio.erikoistuvanOpiskelijatunnus,
            kehittamistoimenpiteetRiittavat: null,
            korjausehdotus: null,
            lahikouluttaja: { id: Cypress.env('kouluttajaId'), sopimusHyvaksytty: false },
            lahiesimies: { id: esihenkiloId, sopimusHyvaksytty: false }
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(201)
        expect(body.id).to.be.a('number')
        lomake = body
        tarkistaTallennus(lomake, null, null, null)
      })
  })

  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
  })

  const tapaukset = [
    {
      nimi: 'säilyttää riittäviksi arvioidut toimenpiteet molempien hyväksynnöissä',
      riittavat: true
    },
    {
      nimi: 'säilyttää arvion riittämättömistä toimenpiteistä molempien hyväksynnöissä',
      riittavat: false
    }
  ]

  tapaukset.forEach(({ nimi, riittavat }) => {
    it(nimi, () => {
      let kouluttajanKuittausaika: string
      let esihenkilonKuittausaika: string
      const arvioTeksti = riittavat ? RIITTAVAT_TEKSTI : EI_RIITTAVAT_TEKSTI

      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
      const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
      // Both decisions below are real browser requests; neither approval response is stubbed.
      cy.intercept('PUT', `**${API}`, (request) => {
        hyvaksyntapyynto(request.body)
      }).as('hyvaksyKehittamistoimenpiteet')
      avaaArvioijanLomake().then((tallennettu) => tarkistaTallennus(tallennettu, null, null, null))
      tarkistaTaustatNakyvissa()

      hyvaksyPainike().click()
      cy.get(SIVU).find('.invalid-feedback:visible').should('contain.text', 'Pakollinen tieto')
      cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
      cy.get('@hyvaksyntapyynto').should('not.have.been.called')

      cy.get(SIVU).contains('label', arvioTeksti).click()
      peruutaVahvistus(0)
      cy.get(SIVU)
        .contains('label', arvioTeksti)
        .parent()
        .find('input[type="radio"]')
        .should('be.checked')
      cy.apiRequest({ method: 'GET', url: `${API}/${lomake.id}` }).then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaTallennus(body, null, null, null)
      })

      vahvistaHyvaksynta().then(({ request, response }) => {
        expect(request.body).to.include({
          id: lomake.id,
          kehittamistoimenpiteetRiittavat: riittavat
        })
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.lahikouluttaja.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        kouluttajanKuittausaika = response?.body.lahikouluttaja.kuittausaika
        tarkistaTallennus(response?.body, riittavat, kouluttajanKuittausaika, null)
      })
      cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
      cy.location('pathname').should('eq', '/koejakso')
      avaaArvioijanLomake().then((tallennettu) => {
        tarkistaTallennus(tallennettu, riittavat, kouluttajanKuittausaika, null)
      })
      tarkistaTaustatNakyvissa()
      tarkistaArvioNakyvissa(arvioTeksti)
      hyvaksyPainike().should('not.exist')

      cy.loginAsErikoistuva()
      cy.intercept('GET', '**/api/erikoistuva-laakari/koejakso').as('koejaksoOdottaaEsihenkiloa')
      cy.visit('/koejakso')
      cy.wait('@koejaksoOdottaaEsihenkiloa').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        tarkistaTallennus(
          response?.body.kehittamistoimenpiteet,
          riittavat,
          kouluttajanKuittausaika,
          null
        )
        expect(response?.body.kehittamistoimenpiteidenTila).to.eq('ODOTTAA_HYVAKSYNTAA')
        expect(response?.body.loppukeskustelunTila).to.eq('EI_AKTIIVINEN')
      })
      loppukeskustelunLinkki().should('be.visible').and('have.attr', 'aria-disabled', 'true')

      cy.loginAsEsihenkilo(esihenkiloToken)
      avaaArvioijanLomake().then((tallennettu) => {
        tarkistaTallennus(tallennettu, riittavat, kouluttajanKuittausaika, null)
      })
      tarkistaTaustatNakyvissa()
      tarkistaArvioNakyvissa(arvioTeksti)
      peruutaVahvistus(1)
      cy.apiRequest({ method: 'GET', url: `${API}/${lomake.id}` }).then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaTallennus(body, riittavat, kouluttajanKuittausaika, null)
      })

      vahvistaHyvaksynta().then(({ request, response }) => {
        expect(request.body).to.include({
          id: lomake.id,
          kehittamistoimenpiteetRiittavat: riittavat,
          korjausehdotus: null
        })
        expect(request.body.lahikouluttaja.kuittausaika).to.eq(kouluttajanKuittausaika)
        expect(request.body.lahiesimies.id).to.eq(esihenkiloId)
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.lahiesimies.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        esihenkilonKuittausaika = response?.body.lahiesimies.kuittausaika
        tarkistaTallennus(
          response?.body,
          riittavat,
          kouluttajanKuittausaika,
          esihenkilonKuittausaika
        )
      })
      cy.get('@hyvaksyntapyynto').should('have.been.calledTwice')
      cy.location('pathname').should('eq', '/koejakso')
      avaaArvioijanLomake().then((tallennettu) => {
        tarkistaTallennus(tallennettu, riittavat, kouluttajanKuittausaika, esihenkilonKuittausaika)
      })
      cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
      tarkistaTaustatNakyvissa()
      tarkistaArvioNakyvissa(arvioTeksti)
      hyvaksyPainike().should('not.exist')
      cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')

      cy.loginAsErikoistuva()
      cy.intercept('GET', '**/api/erikoistuva-laakari/koejakso').as('koejaksoHyvaksytty')
      cy.visit('/koejakso/kehittamistoimenpiteet')
      cy.wait('@koejaksoHyvaksytty').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        tarkistaTallennus(
          response?.body.kehittamistoimenpiteet,
          riittavat,
          kouluttajanKuittausaika,
          esihenkilonKuittausaika
        )
        expect(response?.body.valiarviointi.edistyminenTavoitteidenMukaista).to.eq(false)
        expect(response?.body.valiarviointi.kehittamistoimenpiteet).to.eq(TOIMENPITEET)
        expect(response?.body.valiarviointi.kehittamistoimenpideKategoriat).to.have.members(
          KATEGORIAT
        )
        expect(response?.body.valiarviointi.muuKategoria).to.eq(MUU_KATEGORIA)
        expect(response?.body.kehittamistoimenpiteidenTila).to.eq('HYVAKSYTTY')
        // Completion of this assessment opens the final discussion for either outcome.
        expect(response?.body.loppukeskustelunTila).to.eq('UUSI')
      })
      cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
      cy.get(SIVU).contains('td', TOIMENPITEET).should('be.visible')
      tarkistaArvioNakyvissa(arvioTeksti)
      cy.get(SIVU)
        .find('.hyvaksynta-pvm')
        .parent()
        .contains('p', ESIHENKILO_NIMI)
        .should('be.visible')
      cy.get(SIVU).contains('button', 'Tyhjennä lomake').should('not.exist')

      cy.visit('/koejakso')
      cy.wait('@koejaksoHyvaksytty').its('response.statusCode').should('eq', 200)
      loppukeskustelunLinkki().should('be.visible').and('not.have.attr', 'aria-disabled')
      // Query again because attribute assertions can change the yielded subject.
      loppukeskustelunLinkki().click()
      cy.location('pathname').should('eq', '/koejakso/loppukeskustelu')
      cy.get(SIVU)
        .contains('label', /^\s*Kouluttaja\s*\*/)
        .should('be.visible')
    })
  })
})
