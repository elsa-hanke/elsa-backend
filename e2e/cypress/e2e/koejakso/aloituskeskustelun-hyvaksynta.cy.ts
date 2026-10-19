import {
  E2E_ERIKOISTUVA_EMAIL,
  ESIHENKILÖ_EMAIL,
  KOULUTTAJA_EMAIL
} from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-send'
const API = '/api/kouluttaja/koejakso/aloituskeskustelu'
const TAVOITTEET = 'E2E aloituskeskustelun yhteiset osaamistavoitteet'
const HYVAKSYTTY_TEKSTI = 'Aloituskeskustelu on hyväksytty kaikkien osapuolten toimesta.'
const ESIHENKILO_NIMI = 'Tessa Testilä'

type Hyvaksyja = {
  id: number
  sopimusHyvaksytty: boolean
  kuittausaika: string | null
}

type Aloituskeskustelu = {
  id: number
  lahetetty: boolean
  erikoistuvanKuittausaika: string
  erikoistuvanSahkoposti: string
  koejaksonSuorituspaikka: string
  koejaksonOsaamistavoitteet: string
  lahikouluttaja: Hyvaksyja
  lahiesimies: Hyvaksyja
}

const paikallinenPaiva = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')}`

const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy ja lähetä\s*$/)

const valiarvioinninLinkki = () =>
  cy.get(SIVU).contains('h2', 'Väliarviointi').parent().contains('a', 'Pyydä arviointia')

const seuraaHyvaksyntaa = () => {
  const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
  // Observe real browser requests; do not stub the approval response.
  cy.intercept('PUT', `**${API}`, (request) => {
    hyvaksyntapyynto(request.body)
  }).as('hyvaksyAloituskeskustelu')
}

const peruutaVahvistus = () => {
  cy.get(VAHVISTUS).should('be.visible')
  cy.get('@hyvaksyntapyynto').should('not.have.been.called')
  cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@hyvaksyntapyynto').should('not.have.been.called')
  hyvaksyPainike().should('be.visible').and('not.be.disabled')
}

const vahvistaHyvaksynta = () => {
  hyvaksyPainike().click()
  cy.get(VAHVISTUS)
    .should('be.visible')
    .contains('button', /^\s*Hyväksy ja lähetä\s*$/)
    .click()
  return cy.wait('@hyvaksyAloituskeskustelu')
}

