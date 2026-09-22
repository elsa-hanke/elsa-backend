import {
  E2E_ERIKOISTUVA_EMAIL,
  ESIHENKILÖ_EMAIL,
  KOULUTTAJA_EMAIL
} from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-send'
const API = '/api/kouluttaja/koejakso/valiarviointi'
const EDISTYMINEN = 'Edistyminen on ollut sovittujen osaamistavoitteiden mukaista'
const KATEGORIAT =
  'Keskustelu ja mahdolliset toimenpiteet ovat tarpeen ennen koejakson hyväksymistä, liittyen:'
const TOIMENPITEET = 'Selvitys keskustelussa sovituista kehittämistoimenpiteistä'
const VAHVUUDET = 'E2E: Erikoistuja kohtaa potilaat rauhallisesti ja kuuntelee huolellisesti.'
const KEHITTAMISTOIMENPITEET = 'E2E: Sovittiin viikoittaisesta ohjauskeskustelusta.'
const MUU_KATEGORIA = 'E2E: Ajankäytön suunnittelu vastaanotolla'

type Hyvaksyja = {
  id: number
  sopimusHyvaksytty: boolean
  kuittausaika: string | null
}

type ArvionSisalto = {
  edistyminenTavoitteidenMukaista: boolean | null
  kehittamistoimenpideKategoriat: string[] | null
  muuKategoria: string | null
  vahvuudet: string | null
  kehittamistoimenpiteet: string | null
}

type Valiarviointi = ArvionSisalto & {
  id: number
  erikoistuvanKuittausaika: string | null
  lahikouluttaja: Hyvaksyja
  lahiesimies: Hyvaksyja
}

const kentta = (label: string) => cy.get(SIVU).contains('label', label).closest('.form-group')
const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy ja lähetä\s*$/)

const tarkistaSisalto = (lomake: ArvionSisalto, odotettu: ArvionSisalto) => {
  expect(lomake).to.include({
    edistyminenTavoitteidenMukaista: odotettu.edistyminenTavoitteidenMukaista,
    muuKategoria: odotettu.muuKategoria,
    vahvuudet: odotettu.vahvuudet,
    kehittamistoimenpiteet: odotettu.kehittamistoimenpiteet
  })
  // An empty JPA collection may be returned as [] after reopening, even if PUT sent null.
  expect(lomake.kehittamistoimenpideKategoriat ?? []).to.have.members(
    odotettu.kehittamistoimenpideKategoriat ?? []
  )
}

const tarkistaEtteiLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@hyvaksyntapyynto').should('not.have.been.called')
}

const taytaTekstikentat = () => {
  kentta('Vahvuudet').find('textarea').clear().type(VAHVUUDET)
  kentta(TOIMENPITEET).find('textarea').clear().type(KEHITTAMISTOIMENPITEET)
}

