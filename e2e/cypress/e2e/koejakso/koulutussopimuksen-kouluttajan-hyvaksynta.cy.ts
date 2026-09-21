import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL } from '../../support/commands/credentials'

const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'
const KOULUTTAJAN_OSIO = '.kouluttaja-section'
const LOMAKE = '.koulutussopimus'
const VAHVISTUS = '#confirm-send-kouluttaja'
const HYVAKSYNTA_URL = '**/kouluttaja/koejakso/koulutussopimus'
const KOULUTTAJAN_TIEDOT = [
  { label: 'Kouluttajan nimike', property: 'nimike', value: 'Erikoislääkäri' },
  { label: 'Toimipaikka', property: 'toimipaikka', value: 'E2E Kouluttajan toimipaikka' },
  { label: 'Lähiosoite', property: 'lahiosoite', value: 'Testikatu 1' },
  { label: 'Postitoimipaikka', property: 'postitoimipaikka', value: '00100 Helsinki' },
  { label: 'Sähköpostiosoite', property: 'sahkoposti', value: KOULUTTAJA_EMAIL },
  { label: 'Matkapuhelinnumero', property: 'puhelin', value: '+358401234568' }
] as const

const kouluttajanKentta = (label: string) =>
  cy.get(KOULUTTAJAN_OSIO).contains('label', label).closest('.form-group').find('input')

const taytaKouluttajanTiedot = () => {
  KOULUTTAJAN_TIEDOT.forEach(({ label, value }) => {
    kouluttajanKentta(label).clear().type(value)
  })
}

const tarkistaEtteiHyvaksyntaaLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@hyvaksyntapyynto').should('not.have.been.called')
}

