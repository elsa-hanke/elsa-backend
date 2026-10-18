import {
  E2E_ERIKOISTUVA_EMAIL,
  ESIHENKILÖ_EMAIL,
  KOULUTTAJA_EMAIL
} from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-send'
const API = '/api/kouluttaja/koejakso/valiarviointi'
const HYVAKSYTTY_TEKSTI = 'Väliarviointi on hyväksytty kaikkien osapuolten toimesta.'
const EDISTYMINEN = 'Edistyminen on ollut sovittujen osaamistavoitteiden mukaista'
const KATEGORIAT =
  'Keskustelu ja mahdolliset toimenpiteet ovat tarpeen ennen koejakson hyväksymistä, liittyen:'
const VAHVUUDET = 'E2E: Erikoistuja kohtaa potilaat rauhallisesti ja kuuntelee huolellisesti.'
const KEHITTAMISTOIMENPITEET = 'E2E: Sovittiin viikoittaisesta ohjauskeskustelusta.'
const MUU_KATEGORIA = 'E2E: Ajankäytön suunnittelu vastaanotolla'
const ESIHENKILO_NIMI = 'Tessa Testilä'

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
  korjausehdotus: string | null
  lahikouluttaja: Hyvaksyja
  lahiesimies: Hyvaksyja
}

const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy ja lähetä\s*$/)
const arviointilinkki = (otsikko: string) =>
  cy.get(SIVU).contains('h2', otsikko).parent().contains('a', 'Pyydä arviointia')

const tarkistaSisalto = (lomake: ArvionSisalto, odotettu: ArvionSisalto) => {
  expect(lomake).to.include({
    edistyminenTavoitteidenMukaista: odotettu.edistyminenTavoitteidenMukaista,
    muuKategoria: odotettu.muuKategoria,
    vahvuudet: odotettu.vahvuudet,
    kehittamistoimenpiteet: odotettu.kehittamistoimenpiteet
  })
  // The database may return [] for an empty collection even when PUT sent null.
  expect(lomake.kehittamistoimenpideKategoriat ?? []).to.have.members(
    odotettu.kehittamistoimenpideKategoriat ?? []
  )
}

const tarkistaArvioNakyvissa = (arvio: ArvionSisalto) => {
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
  cy.get(SIVU).find('textarea, input[type="radio"], input[type="checkbox"]').should('not.exist')
}

const arviot: { nimi: string; arvio: ArvionSisalto }[] = [
  {
    nimi: 'hyväksyy myönteisen arvion vasta vahvistuksesta ja avaa erikoistuvalle loppukeskustelun',
    arvio: {
      edistyminenTavoitteidenMukaista: true,
      kehittamistoimenpideKategoriat: [],
      muuKategoria: null,
      vahvuudet: VAHVUUDET,
      kehittamistoimenpiteet: KEHITTAMISTOIMENPITEET
    }
  },
  {
    nimi: 'säilyttää huolenaiheet hyväksynnässä ja avaa kehittämistoimenpiteet loppukeskustelun sijaan',
    arvio: {
      edistyminenTavoitteidenMukaista: false,
      kehittamistoimenpideKategoriat: ['TYOSSASUORIUTUMINEN', 'MUU'],
      muuKategoria: MUU_KATEGORIA,
      vahvuudet: VAHVUUDET,
      kehittamistoimenpiteet: KEHITTAMISTOIMENPITEET
    }
  }
]