describe('Väliarvioinnin täyttäminen ja hyväksyminen kouluttajan käyttöliittymässä', () => {
  let valiarviointi: Valiarviointi
  let esihenkiloId: number

  const tarkistaHyvaksynnat = (lomake: Valiarviointi, kouluttajaHyvaksytty: boolean) => {
    expect(lomake.id).to.eq(valiarviointi.id)
    expect(lomake.erikoistuvanKuittausaika).to.eq(valiarviointi.erikoistuvanKuittausaika)
    expect(lomake.lahikouluttaja).to.include({
      id: Cypress.env('kouluttajaId'),
      sopimusHyvaksytty: kouluttajaHyvaksytty
    })
    expect(lomake.lahiesimies).to.include({
      id: esihenkiloId,
      sopimusHyvaksytty: false,
      kuittausaika: null
    })
    if (kouluttajaHyvaksytty) {
      expect(lomake.lahikouluttaja.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
    } else {
      expect(lomake.lahikouluttaja.kuittausaika).to.be.null
    }
  }

  const avaaLomake = () => {
    cy.intercept('GET', `**${API}/${valiarviointi.id}`).as('haeValiarviointi')
    cy.visit(`/koejakso/valiarviointi/${valiarviointi.id}`)
    return cy.wait('@haeValiarviointi').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      return response?.body as Valiarviointi
    })
  }

  const peruutaVahvistus = () => {
    hyvaksyPainike().click()
    cy.get(VAHVISTUS)
      .contains('Lähetyksen jälkeen lomake menee lähiesihenkilölle')
      .should('be.visible')
    cy.get('@hyvaksyntapyynto').should('not.have.been.called')
    cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
    tarkistaEtteiLahetetty()
    hyvaksyPainike().should('be.visible').and('not.be.disabled')
    cy.apiRequest({ method: 'GET', url: `${API}/${valiarviointi.id}` }).then(({ status, body }) => {
      expect(status).to.eq(200)
      tarkistaHyvaksynnat(body, false)
      tarkistaSisalto(body, valiarviointi)
    })
    kentta('Vahvuudet').find('textarea').should('have.value', VAHVUUDET)
    kentta(TOIMENPITEET).find('textarea').should('have.value', KEHITTAMISTOIMENPITEET)
  }

  const hyvaksyJaTarkistaTallennus = (arvio: ArvionSisalto) => {
    let kouluttajanKuittausaika: string | null
    hyvaksyPainike().click()
    cy.get(VAHVISTUS)
      .should('be.visible')
      .contains('button', /^\s*Hyväksy ja lähetä\s*$/)
      .click()
    cy.wait('@hyvaksyValiarviointi').then(({ request, response }) => {
      expect(request.body.id).to.eq(valiarviointi.id)
      tarkistaSisalto(request.body, arvio)
      if (arvio.edistyminenTavoitteidenMukaista) {
        expect(request.body.kehittamistoimenpideKategoriat).to.be.null
        expect(request.body.muuKategoria).to.be.null
      }
      expect(response?.statusCode).to.eq(200)
      tarkistaHyvaksynnat(response?.body, true)
      tarkistaSisalto(response?.body, arvio)
      kouluttajanKuittausaika = response?.body.lahikouluttaja.kuittausaika
    })
    cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
    cy.location('pathname').should('eq', '/koejakso')

    avaaLomake().then((lomake) => {
      tarkistaHyvaksynnat(lomake, true)
      tarkistaSisalto(lomake, arvio)
      expect(lomake.lahikouluttaja.kuittausaika).to.eq(kouluttajanKuittausaika)
    })
    cy.get(SIVU)
      .contains('h5', EDISTYMINEN)
      .next('p')
      .should(
        'contain.text',
        arvio.edistyminenTavoitteidenMukaista ? 'Kyllä' : 'Ei, huolenaiheita on'
      )
    cy.get(SIVU).contains('p', VAHVUUDET).should('be.visible')
    cy.get(SIVU).contains('p', KEHITTAMISTOIMENPITEET).should('be.visible')
    if (arvio.edistyminenTavoitteidenMukaista) {
      cy.get(SIVU).contains('h5', KATEGORIAT).should('not.exist')
    } else {
      cy.get(SIVU).contains('li', 'työssä suoriutumiseen').should('be.visible')
      cy.get(SIVU).contains('li', MUU_KATEGORIA).should('be.visible')
    }
    hyvaksyPainike().should('not.exist')
    cy.get(SIVU).find('textarea, input[type="radio"], input[type="checkbox"]').should('not.exist')

    cy.loginAsErikoistuva()
    cy.intercept('GET', '**/api/erikoistuva-laakari/koejakso').as('haeErikoistuvanKoejakso')
    cy.visit('/koejakso')
    cy.wait('@haeErikoistuvanKoejakso').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      tarkistaHyvaksynnat(response?.body.valiarviointi, true)
      tarkistaSisalto(response?.body.valiarviointi, arvio)
      expect(response?.body.valiarvioinninTila).to.eq('ODOTTAA_HYVAKSYNTAA')
      expect(response?.body.loppukeskustelunTila).to.eq('EI_AKTIIVINEN')
    })
    cy.get(SIVU)
      .contains('h2', 'Loppukeskustelu')
      .parent()
      .contains('a', 'Pyydä arviointia')
      .should('have.attr', 'aria-disabled', 'true')
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
    cy.task<{ kayttajaId: number | string }>('db:seedKouluttaja', {
      email: ESIHENKILÖ_EMAIL,
      etunimi: 'Tessa',
      sukunimi: 'Testilä'
    }).then((result) => {
      const kayttajaId = Number(result.kayttajaId)
      expect(Number.isSafeInteger(kayttajaId), 'supervisor database ID').to.eq(true)
      expect(kayttajaId).to.be.greaterThan(0).and.not.eq(Cypress.env('kouluttajaId'))
      esihenkiloId = kayttajaId
    })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    // Only earlier workflow stages are approved in the database.
    cy.task('db:ensureAloituskeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    cy.apiRequest({ method: 'GET', url: '/api/erikoistuva-laakari/koejakso' }).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.aloituskeskustelunTila).to.eq('HYVAKSYTTY')
        expect(body.valiarvioinninTila).to.eq('UUSI')
        const sopimus = body.koulutussopimus
        // Resident submission is setup; trainer validation and approval use the UI.
        cy.apiRequest({
          method: 'POST',
          url: '/api/erikoistuva-laakari/koejakso/valiarviointi',
          body: {
            erikoistuvanNimi: sopimus.erikoistuvanNimi,
            erikoistuvanErikoisala: sopimus.erikoistuvanErikoisala,
            erikoistuvanYliopisto: sopimus.erikoistuvanYliopisto,
            erikoistuvanOpiskelijatunnus: sopimus.erikoistuvanOpiskelijatunnus,
            edistyminenTavoitteidenMukaista: null,
            kehittamistoimenpideKategoriat: [],
            muuKategoria: '',
            vahvuudet: '',
            kehittamistoimenpiteet: '',
            korjausehdotus: '',
            lahikouluttaja: { id: Cypress.env('kouluttajaId'), sopimusHyvaksytty: false },
            lahiesimies: { id: esihenkiloId, sopimusHyvaksytty: false }
          }
        }).then(({ status: createdStatus, body: createdBody }) => {
          expect(createdStatus).to.eq(201)
          expect(createdBody.id).to.be.a('number')
          valiarviointi = createdBody
          tarkistaHyvaksynnat(valiarviointi, false)
          expect(valiarviointi.edistyminenTavoitteidenMukaista).to.be.null
        })
      }
    )
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
    cy.intercept('PUT', `**${API}`, (request) => {
      hyvaksyntapyynto(request.body)
    }).as('hyvaksyValiarviointi')
    cy.then(() => avaaLomake()).then((lomake) => {
      tarkistaHyvaksynnat(lomake, false)
      tarkistaSisalto(lomake, valiarviointi)
    })
  })

  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
  })

  it('vaatii edistymisarvion ja tallentaa myönteisen arvion vasta vahvistuksesta', () => {
    hyvaksyPainike().click()
    kentta(EDISTYMINEN).find('.invalid-feedback:visible').should('contain.text', 'Pakollinen tieto')
    tarkistaEtteiLahetetty()

    // Switching to Kyllä must discard concern categories entered before the switch.
    kentta(EDISTYMINEN).contains('label', 'Ei, huolenaiheita on').click()
    kentta(KATEGORIAT).contains('label', 'muuhun, kerro mihin:').click()
    kentta(KATEGORIAT).find('input[type="text"]').type(MUU_KATEGORIA)
    kentta(EDISTYMINEN)
      .contains('label', /^\s*Kyllä\s*$/)
      .click()
    cy.get(SIVU).contains('label', KATEGORIAT).should('not.exist')
    taytaTekstikentat()
    peruutaVahvistus()
    hyvaksyJaTarkistaTallennus({
      edistyminenTavoitteidenMukaista: true,
      kehittamistoimenpideKategoriat: [],
      muuKategoria: null,
      vahvuudet: VAHVUUDET,
      kehittamistoimenpiteet: KEHITTAMISTOIMENPITEET
    })
  })

  it('vaatii huolenaiheen ja muun kategorian kuvauksen sekä säilyttää ne uudelleen avattaessa', () => {
    kentta(EDISTYMINEN).contains('label', 'Ei, huolenaiheita on').click()
    hyvaksyPainike().click()
    kentta(KATEGORIAT).find('.invalid-feedback:visible').should('contain.text', 'Pakollinen tieto')
    tarkistaEtteiLahetetty()

    kentta(KATEGORIAT).contains('label', 'muuhun, kerro mihin:').click()
    hyvaksyPainike().click()
    kentta(KATEGORIAT).find('input[type="text"]').should('have.class', 'is-invalid')
    kentta(KATEGORIAT).find('.invalid-feedback:visible').should('contain.text', 'Pakollinen tieto')
    tarkistaEtteiLahetetty()

    kentta(KATEGORIAT).find('input[type="text"]').type(MUU_KATEGORIA)
    kentta(KATEGORIAT).contains('label', 'työssä suoriutumiseen').click()
    taytaTekstikentat()
    peruutaVahvistus()
    hyvaksyJaTarkistaTallennus({
      edistyminenTavoitteidenMukaista: false,
      kehittamistoimenpideKategoriat: ['TYOSSASUORIUTUMINEN', 'MUU'],
      muuKategoria: MUU_KATEGORIA,
      vahvuudet: VAHVUUDET,
      kehittamistoimenpiteet: KEHITTAMISTOIMENPITEET
    })
  })
})
