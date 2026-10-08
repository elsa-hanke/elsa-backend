import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL
} from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-sign'
const API = '/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio'
const VIRKAILIJAN_API = '/api/virkailija/koejakso/vastuuhenkilonarvio'
const ERIKOISTUVAN_API = '/api/erikoistuva-laakari/koejakso'
const PAATOS = 'Koejakso on'
const PERUSTELU = 'Perustelu hylkäämiselle'
const KESKUSTELTU =
  'Hylätyn koejakson arviointi on käyty läpi koejakson suorittajan kanssa keskustellen'
const HYLKAYKSEN_PERUSTELU = 'E2E: Koejakson osaamistavoitteet eivät vielä täyty.'
const VIRKAILIJAN_YHTEENVETO = '<p>E2E: Koejakson lomakkeet ja työtodistus on tarkistettu.</p>'
const VIRKAILIJAN_LISATIEDOT = 'E2E: Koejakso on valmis vastuuhenkilön arvioitavaksi.'
const ERIKOISTUVAN_PUHELIN = '+358401234567'
const VASTUUHENKILON_PUHELIN = '+358401234569'
const VASTUUHENKILON_NIMI = 'Mia Ålands'

type Hyvaksyja = {
  id: number | null
  sopimusHyvaksytty: boolean
  kuittausaika: string | null
}

type Arvio = {
  id: number
  erikoistuvanKuittausaika: string
  virkailija: Hyvaksyja
  vastuuhenkilo: Hyvaksyja | null
  koejaksoHyvaksytty: boolean | null
  perusteluHylkaamiselle: string | null
  hylattyArviointiKaytyLapiKeskustellen: boolean | null
}

const kentta = (label: string) => cy.get(SIVU).contains('label', label).closest('.form-group')
const sahkoposti = () => kentta('Sähköpostiosoite').find('input')
const puhelin = () => kentta('Matkapuhelinnumero').find('input')
const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy\s*$/)
const keskusteluvalinta = () => kentta(KESKUSTELTU).find('input[type="checkbox"]')

const taytaYhteystiedot = () => {
  sahkoposti().clear().type(VASTUUHENKILO_EMAIL)
  puhelin().clear().type(VASTUUHENKILON_PUHELIN)
}

const valitsePaatos = (hyvaksytty: boolean) => {
  kentta(PAATOS)
    .contains('label', hyvaksytty ? /^\s*Hyväksytty\s*$/ : /^\s*Hylätty\s*$/)
    .click()
  kentta(PAATOS).find('input[type="radio"]:checked').should('have.value', String(hyvaksytty))
}

const tarkistaEtteiLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@arviointipyynto').should('not.have.been.called')
}

const tarkistaPaatosNakyvissa = (hyvaksytty: boolean) => {
  kentta(PAATOS)
    .should('be.visible')
    .and('contain.text', hyvaksytty ? 'Hyväksytty' : 'Hylätty')
  kentta(PAATOS).find('input').should('not.exist')
  if (hyvaksytty) {
    cy.get(SIVU).contains('label', PERUSTELU).should('not.exist')
    cy.get(SIVU).contains('label', KESKUSTELTU).should('not.exist')
  } else {
    kentta(PERUSTELU).should('be.visible').and('contain.text', HYLKAYKSEN_PERUSTELU)
    kentta(PERUSTELU).find('textarea').should('not.exist')
    kentta(KESKUSTELTU).should('be.visible').and('contain.text', 'Kyllä')
    keskusteluvalinta().should('not.exist')
  }
  cy.get(SIVU).find('.hyvaksynta-pvm').should('have.length', 2)
  cy.get(SIVU)
    .find('.hyvaksynta-pvm')
    .first()
    .parent()
    .contains('p', VASTUUHENKILON_NIMI)
    .should('be.visible')
}