describe('Väliarvioinnin hyväksyminen lähiesihenkilön käyttöliittymässä', () => {
  let valiarviointi: Valiarviointi
  let esihenkiloId: number
  let esihenkiloToken: string

  const tarkistaHyvaksynnat = (
    lomake: Valiarviointi,
    kouluttajanKuittausaika: string,
    esihenkilonKuittausaika: string | null
  ) => {
    expect(lomake.id).to.eq(valiarviointi.id)
    expect(lomake.erikoistuvanKuittausaika).to.eq(valiarviointi.erikoistuvanKuittausaika)
    expect(lomake.korjausehdotus).to.be.null
    expect(lomake.lahikouluttaja).to.include({
      id: Cypress.expose('kouluttajaId'),
      sopimusHyvaksytty: true,
      kuittausaika: kouluttajanKuittausaika
    })
    expect(lomake.lahiesimies).to.include({
      id: esihenkiloId,
      sopimusHyvaksytty: esihenkilonKuittausaika !== null,
      kuittausaika: esihenkilonKuittausaika
    })
  }

  const avaaLomake = () => {
    cy.intercept('GET', `**${API}/${valiarviointi.id}`).as('haeValiarviointi')
    cy.visit(`/koejakso/valiarviointi/${valiarviointi.id}`)
    return cy.wait('@haeValiarviointi').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      return response?.body as Valiarviointi
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
      expect(kayttajaId).to.be.greaterThan(0).and.not.eq(Cypress.expose('kouluttajaId'))
      expect(result.token).to.be.a('string').and.not.be.empty
      esihenkiloId = kayttajaId
      esihenkiloToken = result.token
    })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    // Earlier stages are prerequisites; each case creates its own pending midterm assessment.
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
            muuKategoria: null,
            vahvuudet: null,
            kehittamistoimenpiteet: null,
            korjausehdotus: null,
            lahikouluttaja: { id: Cypress.expose('kouluttajaId'), sopimusHyvaksytty: false },
            lahiesimies: { id: esihenkiloId, sopimusHyvaksytty: false }
          }
        }).then(({ status: createdStatus, body: createdBody }) => {
          expect(createdStatus).to.eq(201)
          expect(createdBody.id).to.be.a('number')
          expect(createdBody.lahikouluttaja).to.include({
            id: Cypress.expose('kouluttajaId'),
            sopimusHyvaksytty: false,
            kuittausaika: null
          })
          expect(createdBody.lahiesimies).to.include({
            id: esihenkiloId,
            sopimusHyvaksytty: false,
            kuittausaika: null
          })
          valiarviointi = createdBody
        })
      }
    )
  })

  after(() => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
  })

  arviot.forEach(({ nimi, arvio }) => {
    it(nimi, () => {
      let kouluttajanKuittausaika: string
      let esihenkilonKuittausaika: string

      // Trainer approval has its own UI spec; prepare this supervisor case through the API.
      cy.task<string | null>('tokens:get', 'kouluttajaToken', { log: false }).then((token) => cy.loginAsKouluttaja(token ?? undefined))
      cy.apiRequest({ method: 'PUT', url: API, body: { ...valiarviointi, ...arvio } }).then(
        ({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.lahikouluttaja.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
          kouluttajanKuittausaika = body.lahikouluttaja.kuittausaika
          tarkistaHyvaksynnat(body, kouluttajanKuittausaika, null)
          tarkistaSisalto(body, arvio)
        }
      )

      cy.loginAsEsihenkilo(esihenkiloToken)
      const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
      // Observe the supervisor's real browser request without stubbing its response.
      cy.intercept('PUT', `**${API}`, (request) => {
        hyvaksyntapyynto(request.body)
      }).as('hyvaksyValiarviointi')
      avaaLomake().then((lomake) => {
        tarkistaHyvaksynnat(lomake, kouluttajanKuittausaika, null)
        tarkistaSisalto(lomake, arvio)
      })
      tarkistaArvioNakyvissa(arvio)

      hyvaksyPainike().should('be.visible').and('not.be.disabled').click()
      cy.get(VAHVISTUS).should('be.visible')
      cy.get('@hyvaksyntapyynto').should('not.have.been.called')
      cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
      cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
      cy.get('@hyvaksyntapyynto').should('not.have.been.called')
      hyvaksyPainike().should('be.visible').and('not.be.disabled')
      cy.apiRequest({ method: 'GET', url: `${API}/${valiarviointi.id}` }).then(
        ({ status, body }) => {
          expect(status).to.eq(200)
          tarkistaHyvaksynnat(body, kouluttajanKuittausaika, null)
          tarkistaSisalto(body, arvio)
        }
      )

      hyvaksyPainike().click()
      cy.get(VAHVISTUS)
        .should('be.visible')
        .contains('button', /^\s*Hyväksy ja lähetä\s*$/)
        .click()
      cy.wait('@hyvaksyValiarviointi').then(({ request, response }) => {
        expect(request.body.id).to.eq(valiarviointi.id)
        expect(request.body.lahiesimies.id).to.eq(esihenkiloId)
        expect(request.body.korjausehdotus).to.be.null
        tarkistaSisalto(request.body, arvio)
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.lahiesimies.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        esihenkilonKuittausaika = response?.body.lahiesimies.kuittausaika
        tarkistaHyvaksynnat(response?.body, kouluttajanKuittausaika, esihenkilonKuittausaika)
        tarkistaSisalto(response?.body, arvio)
      })
      cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
      cy.location('pathname').should('eq', '/koejakso')

      avaaLomake().then((lomake) => {
        tarkistaHyvaksynnat(lomake, kouluttajanKuittausaika, esihenkilonKuittausaika)
        tarkistaSisalto(lomake, arvio)
      })
      cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
      tarkistaArvioNakyvissa(arvio)
      hyvaksyPainike().should('not.exist')
      cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')

      cy.loginAsErikoistuva()
      cy.intercept('GET', '**/api/erikoistuva-laakari/koejakso').as('haeErikoistuvanKoejakso')
      cy.visit('/koejakso/valiarviointi')
      cy.wait('@haeErikoistuvanKoejakso').then(({ response }) => {
        expect(response?.statusCode).to.eq(200)
        tarkistaHyvaksynnat(
          response?.body.valiarviointi,
          kouluttajanKuittausaika,
          esihenkilonKuittausaika
        )
        tarkistaSisalto(response?.body.valiarviointi, arvio)
        expect(response?.body.valiarvioinninTila).to.eq('HYVAKSYTTY')
        expect(response?.body.kehittamistoimenpiteidenTila).to.eq(
          arvio.edistyminenTavoitteidenMukaista ? 'EI_AKTIIVINEN' : 'UUSI'
        )
        expect(response?.body.loppukeskustelunTila).to.eq(
          arvio.edistyminenTavoitteidenMukaista ? 'UUSI' : 'EI_AKTIIVINEN'
        )
      })
      cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
      tarkistaArvioNakyvissa(arvio)
      cy.get(SIVU)
        .find('.hyvaksynta-pvm')
        .parent()
        .contains('p', ESIHENKILO_NIMI)
        .should('be.visible')
      cy.get(SIVU).contains('button', 'Tyhjennä lomake').should('not.exist')

      cy.visit('/koejakso')
      cy.wait('@haeErikoistuvanKoejakso').its('response.statusCode').should('eq', 200)
      const seuraavaVaihe = arvio.edistyminenTavoitteidenMukaista
        ? 'Loppukeskustelu'
        : 'Kehittämistoimenpiteet'
      const lukittuVaihe = arvio.edistyminenTavoitteidenMukaista
        ? 'Kehittämistoimenpiteet'
        : 'Loppukeskustelu'
      arviointilinkki(lukittuVaihe).should('be.visible').and('have.attr', 'aria-disabled', 'true')
      arviointilinkki(seuraavaVaihe).should('be.visible').and('not.have.attr', 'aria-disabled')
      // Attribute assertions change the subject; query the link again before clicking.
      arviointilinkki(seuraavaVaihe).click()
      cy.location('pathname').should(
        'eq',
        arvio.edistyminenTavoitteidenMukaista
          ? '/koejakso/loppukeskustelu'
          : '/koejakso/kehittamistoimenpiteet'
      )
      cy.get(SIVU)
        .contains('label', /^\s*Kouluttaja\s*\*/)
        .should('be.visible')
    })
  })
})