describe('Aloituskeskustelun hyväksyminen kouluttajan ja lähiesihenkilön käyttöliittymässä', () => {
  let aloituskeskustelu: Aloituskeskustelu
  let esihenkiloId: number
  let esihenkiloToken: string

  const tarkistaHyvaksynnat = (
    lomake: Aloituskeskustelu,
    kouluttajaHyvaksytty: boolean,
    esihenkiloHyvaksytty: boolean
  ) => {
    expect(lomake).to.include({
      id: aloituskeskustelu.id,
      lahetetty: true,
      erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
      koejaksonSuorituspaikka: 'E2E Testisairaala',
      koejaksonOsaamistavoitteet: TAVOITTEET
    })
    expect(lomake.erikoistuvanKuittausaika).to.eq(aloituskeskustelu.erikoistuvanKuittausaika)
    expect(lomake.lahikouluttaja).to.include({
      id: Cypress.env('kouluttajaId'),
      sopimusHyvaksytty: kouluttajaHyvaksytty
    })
    expect(lomake.lahiesimies).to.include({
      id: esihenkiloId,
      sopimusHyvaksytty: esihenkiloHyvaksytty
    })
    for (const hyvaksyja of [lomake.lahikouluttaja, lomake.lahiesimies]) {
      if (hyvaksyja.sopimusHyvaksytty) {
        expect(hyvaksyja.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      } else {
        expect(hyvaksyja.kuittausaika).to.be.null
      }
    }
  }

  const avaaLomake = () => {
    cy.intercept('GET', `**${API}/${aloituskeskustelu.id}`).as('haeAloituskeskustelu')
    cy.visit(`/koejakso/aloituskeskustelu/${aloituskeskustelu.id}`)
    return cy.wait('@haeAloituskeskustelu').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      return response?.body as Aloituskeskustelu
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
    // Only the prerequisite contract is approved in the database.
    cy.task('db:ensureKoulutussopimusHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    cy.apiRequest({ method: 'GET', url: '/api/erikoistuva-laakari/koejakso' }).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.koulutusSopimuksenTila).to.eq('HYVAKSYTTY')
        const sopimus = body.koulutussopimus
        const alku = new Date()
        const loppu = new Date(alku)
        loppu.setFullYear(loppu.getFullYear() + 1)

        // Resident submission is setup; the decisions below are made through the UI.
        cy.apiRequest({
          method: 'POST',
          url: '/api/erikoistuva-laakari/koejakso/aloituskeskustelu',
          body: {
            erikoistuvanNimi: sopimus.erikoistuvanNimi,
            erikoistuvanErikoisala: sopimus.erikoistuvanErikoisala,
            erikoistuvanYliopisto: sopimus.erikoistuvanYliopisto,
            erikoistuvanOpiskelijatunnus: sopimus.erikoistuvanOpiskelijatunnus,
            erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
            koejaksonSuorituspaikka: 'E2E Testisairaala',
            koejaksonAlkamispaiva: paikallinenPaiva(alku),
            koejaksonPaattymispaiva: paikallinenPaiva(loppu),
            suoritettuKokoaikatyossa: true,
            koejaksonOsaamistavoitteet: TAVOITTEET,
            lahikouluttaja: { id: Cypress.env('kouluttajaId'), sopimusHyvaksytty: false },
            lahiesimies: { id: esihenkiloId, sopimusHyvaksytty: false },
            lahetetty: true
          }
        }).then(({ status: createdStatus, body: createdBody }) => {
          expect(createdStatus).to.eq(201)
          expect(createdBody.id).to.be.a('number')
          expect(createdBody.erikoistuvanKuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
          aloituskeskustelu = createdBody
          tarkistaHyvaksynnat(aloituskeskustelu, false, false)
        })
      }
    )
  })

  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
  })

  it('kouluttaja hyväksyy vahvistuksesta ja jättää lähiesihenkilön hyväksynnän odottamaan', () => {
    let kouluttajanKuittausaika: string
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    seuraaHyvaksyntaa()
    avaaLomake().then((lomake) => tarkistaHyvaksynnat(lomake, false, false))
    cy.get(SIVU).contains('p', TAVOITTEET).should('be.visible')
    hyvaksyPainike().click()
    cy.get(VAHVISTUS)
      .contains('Lähetyksen jälkeen lomake menee lähiesihenkilölle')
      .should('be.visible')
    peruutaVahvistus()
    cy.apiRequest({ method: 'GET', url: `${API}/${aloituskeskustelu.id}` }).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaHyvaksynnat(body, false, false)
      }
    )

    vahvistaHyvaksynta().then(({ request, response }) => {
      expect(request.body.id).to.eq(aloituskeskustelu.id)
      expect(request.body.lahikouluttaja.sopimusHyvaksytty).to.eq(true)
      expect(request.body.lahikouluttaja.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      expect(request.body.lahiesimies).to.include({ id: esihenkiloId, sopimusHyvaksytty: false })
      expect(response?.statusCode).to.eq(200)
      tarkistaHyvaksynnat(response?.body, true, false)
      kouluttajanKuittausaika = response?.body.lahikouluttaja.kuittausaika
    })
    cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
    cy.location('pathname').should('eq', '/koejakso')
    avaaLomake().then((lomake) => {
      tarkistaHyvaksynnat(lomake, true, false)
      expect(lomake.lahikouluttaja.kuittausaika).to.eq(kouluttajanKuittausaika)
    })
    cy.get(SIVU).contains('Aloituskeskustelu odottaa lähiesihenkilön toimia.').should('be.visible')
    hyvaksyPainike().should('not.exist')
    cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')

    cy.loginAsErikoistuva()
    cy.intercept('GET', '**/api/erikoistuva-laakari/koejakso').as('haeErikoistuvanKoejakso')
    cy.visit('/koejakso')
    cy.wait('@haeErikoistuvanKoejakso').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      tarkistaHyvaksynnat(response?.body.aloituskeskustelu, true, false)
      expect(response?.body.aloituskeskustelunTila).to.eq('ODOTTAA_ESIMIEHEN_HYVAKSYNTAA')
      expect(response?.body.valiarvioinninTila).to.eq('EI_AKTIIVINEN')
    })
    valiarvioinninLinkki().should('have.attr', 'aria-disabled', 'true')
  })

  it('lähiesihenkilö vahvistaa hyväksynnän ja avaa erikoistuvalle väliarvioinnin', () => {
    let kouluttajanKuittausaika: string
    let esihenkilonKuittausaika: string
    // Trainer approval was covered above; prepare this case independently.
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    cy.apiRequest({ method: 'PUT', url: API, body: aloituskeskustelu }).then(({ status, body }) => {
      expect(status).to.eq(200)
      tarkistaHyvaksynnat(body, true, false)
      kouluttajanKuittausaika = body.lahikouluttaja.kuittausaika
    })

    cy.loginAsEsihenkilo(esihenkiloToken)
    seuraaHyvaksyntaa()
    avaaLomake().then((lomake) => tarkistaHyvaksynnat(lomake, true, false))
    cy.get(SIVU).contains('p', TAVOITTEET).should('be.visible')
    hyvaksyPainike().click()
    cy.get(VAHVISTUS).contains('on hyväksytty kaikkien osapuolten toimesta.').should('be.visible')
    peruutaVahvistus()
    cy.apiRequest({ method: 'GET', url: `${API}/${aloituskeskustelu.id}` }).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaHyvaksynnat(body, true, false)
        expect(body.lahikouluttaja.kuittausaika).to.eq(kouluttajanKuittausaika)
      }
    )

    vahvistaHyvaksynta().then(({ request, response }) => {
      expect(request.body.id).to.eq(aloituskeskustelu.id)
      expect(request.body.lahiesimies.sopimusHyvaksytty).to.eq(true)
      expect(request.body.lahiesimies.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
      expect(request.body.lahikouluttaja).to.include({
        id: Cypress.env('kouluttajaId'),
        sopimusHyvaksytty: true,
        kuittausaika: kouluttajanKuittausaika
      })
      expect(response?.statusCode).to.eq(200)
      tarkistaHyvaksynnat(response?.body, true, true)
      esihenkilonKuittausaika = response?.body.lahiesimies.kuittausaika
    })
    cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
    cy.location('pathname').should('eq', '/koejakso')
    avaaLomake().then((lomake) => {
      tarkistaHyvaksynnat(lomake, true, true)
      expect(lomake.lahikouluttaja.kuittausaika).to.eq(kouluttajanKuittausaika)
      expect(lomake.lahiesimies.kuittausaika).to.eq(esihenkilonKuittausaika)
    })
    cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
    hyvaksyPainike().should('not.exist')
    cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')

    cy.loginAsErikoistuva()
    cy.intercept('GET', '**/api/erikoistuva-laakari/koejakso').as('haeErikoistuvanKoejakso')
    cy.visit('/koejakso/aloituskeskustelu')
    cy.wait('@haeErikoistuvanKoejakso').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      tarkistaHyvaksynnat(response?.body.aloituskeskustelu, true, true)
      expect(response?.body.aloituskeskustelu.lahiesimies.kuittausaika).to.eq(
        esihenkilonKuittausaika
      )
      expect(response?.body.aloituskeskustelunTila).to.eq('HYVAKSYTTY')
      expect(response?.body.valiarvioinninTila).to.eq('UUSI')
    })
    cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
    cy.get(SIVU).contains('p', TAVOITTEET).should('be.visible')
    cy.get(SIVU)
      .find('.hyvaksynta-pvm')
      .should('have.length', 3)
      .last()
      .parent()
      .contains('p', ESIHENKILO_NIMI)
      .should('be.visible')
    cy.get(SIVU).contains('button', 'Tyhjennä lomake').should('not.exist')

    cy.visit('/koejakso')
    cy.wait('@haeErikoistuvanKoejakso')
    valiarvioinninLinkki().should('be.visible').and('not.have.attr', 'aria-disabled')
    // Attribute assertions change the subject; query the link again before clicking.
    valiarvioinninLinkki().click()
    cy.location('pathname').should('eq', '/koejakso/valiarviointi')
    cy.get(SIVU).contains('label', 'Kouluttaja').should('be.visible')
  })
})
