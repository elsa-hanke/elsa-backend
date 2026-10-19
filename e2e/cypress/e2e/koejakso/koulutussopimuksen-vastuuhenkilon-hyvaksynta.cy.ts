import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL
} from '../../support/commands/credentials'

const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'
const VASTUUHENKILO_NIMI = 'Mia Ålands'
const LOMAKE = '.koulutussopimus'
const VAHVISTUS = '#confirm-send-vastuuhenkilo'
const HYVAKSYNTA_URL = '**/vastuuhenkilo/koejakso/koulutussopimus'
const HYVAKSYTTY_TEKSTI = 'Koejakson koulutussopimus on hyväksytty kaikkien osapuolten toimesta.'
const VASTUUHENKILON_TIEDOT = [
  { label: 'Sähköpostiosoite', property: 'sahkoposti', value: VASTUUHENKILO_EMAIL },
  { label: 'Matkapuhelinnumero', property: 'puhelin', value: '+358401234569' }
] as const

const vastuuhenkilonKentta = (label: string) =>
  cy.get(LOMAKE).contains('label', label).closest('.form-group').find('input')

// Vue slots preserve whitespace around the button label.
const hyvaksyPainike = () => cy.get(LOMAKE).contains('button[type="submit"]', /^\s*Hyväksy\s*$/)

const taytaVastuuhenkilonTiedot = () => {
  VASTUUHENKILON_TIEDOT.forEach(({ label, value }) => {
    vastuuhenkilonKentta(label).clear().type(value)
  })
}

const tarkistaEtteiHyvaksyntaaLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@hyvaksyntapyynto').should('not.have.been.called')
}