describe('Koulutussopimuksen hyväksyminen kouluttajan käyttöliittymässä', () => {
  let sopimusId: number

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.submitKoulutussopimusViaUi(KOULUTTAJA_NIMI).then((id) => {
      sopimusId = id
      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))

      // Spy on real requests; approval responses are not stubbed.
      const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
      cy.intercept('PUT', HYVAKSYNTA_URL, (request) => {
        hyvaksyntapyynto(request.body)
      }).as('hyvaksyKoulutussopimus')
      cy.intercept('GET', `**/kouluttaja/koejakso/koulutussopimus/${id}`).as('haeKoulutussopimus')
      cy.visit(`/koejakso/koulutussopimus/${id}`)
      cy.wait('@haeKoulutussopimus').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.id).to.eq(id)
        expect(response?.body.kouluttajat).to.have.length(1)
        expect(response?.body.kouluttajat[0].kayttajaId).to.eq(Cypress.env('kouluttajaId'))
        expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(false)
      })
      cy.get(KOULUTTAJAN_OSIO).contains(KOULUTTAJA_NIMI).should('be.visible')
      cy.get(KOULUTTAJAN_OSIO).find('input').should('have.length', KOULUTTAJAN_TIEDOT.length)
    })
  })

  it('estää hyväksynnän puuttuvilla tiedoilla sekä virheellisellä sähköpostilla ja puhelinnumerolla', () => {
    KOULUTTAJAN_TIEDOT.forEach(({ label }) => {
      kouluttajanKentta(label).clear()
    })
    cy.get(LOMAKE).contains('button', 'Hyväksy ja lähetä').click()

    KOULUTTAJAN_TIEDOT.forEach(({ label }) => {
      kouluttajanKentta(label).should('have.class', 'is-invalid')
    })
    tarkistaEtteiHyvaksyntaaLahetetty()

    taytaKouluttajanTiedot()
    kouluttajanKentta('Sähköpostiosoite').clear().type('virheellinen-osoite')
    kouluttajanKentta('Matkapuhelinnumero').clear().type('123')
    cy.get(LOMAKE).contains('button', 'Hyväksy ja lähetä').click()

    kouluttajanKentta('Sähköpostiosoite').should('have.class', 'is-invalid')
    kouluttajanKentta('Matkapuhelinnumero').should('have.class', 'is-invalid')
    cy.get(KOULUTTAJAN_OSIO).contains('Sähköpostiosoite ei ole kelvollinen').should('be.visible')
    cy.get(KOULUTTAJAN_OSIO)
      .contains('Tarkista, että puhelinnumero on muodossa +358501234567')
      .should('be.visible')
    tarkistaEtteiHyvaksyntaaLahetetty()

    cy.apiRequest({
      method: 'GET',
      url: `/api/kouluttaja/koejakso/koulutussopimus/${sopimusId}`
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.kouluttajat[0].sopimusHyvaksytty).to.eq(false)
      expect(body.kouluttajat[0].kuittausaika).to.be.null
    })
  })

  it('lähettää hyväksynnän vasta vahvistuksesta ja säilyttää tiedot uudelleen avattaessa', () => {
    taytaKouluttajanTiedot()
    cy.get(LOMAKE).contains('button', 'Hyväksy ja lähetä').click()
    cy.get(VAHVISTUS).should('be.visible')
    cy.get('@hyvaksyntapyynto').should('not.have.been.called')

    cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
    tarkistaEtteiHyvaksyntaaLahetetty()
    KOULUTTAJAN_TIEDOT.forEach(({ label, value }) => {
      kouluttajanKentta(label).should('have.value', value)
    })
    cy.apiRequest({
      method: 'GET',
      url: `/api/kouluttaja/koejakso/koulutussopimus/${sopimusId}`
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      expect(body.kouluttajat[0].sopimusHyvaksytty).to.eq(false)
      expect(body.kouluttajat[0].kuittausaika).to.be.null
    })

    cy.get(LOMAKE).contains('button', 'Hyväksy ja lähetä').click()
    cy.get(VAHVISTUS).should('be.visible').contains('button', 'Hyväksy ja lähetä').click()
    cy.wait('@hyvaksyKoulutussopimus').then(({ request, response }) => {
      expect(request.body.id).to.eq(sopimusId)
      expect(request.body.kouluttajat).to.have.length(1)
      expect(request.body.kouluttajat[0].kayttajaId).to.eq(Cypress.env('kouluttajaId'))
      KOULUTTAJAN_TIEDOT.forEach(({ property, value }) => {
        expect(request.body.kouluttajat[0][property], property).to.eq(value)
      })
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      expect(response?.body.kouluttajat[0].kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
    })
    cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
    cy.location('pathname').should('eq', '/koejakso')

    // A fresh page load must show persisted data and no second approval action.
    cy.visit(`/koejakso/koulutussopimus/${sopimusId}`)
    cy.wait('@haeKoulutussopimus').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      expect(response?.body.kouluttajat[0].kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      KOULUTTAJAN_TIEDOT.forEach(({ property, value }) => {
        expect(response?.body.kouluttajat[0][property], property).to.eq(value)
      })
      expect(response?.body.erikoistuvanSahkoposti).to.eq(E2E_ERIKOISTUVA_EMAIL)
      expect(response?.body.koulutuspaikat[0].nimi).to.eq('E2E Testisairaala')
      expect(response?.body.lahetetty).to.eq(true)
    })
    cy.get(LOMAKE).contains('Sopimus odottaa vastuuhenkilön toimia.').should('be.visible')
    cy.get(KOULUTTAJAN_OSIO).find('input').should('not.exist')
    KOULUTTAJAN_TIEDOT.forEach(({ value }) => {
      cy.get(KOULUTTAJAN_OSIO).contains('p', value).should('be.visible')
    })
    cy.get(LOMAKE).contains('button', 'Hyväksy ja lähetä').should('not.exist')
    cy.get(LOMAKE).contains('button', 'Palauta muokattavaksi').should('not.exist')

    // The responsible person can now see and process the trainer's decision.
    cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
    cy.intercept('GET', `**/vastuuhenkilo/koejakso/koulutussopimus/${sopimusId}`).as(
      'haeVastuuhenkilonKoulutussopimus'
    )
    cy.visit(`/koejakso/koulutussopimus/${sopimusId}`)
    cy.wait('@haeVastuuhenkilonKoulutussopimus').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.id).to.eq(sopimusId)
      expect(response?.body.kouluttajat[0].sopimusHyvaksytty).to.eq(true)
      expect(response?.body.vastuuhenkilo.sopimusHyvaksytty).to.eq(false)
    })
    cy.get(KOULUTTAJAN_OSIO).contains('p', 'E2E Kouluttajan toimipaikka').should('be.visible')
    cy.get(LOMAKE)
      .contains('button', /^Hyväksy$/)
      .should('be.visible')
  })
})
