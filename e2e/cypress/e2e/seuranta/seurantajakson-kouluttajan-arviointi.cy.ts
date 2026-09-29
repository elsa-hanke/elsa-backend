import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-modal'
const KOULUTTAJAN_API = '/api/kouluttaja/seurantakeskustelut/seurantajakso'
const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'
const EDISTYMINEN = 'Edistyminen on ollut sovittujen osaamistavoitteiden mukaista'
const KOULUTTAJAN_ARVIO =
  'Kouluttajan arviointi jaksosta, erikoistujan etenemisestä ja seurantakeskustelusta'
const TYOSKENTELYVALMIUDET = 'Erikoisalan työskentelyvalmiudet'
const JATKOTOIMET = 'Jatkotoimet ja raportointi'
const ARVIO = 'E2E: Osaaminen on kehittynyt. Seuraavalla jaksolla jatketaan ohjattua harjoittelua.'
const HUOLENAIHEET = 'E2E: Itsenäinen päätöksenteko tarvitsee vielä harjoittelua.'
const VALMIUDET = 'E2E: Erikoistuja työskentelee ohjatusti erikoisalan vastaanotolla.'
const RAPORTOINTI = 'E2E: Sovitaan viikoittainen ohjaus ja arvioidaan eteneminen kuukauden päästä.'
const HYVAKSYTTY_ILMOITUS = 'Seurantajakso on arvioitu ja yhteiset merkinnät hyväksytty.'

type KouluttajanArvio = {
  edistyminenTavoitteidenMukaista: boolean | null
  huolenaiheet: string | null
  kouluttajanArvio: string | null
  erikoisalanTyoskentelyvalmiudet: string | null
  jatkotoimetJaRaportointi: string | null
}

type Seurantajakso = KouluttajanArvio & {
  id: number
  alkamispaiva: string
  paattymispaiva: string
  omaArviointi: string
  lisahuomioita: string
  seuraavanJaksonTavoitteet: string
  opintooikeusId: number
  kouluttaja: { id: number; nimi: string }
  koulutusjaksot: unknown[]
  tila: string | null
}

const TYHJA_ARVIO: KouluttajanArvio = {
  edistyminenTavoitteidenMukaista: null,
  huolenaiheet: null,
  kouluttajanArvio: null,
  erikoisalanTyoskentelyvalmiudet: null,
  jatkotoimetJaRaportointi: null
}

const odotettuArvio = (edistyminen: boolean): KouluttajanArvio => ({
  edistyminenTavoitteidenMukaista: edistyminen,
  huolenaiheet: edistyminen ? null : HUOLENAIHEET,
  kouluttajanArvio: ARVIO,
  erikoisalanTyoskentelyvalmiudet: edistyminen ? null : VALMIUDET,
  jatkotoimetJaRaportointi: edistyminen ? null : RAPORTOINTI
})

const kentta = (label: string) => cy.get(SIVU).contains('label', label).closest('.form-group')
const lahetaPainike = () => cy.get(SIVU).contains('button', /^\s*Tallenna ja lähetä\s*$/)

const valitseEdistyminen = (edistyminen: boolean) => {
  kentta(EDISTYMINEN)
    .contains('label', edistyminen ? /^\s*Kyllä\s*$/ : /^\s*Ei, huolenaiheita on\s*$/)
    .click()
  kentta(EDISTYMINEN).find('input[type="radio"]:checked').should('have.value', String(edistyminen))
}

const tarkistaEtteiLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@arviointipyynto').should('not.have.been.called')
}