describe('Koulutussopimuksen hyväksyminen vastuuhenkilön käyttöliittymässä', () => {
  let sopimusId: number
  let kouluttajanKuittausaika: string

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.submitKoulutussopimusViaUi(KOULUTTAJA_NIMI).then((id) => {
      sopimusId = id

      // Trainer approval is setup; this spec exercises the responsible person's UI.
      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
      cy.apiRequest({
        method: 'GET',
        url: `/api/kouluttaja/koejakso/koulutussopimus/${id}`
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.kouluttajat).to.have.length(1)
        expect(body.kouluttajat[0].kayttajaId).to.eq(Cypress.env('kouluttajaId'))
        cy.apiRequest({
          method: 'PUT',
          url: '/api/kouluttaja/koejakso/koulutussopimus',
          body: {
            ...body,
            korjausehdotus: null,
            kouluttajat: [
              {
                ...body.kouluttajat[0],
                nimike: 'Erikoislääkäri',
                toimipaikka: 'E2E Kouluttajan toimipaikka',
                lahiosoite: 'Testikatu 1',
                postitoimipaikka: '00100 Helsinki',
                sahkoposti: KOULUTTAJA_EMAIL,
                puhelin: '+358401234568'
              }
            ]
          }
        }).then(({ status: approvalStatus, body: approvedBody }) => {
          expect(approvalStatus).to.eq(200)
          expect(approvedBody.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
          expect(approvedBody.kouluttajat[0].kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
          kouluttajanKuittausaika = approvedBody.kouluttajat[0].kuittausaika
          expect(approvedBody.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
        })
      })

      cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
      // Observe real approval requests without stubbing their responses.
      const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
      cy.intercept('PUT', HYVAKSYNTA_URL, (request) => {
        hyvaksyntapyynto(request.body)
      }).as('hyvaksyKoulutussopimus')
      cy.intercept('GET', `**/vastuuhenkilo/koejakso/koulutussopimus/${id}`).as(
        'haeKoulutussopimus'
      )
      cy.visit(`/koejakso/koulutussopimus/${id}`)
      cy.wait('@haeKoulutussopimus').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.id).to.eq(id)
        expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
        expect(response?.body.vastuuhenkilo.id).to.eq(Cypress.env('vastuuhenkiloId'))
        expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
        expect(response?.body.vastuuhenkilo.kuittausaika).to.be.null
      })
      cy.get(LOMAKE).contains('p', VASTUUHENKILO_NIMI).should('be.visible')
      cy.get(LOMAKE).find('input').should('have.length', VASTUUHENKILON_TIEDOT.length)
      hyvaksyPainike().should('be.visible').and('not.be.disabled')
    })
  })

  it('estää hyväksynnän puuttuvilla tai virheellisillä yhteystiedoilla', () => {
    VASTUUHENKILON_TIEDOT.forEach(({ label }) => {
      vastuuhenkilonKentta(label).clear()
    })
    hyvaksyPainike().click()
    VASTUUHENKILON_TIEDOT.forEach(({ label }) => {
      vastuuhenkilonKentta(label).should('have.class', 'is-invalid')
    })
    cy.get(LOMAKE).contains('Pakollinen tieto').should('be.visible')
    tarkistaEtteiHyvaksyntaaLahetetty()

    vastuuhenkilonKentta('Sähköpostiosoite').type('virheellinen-osoite')
    vastuuhenkilonKentta('Matkapuhelinnumero').type('123')
    hyvaksyPainike().click()
    VASTUUHENKILON_TIEDOT.forEach(({ label }) => {
      vastuuhenkilonKentta(label).should('have.class', 'is-invalid')
    })
    cy.get(LOMAKE).contains('Sähköpostiosoite ei ole kelvollinen').should('be.visible')
    cy.get(LOMAKE)
      .contains('Tarkista, että puhelinnumero on muodossa +358501234567')
      .should('be.visible')
    tarkistaEtteiHyvaksyntaaLahetetty()

    cy.apiRequest({
      method: 'GET',
      url: `/api/vastuuhenkilo/koejakso/koulutussopimus/${sopimusId}`
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
      expect(body.vastuuhenkilo.kuittausaika).to.be.null
    })
  })

  it('hyväksyy vasta vahvistuksesta ja näyttää hyväksytyn sopimuksen myös erikoistuvalle', () => {
    let vastuuhenkilonKuittausaika: string

    taytaVastuuhenkilonTiedot()
    hyvaksyPainike().click()
    cy.get(VAHVISTUS).should('be.visible')
    cy.get('@hyvaksyntapyynto').should('not.have.been.called')

    cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
    tarkistaEtteiHyvaksyntaaLahetetty()
    VASTUUHENKILON_TIEDOT.forEach(({ label, value }) => {
      vastuuhenkilonKentta(label).should('have.value', value)
    })
    cy.apiRequest({
      method: 'GET',
      url: `/api/vastuuhenkilo/koejakso/koulutussopimus/${sopimusId}`
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
      expect(body.vastuuhenkilo.kuittausaika).to.be.null
    })

    hyvaksyPainike().should('not.be.disabled').click()
    cy.get(VAHVISTUS)
      .should('be.visible')
      .contains('button', /^\s*Hyväksy\s*$/)
      .click()
    cy.wait('@hyvaksyKoulutussopimus').then(({ request, response }) => {
      expect(request.body.id).to.eq(sopimusId)
      expect(request.body.lahetetty).to.eq(true)
      expect(request.body.vastuuhenkilo.id).to.eq(Cypress.env('vastuuhenkiloId'))
      expect(request.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      VASTUUHENKILON_TIEDOT.forEach(({ property, value }) => {
        expect(request.body.vastuuhenkilo[property], property).to.eq(value)
      })
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      expect(response?.body.vastuuhenkilo.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      vastuuhenkilonKuittausaika = response?.body.vastuuhenkilo.kuittausaika
    })
    cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
    cy.location('pathname').should('eq', '/koejakso')

    // A fresh page load must retain approval and contact details and prevent another decision.
    cy.visit(`/koejakso/koulutussopimus/${sopimusId}`)
    cy.wait('@haeKoulutussopimus').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.id).to.eq(sopimusId)
      expect(response?.body.vastuuhenkilo.id).to.eq(Cypress.env('vastuuhenkiloId'))
      expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      expect(response?.body.vastuuhenkilo.kuittausaika).to.eq(vastuuhenkilonKuittausaika)
      VASTUUHENKILON_TIEDOT.forEach(({ property, value }) => {
        expect(response?.body.vastuuhenkilo[property], property).to.eq(value)
      })
      expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      expect(response?.body.kouluttajat[0].kuittausaika).to.eq(kouluttajanKuittausaika)
    })
    cy.get(LOMAKE).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
    VASTUUHENKILON_TIEDOT.forEach(({ value }) => {
      cy.get(LOMAKE).contains('p', value).should('be.visible')
    })
    cy.get(LOMAKE).find('input').should('not.exist')
    hyvaksyPainike().should('not.exist')
    cy.get(LOMAKE).contains('button', 'Palauta muokattavaksi').should('not.exist')

    cy.loginAsErikoistuva()
    cy.intercept('GET', '**/erikoistuva-laakari/koejakso').as('haeErikoistuvanKoejakso')
    cy.visit('/koejakso/koulutussopimus')
    cy.wait('@haeErikoistuvanKoejakso').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.koulutusSopimuksenTila).to.eq('HYVAKSYTTY')
      const sopimus = response?.body.koulutussopimus
      expect(sopimus.id).to.eq(sopimusId)
      expect(sopimus.vastuuhenkilo.id).to.eq(Cypress.env('vastuuhenkiloId'))
      expect(sopimus.vastuuhenkilo.sopimusHyvaksytty).to.eq(true)
      expect(sopimus.vastuuhenkilo.kuittausaika).to.eq(vastuuhenkilonKuittausaika)
      expect(sopimus.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      expect(sopimus.kouluttajat[0].kuittausaika).to.eq(kouluttajanKuittausaika)
      expect(sopimus.erikoistuvanSahkoposti).to.eq(E2E_ERIKOISTUVA_EMAIL)
      expect(sopimus.koulutuspaikat[0].nimi).to.eq('E2E Testisairaala')
    })
    cy.get(LOMAKE).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
    cy.get(LOMAKE).contains('p', 'E2E Testisairaala').should('be.visible')
    cy.get(LOMAKE)
      .find('.hyvaksynta-pvm')
      .should('have.length', 3)
      .last()
      .parent()
      .contains('p', VASTUUHENKILO_NIMI)
      .should('be.visible')
    cy.get(LOMAKE).find('input').should('not.exist')
    cy.get(LOMAKE).contains('button', 'Hyväksy ja lähetä').should('not.exist')
    cy.get(LOMAKE).contains('button', 'Tyhjennä lomake').should('not.exist')
  })
})