describe('Vastuuhenkilön lopullinen koejaksoarvio käyttöliittymässä', () => {
  let arvio: Arvio

  const tarkistaAiemmatTiedot = (tallennettu: Arvio) => {
    expect(tallennettu).to.include({
      id: arvio.id,
      erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
      erikoistuvanPuhelinnumero: ERIKOISTUVAN_PUHELIN,
      erikoistuvanKuittausaika: arvio.erikoistuvanKuittausaika,
      virkailijanYhteenveto: VIRKAILIJAN_YHTEENVETO,
      lisatiedotVirkailijalta: VIRKAILIJAN_LISATIEDOT,
      virkailijanKorjausehdotus: null,
      vastuuhenkilonKorjausehdotus: null
    })
    expect(tallennettu.virkailija).to.include({
      id: Cypress.env('virkailijaId'),
      sopimusHyvaksytty: true,
      kuittausaika: arvio.virkailija.kuittausaika
    })
  }

  const tarkistaOdottaaPaatosta = (tallennettu: Arvio) => {
    tarkistaAiemmatTiedot(tallennettu)
    expect(tallennettu).to.include({
      koejaksoHyvaksytty: null,
      perusteluHylkaamiselle: null,
      hylattyArviointiKaytyLapiKeskustellen: null
    })
    expect(tallennettu.vastuuhenkilo?.sopimusHyvaksytty ?? false).to.eq(false)
    expect(tallennettu.vastuuhenkilo?.kuittausaika ?? null).to.be.null
  }

  const tarkistaTallennettuPaatos = (
    tallennettu: Arvio,
    hyvaksytty: boolean,
    kuittausaika: string
  ) => {
    tarkistaAiemmatTiedot(tallennettu)
    expect(tallennettu).to.include({
      koejaksoHyvaksytty: hyvaksytty,
      perusteluHylkaamiselle: hyvaksytty ? null : HYLKAYKSEN_PERUSTELU,
      hylattyArviointiKaytyLapiKeskustellen: hyvaksytty ? null : true,
      vastuuhenkilonSahkoposti: VASTUUHENKILO_EMAIL,
      vastuuhenkilonPuhelinnumero: VASTUUHENKILON_PUHELIN
    })
    expect(tallennettu.vastuuhenkilo).to.include({
      id: Cypress.env('vastuuhenkiloId'),
      sopimusHyvaksytty: true,
      kuittausaika
    })
  }

  const avaaArvio = (alias: string) => {
    // Use a fresh one-use alias for each visit, including reopening after the decision.
    cy.intercept({ method: 'GET', url: `**${API}/${arvio.id}`, times: 1 }).as(alias)
    cy.visit(`/koejakso/vastuuhenkilon-arvio/${arvio.id}`)
    return cy.wait(`@${alias}`).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      // External archiving, including Helsinki integration tests, is outside this spec.
      expect(response?.body.arkistoitava, 'ulkoinen arkistointi pois käytöstä E2E-testissä').to.eq(
        false
      )
      return response?.body
    })
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, seedVirkailija: true, storeTokens: true })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    cy.task('db:ensureKoejaksoTyoskentelyjakso', { email: E2E_ERIKOISTUVA_EMAIL })
    // The trainee's request and clerk's review are setup; the final decision uses the UI.
    cy.apiRequest({
      method: 'POST',
      url: `${ERIKOISTUVAN_API}/vastuuhenkilonarvio`,
      form: true,
      body: {
        vastuuhenkilonArvioJson: JSON.stringify({
          erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
          erikoistuvanPuhelinnumero: ERIKOISTUVAN_PUHELIN,
          koejaksoHyvaksytty: null,
          perusteluHylkaamiselle: null,
          hylattyArviointiKaytyLapiKeskustellen: null
        })
      }
    }).then(({ status, body }) => {
      expect(status).to.eq(201)
      expect(body.id).to.be.a('number')
      expect(body.erikoistuvanKuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      arvio = body
    })
    cy.loginAsVirkailija(Cypress.env('virkailijaToken'))
    cy.then(() => cy.apiRequest({ method: 'GET', url: `${VIRKAILIJAN_API}/${arvio.id}` }))
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.koulutussopimusHyvaksytty).to.eq(true)
        expect(body.loppukeskustelu.lahiesimies.sopimusHyvaksytty).to.eq(true)
        return cy.apiRequest({
          method: 'PUT',
          url: VIRKAILIJAN_API,
          body: {
            ...body,
            virkailijanYhteenveto: VIRKAILIJAN_YHTEENVETO,
            lisatiedotVirkailijalta: VIRKAILIJAN_LISATIEDOT,
            virkailijanKorjausehdotus: null
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.virkailija.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        arvio = body
        tarkistaOdottaaPaatosta(arvio)
      })
    cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
    const arviointipyynto = cy.spy().as('arviointipyynto')
    cy.intercept('PUT', `**${API}`, (request) => {
      arviointipyynto(request.body)
    }).as('tallennaPaatos')
    cy.then(() => avaaArvio('arvioEnnenPaatosta')).then((tallennettu) => {
      tarkistaOdottaaPaatosta(tallennettu)
      expect(tallennettu.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
    })
    hyvaksyPainike().should('be.visible').and('not.be.disabled')
    kentta(PAATOS).find('input[type="radio"]:checked').should('not.exist')
  })

  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    // Final decisions also create PDFs attached to the study right, not to the form.
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('vaatii yhteystiedot ja päätöksen sekä hylkäykselle perustelun ja käydyn keskustelun', () => {
    sahkoposti().clear()
    puhelin().clear()
    hyvaksyPainike().click()
    sahkoposti().should('have.class', 'is-invalid')
    puhelin().should('have.class', 'is-invalid')
    kentta(PAATOS).find('.invalid-feedback:visible').should('contain.text', 'Pakollinen tieto')
    tarkistaEtteiLahetetty()

    valitsePaatos(true)
    sahkoposti().type('virheellinen-osoite')
    puhelin().type('123')
    hyvaksyPainike().click()
    sahkoposti().should('have.class', 'is-invalid')
    puhelin().should('have.class', 'is-invalid')
    kentta('Sähköpostiosoite').contains('Sähköpostiosoite ei ole kelvollinen').should('be.visible')
    kentta('Matkapuhelinnumero')
      .contains('Tarkista, että puhelinnumero on muodossa +358501234567')
      .should('be.visible')
    tarkistaEtteiLahetetty()

    taytaYhteystiedot()
    valitsePaatos(false)
    keskusteluvalinta().should('not.be.checked')
    hyvaksyPainike().click()
    kentta(PERUSTELU).find('textarea').should('have.class', 'is-invalid')
    kentta(KESKUSTELTU).find('.invalid-feedback:visible').should('be.visible')
    tarkistaEtteiLahetetty()

    // Each rejection requirement must block submission independently of the other.
    kentta(PERUSTELU).find('textarea').type(HYLKAYKSEN_PERUSTELU)
    hyvaksyPainike().click()
    kentta(PERUSTELU).find('textarea').should('not.have.class', 'is-invalid')
    kentta(KESKUSTELTU).find('.invalid-feedback:visible').should('be.visible')
    tarkistaEtteiLahetetty()

    kentta(PERUSTELU).find('textarea').clear()
    kentta(KESKUSTELTU)
      .contains('label', /^\s*Kyllä\s*$/)
      .click()
    keskusteluvalinta().should('be.checked')
    hyvaksyPainike().click()
    kentta(PERUSTELU).find('textarea').should('have.class', 'is-invalid')
    tarkistaEtteiLahetetty()
    cy.apiRequest({ method: 'GET', url: `${API}/${arvio.id}` }).then(({ status, body }) => {
      expect(status).to.eq(200)
      tarkistaOdottaaPaatosta(body)
    })
  })

  const paatokset = [
    {
      nimi: 'hyväksyy koejakson vasta vahvistuksesta ja näyttää päätöksen erikoistuvalle',
      hyvaksytty: true
    },
    {
      nimi: 'säilyttää hylkäyksen perustelun ja keskustelumerkinnän myös erikoistuvan näkymässä',
      hyvaksytty: false
    }
  ]

  paatokset.forEach(({ nimi, hyvaksytty }) => {
    it(nimi, () => {
      let kuittausaika: string
      taytaYhteystiedot()
      valitsePaatos(hyvaksytty)
      if (!hyvaksytty) {
        kentta(PERUSTELU).find('textarea').type(HYLKAYKSEN_PERUSTELU)
        kentta(KESKUSTELTU)
          .contains('label', /^\s*Kyllä\s*$/)
          .click()
      }
      hyvaksyPainike().click()
      cy.get(VAHVISTUS).should('be.visible').and('contain.text', 'Vahvista lomakkeen lähetys')
      cy.get('@arviointipyynto').should('not.have.been.called')
      cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
      tarkistaEtteiLahetetty()
      sahkoposti().should('have.value', VASTUUHENKILO_EMAIL)
      puhelin().should('have.value', VASTUUHENKILON_PUHELIN)
      kentta(PAATOS).find('input[type="radio"]:checked').should('have.value', String(hyvaksytty))
      if (!hyvaksytty) {
        kentta(PERUSTELU).find('textarea').should('have.value', HYLKAYKSEN_PERUSTELU)
        keskusteluvalinta().should('be.checked')
      }
      cy.apiRequest({ method: 'GET', url: `${API}/${arvio.id}` }).then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaOdottaaPaatosta(body)
      })

      hyvaksyPainike().should('not.be.disabled').click()
      cy.get(VAHVISTUS)
        .should('be.visible')
        .contains('button', /^\s*Hyväksy\s*$/)
        .click()
      // The real backend generates the assessment PDF before completing the response.
      cy.wait('@tallennaPaatos', { responseTimeout: 120000 }).then(({ request, response }) => {
        tarkistaAiemmatTiedot(request.body)
        expect(request.body).to.include({
          koejaksoHyvaksytty: hyvaksytty,
          vastuuhenkilonSahkoposti: VASTUUHENKILO_EMAIL,
          vastuuhenkilonPuhelinnumero: VASTUUHENKILON_PUHELIN,
          perusteluHylkaamiselle: hyvaksytty ? null : HYLKAYKSEN_PERUSTELU,
          hylattyArviointiKaytyLapiKeskustellen: hyvaksytty ? null : true
        })
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.vastuuhenkilo.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        kuittausaika = response?.body.vastuuhenkilo.kuittausaika
        tarkistaTallennettuPaatos(response?.body, hyvaksytty, kuittausaika)
      })
      cy.get('@arviointipyynto').should('have.been.calledOnce')
      cy.location('pathname').should('eq', '/koejakso')

      avaaArvio('arvioPaatoksenJalkeen').then((tallennettu) => {
        tarkistaTallennettuPaatos(tallennettu, hyvaksytty, kuittausaika)
        // HYVAKSYTTY describes the signed form; koejaksoHyvaksytty holds the actual decision.
        expect(tallennettu.tila).to.eq('HYVAKSYTTY')
      })
      tarkistaPaatosNakyvissa(hyvaksytty)
      cy.get(SIVU).contains('p', VASTUUHENKILO_EMAIL).should('be.visible')
      cy.get(SIVU).contains('p', VASTUUHENKILON_PUHELIN).should('be.visible')
      hyvaksyPainike().should('not.exist')
      cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')

      cy.loginAsErikoistuva()
      cy.intercept({ method: 'GET', url: `**${ERIKOISTUVAN_API}`, times: 1 }).as(
        'erikoistuvanPaatos'
      )
      cy.visit('/koejakso/vastuuhenkilon-arvio')
      cy.wait('@erikoistuvanPaatos').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.vastuuhenkilonArvionTila).to.eq('HYVAKSYTTY')
        tarkistaTallennettuPaatos(response?.body.vastuuhenkilonArvio, hyvaksytty, kuittausaika)
      })
      tarkistaPaatosNakyvissa(hyvaksytty)
      cy.get(SIVU)
        .contains('button', /^\s*Lähetä\s*$/)
        .should('not.exist')
      cy.get(SIVU).contains('.alert-dark', 'Odottaa vastuuhenkilön arviointia.').should('not.exist')
    })
  })
})