const tarkistaArvioNakyvissa = (edistyminen: boolean) => {
  kentta(EDISTYMINEN)
    .should('be.visible')
    .and('contain.text', edistyminen ? 'Kyllä' : 'Ei, huolenaiheita on')
  kentta(EDISTYMINEN).find('input').should('not.exist')
  kentta(KOULUTTAJAN_ARVIO).should('be.visible').and('contain.text', ARVIO)
  kentta(KOULUTTAJAN_ARVIO).find('textarea').should('not.exist')
  if (edistyminen) {
    for (const label of ['Huolenaiheet', TYOSKENTELYVALMIUDET, JATKOTOIMET]) {
      cy.get(SIVU).contains('label', label).should('not.exist')
    }
  } else {
    for (const [label, teksti] of [
      ['Huolenaiheet', HUOLENAIHEET],
      [TYOSKENTELYVALMIUDET, VALMIUDET],
      [JATKOTOIMET, RAPORTOINTI]
    ]) {
      kentta(label).should('be.visible').and('contain.text', teksti)
      kentta(label).find('textarea').should('not.exist')
    }
  }
  cy.get(SIVU).contains(HYVAKSYTTY_ILMOITUS).should('not.exist')
}

describe('Seurantajakson kouluttajan arviointi käyttöliittymässä', () => {
  let seurantajakso: Seurantajakso

  const tarkistaErikoistuvanTiedotJaKeskenerainenHyvaksynta = (tallennettu: Seurantajakso) => {
    expect(tallennettu).to.include({
      id: seurantajakso.id,
      alkamispaiva: seurantajakso.alkamispaiva,
      paattymispaiva: seurantajakso.paattymispaiva,
      omaArviointi: seurantajakso.omaArviointi,
      lisahuomioita: seurantajakso.lisahuomioita,
      seuraavanJaksonTavoitteet: seurantajakso.seuraavanJaksonTavoitteet,
      opintooikeusId: seurantajakso.opintooikeusId,
      hyvaksytty: false,
      seurantakeskustelunYhteisetMerkinnat: null,
      seuraavanKeskustelunAjankohta: null,
      korjausehdotus: null
    })
    expect(tallennettu.kouluttaja).to.include({
      id: seurantajakso.kouluttaja.id,
      nimi: KOULUTTAJA_NIMI
    })
    expect(tallennettu.koulutusjaksot).to.deep.eq(seurantajakso.koulutusjaksot)
  }

  const tarkistaOdottaaArviointia = (tallennettu: Seurantajakso) => {
    tarkistaErikoistuvanTiedotJaKeskenerainenHyvaksynta(tallennettu)
    expect(tallennettu).to.include(TYHJA_ARVIO)
    expect(tallennettu.tila).to.eq('ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA')
  }

  const tarkistaTallennettuArvio = (tallennettu: Seurantajakso, edistyminen: boolean) => {
    tarkistaErikoistuvanTiedotJaKeskenerainenHyvaksynta(tallennettu)
    expect(tallennettu).to.include(odotettuArvio(edistyminen))
  }

  const avaaJakso = (
    rooli: 'kouluttaja' | 'erikoistuva-laakari',
    alias: string,
    muokkaa = false
  ) => {
    // Each visit gets a fresh response, never a queued GET from before the assessment.
    cy.intercept({
      method: 'GET',
      url: `**/api/${rooli}/seurantakeskustelut/seurantajakso/${seurantajakso.id}`,
      times: 1
    }).as(alias)
    cy.visit(`/seurantakeskustelut/seurantajakso/${seurantajakso.id}${muokkaa ? '/muokkaa' : ''}`)
    return cy.wait(`@${alias}`).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      return response?.body as Seurantajakso
    })
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    // Only the trainee's initial submission is setup. Leave the shared notes empty.
    cy.createSeurantajaksoViaApi({ kouluttajaId: Cypress.env('kouluttajaId') }).then((body) => {
      expect(body.id).to.be.a('number')
      expect(body.kouluttaja.id).to.eq(Cypress.env('kouluttajaId'))
      seurantajakso = body
    })
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    const arviointipyynto = cy.spy().as('arviointipyynto')
    cy.then(() => {
      // Observe the real request without replacing the response.
      cy.intercept('PUT', `**${KOULUTTAJAN_API}/${seurantajakso.id}`, (request) => {
        arviointipyynto(request.body)
      }).as('tallennaArvio')
      avaaJakso('kouluttaja', 'arvioEnnenTallennusta', true).then(tarkistaOdottaaArviointia)
    })
    cy.get(SIVU).contains('h1', 'Seurantajakson yhteenveto').should('be.visible')
    kentta(KOULUTTAJAN_ARVIO).find('textarea').should('be.visible').and('have.value', '')
    kentta(EDISTYMINEN).find('input[type="radio"]:checked').should('not.exist')
    kentta('Oma arviointi seurantajaksolta').find('textarea').should('not.exist')
  })

  after(() => {
    // Removes the created monitoring periods and their study-right dependencies.
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('vaatii edistymisvalinnan ja kouluttajan arvion sekä kielteiselle valinnalle huolenaiheet', () => {
    lahetaPainike().click()
    kentta(EDISTYMINEN).contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    kentta(KOULUTTAJAN_ARVIO).find('textarea').should('have.class', 'is-invalid')
    tarkistaEtteiLahetetty()

    kentta(KOULUTTAJAN_ARVIO).find('textarea').type(ARVIO)
    lahetaPainike().click()
    kentta(EDISTYMINEN).contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    tarkistaEtteiLahetetty()

    kentta(KOULUTTAJAN_ARVIO).find('textarea').clear()
    valitseEdistyminen(true)
    lahetaPainike().click()
    kentta(KOULUTTAJAN_ARVIO).find('textarea').should('have.class', 'is-invalid')
    tarkistaEtteiLahetetty()

    kentta(KOULUTTAJAN_ARVIO).find('textarea').type(ARVIO)
    valitseEdistyminen(false)
    lahetaPainike().click()
    kentta('Huolenaiheet').find('textarea').should('have.class', 'is-invalid')
    kentta('Huolenaiheet').contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    tarkistaEtteiLahetetty()

    // A positive assessment needs neither concerns nor the optional notes.
    valitseEdistyminen(true)
    lahetaPainike().click()
    cy.get(VAHVISTUS).should('be.visible')
    cy.get('@arviointipyynto').should('not.have.been.called')
    cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
    tarkistaEtteiLahetetty()
    cy.apiRequest({ method: 'GET', url: `${KOULUTTAJAN_API}/${seurantajakso.id}` }).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaOdottaaArviointia(body)
      }
    )
  })

  for (const edistyminen of [true, false]) {
    it(
      edistyminen
        ? 'tallentaa myönteisen arvion vasta vahvistuksesta ja poistaa aiemmin kirjoitetut huolenaiheet'
        : 'säilyttää huolenaiheet ja jatkotoimet kouluttajan ja erikoistujan näkymissä',
      () => {
        valitseEdistyminen(false)
        kentta('Huolenaiheet').find('textarea').type(HUOLENAIHEET)
        if (edistyminen) {
          valitseEdistyminen(true)
          cy.get(SIVU).contains('label', 'Huolenaiheet').should('not.exist')
        } else {
          kentta(TYOSKENTELYVALMIUDET).find('textarea').type(VALMIUDET)
          kentta(JATKOTOIMET).find('textarea').type(RAPORTOINTI)
        }
        kentta(KOULUTTAJAN_ARVIO).find('textarea').type(ARVIO)

        lahetaPainike().click()
        cy.get(VAHVISTUS)
          .should('be.visible')
          .and('contain.text', 'Vahvista lomakkeen lähetys')
          .and(
            'contain.text',
            'Lähetyksen jälkeen seurantajakso on arvioitu ja tieto tästä lähetetään erikoistujalle.'
          )
        cy.get('@arviointipyynto').should('not.have.been.called')
        cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
        tarkistaEtteiLahetetty()
        kentta(KOULUTTAJAN_ARVIO).find('textarea').should('have.value', ARVIO)
        kentta(EDISTYMINEN)
          .find('input[type="radio"]:checked')
          .should('have.value', String(edistyminen))
        kentta(TYOSKENTELYVALMIUDET)
          .find('textarea')
          .should('have.value', edistyminen ? '' : VALMIUDET)
        kentta(JATKOTOIMET)
          .find('textarea')
          .should('have.value', edistyminen ? '' : RAPORTOINTI)
        if (!edistyminen) {
          kentta('Huolenaiheet').find('textarea').should('have.value', HUOLENAIHEET)
        }
        cy.apiRequest({ method: 'GET', url: `${KOULUTTAJAN_API}/${seurantajakso.id}` }).then(
          ({ status, body }) => {
            expect(status).to.eq(200)
            tarkistaOdottaaArviointia(body)
          }
        )

        lahetaPainike().click()
        cy.get(VAHVISTUS).should('be.visible')
        cy.get(VAHVISTUS).contains('button', 'Tallenna ja lähetä').click()
        cy.wait('@tallennaArvio').then(({ request, response }) => {
          tarkistaErikoistuvanTiedotJaKeskenerainenHyvaksynta(request.body)
          const { huolenaiheet, ...arvio } = odotettuArvio(edistyminen)
          expect(request.body).to.include(arvio)
          if (!edistyminen) {
            expect(request.body.huolenaiheet).to.eq(huolenaiheet)
          }
          expect(response?.statusCode).to.eq(200)
          // Previously entered concerns must not survive a positive assessment.
          tarkistaTallennettuArvio(response?.body, edistyminen)
          // The PUT response has no calculated workflow status; verify it on the next GET.
        })
        cy.get('@arviointipyynto').should('have.been.calledOnce')
        cy.location('pathname').should(
          'eq',
          `/seurantakeskustelut/seurantajakso/${seurantajakso.id}`
        )
        cy.get(SIVU)
          .contains('.alert', 'Seurantajakso on arvioitu ja tieto tästä lähetetty erikoistujalle.')
          .should('be.visible')
        tarkistaArvioNakyvissa(edistyminen)

        avaaJakso('kouluttaja', 'arvioUudelleenAvattuna', true).then((tallennettu) => {
          tarkistaTallennettuArvio(tallennettu, edistyminen)
          expect(tallennettu.tila).to.eq('ODOTTAA_YHTEISIA_MERKINTOJA')
        })
        kentta(KOULUTTAJAN_ARVIO).find('textarea').should('have.value', ARVIO)
        kentta(EDISTYMINEN)
          .find('input[type="radio"]:checked')
          .should('have.value', String(edistyminen))
        kentta(TYOSKENTELYVALMIUDET)
          .find('textarea')
          .should('have.value', edistyminen ? '' : VALMIUDET)
        kentta(JATKOTOIMET)
          .find('textarea')
          .should('have.value', edistyminen ? '' : RAPORTOINTI)
        if (edistyminen) {
          cy.get(SIVU).contains('label', 'Huolenaiheet').should('not.exist')
        } else {
          kentta('Huolenaiheet').find('textarea').should('have.value', HUOLENAIHEET)
        }

        cy.loginAsErikoistuva()
        avaaJakso('erikoistuva-laakari', 'erikoistuvanArvio').then((tallennettu) => {
          tarkistaTallennettuArvio(tallennettu, edistyminen)
          expect(tallennettu.tila).to.eq('ODOTTAA_YHTEISIA_MERKINTOJA')
        })
        cy.get(SIVU)
          .contains('.alert', 'Kouluttaja on arvioinut seurantajakson.')
          .should('be.visible')
        cy.get(SIVU)
          .contains('.alert', 'Täydennä seurantakeskustelun jälkeen vielä seuraavat tiedot:')
          .should('be.visible')
        tarkistaArvioNakyvissa(edistyminen)
        cy.get(SIVU)
          .contains('Seurantajakso sisältää huolia.')
          .should(edistyminen ? 'not.exist' : 'be.visible')
        cy.get(SIVU)
          .contains('a', 'Muokkaa tietoja')
          .should('be.visible')
          .and(
            'have.attr',
            'href',
            `/seurantakeskustelut/seurantajakso/${seurantajakso.id}/muokkaa`
          )
        cy.get('@arviointipyynto').should('have.been.calledOnce')
      }
    )
  }
})
