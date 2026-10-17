import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-modal'
const KOULUTTAJAN_API = '/api/kouluttaja/seurantakeskustelut/seurantajakso'
const ERIKOISTUVAN_API = '/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso'
const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'
const EDISTYMINEN = 'Edistyminen on ollut sovittujen osaamistavoitteiden mukaista'
const ARVION_OTSIKKO =
  'Kouluttajan arviointi jaksosta, erikoistujan etenemisestä ja seurantakeskustelusta'
const MERKINNAT = 'Yhteiset merkinnät keskustelusta ja jatkosuunnitelmista'
const SEURAAVA_KESKUSTELU = 'Seuraavan keskustelun ajankohta'
const MERKINTARIVIT = [
  'E2E: Keskustelussa käytiin läpi seurantajakson vahvuudet ja kehittämiskohteet.',
  'E2E: Sovittiin ohjatusta harjoittelusta ja edistymisen seurannasta.'
]
const YHTEISET_MERKINNAT = MERKINTARIVIT.join('\n\n')
const ALKUPERAINEN_ARVIO = {
  edistyminenTavoitteidenMukaista: false,
  huolenaiheet: 'E2E: Itsenäinen päätöksenteko tarvitsee vielä harjoittelua.',
  kouluttajanArvio: 'E2E: Osaaminen kehittyy, mutta erikoistuja tarvitsee vielä ohjausta.',
  erikoisalanTyoskentelyvalmiudet: 'E2E: Erikoistuja työskentelee ohjatusti vastaanotolla.',
  jatkotoimetJaRaportointi: 'E2E: Sovitaan viikoittainen ohjaus ja seurataan etenemistä.'
}
const TARKENNETTU_ARVIO =
  'E2E: Yhteiset merkinnät vastaavat keskustelua ja jatkosuunnitelma on sovittu.'
const HYVAKSYTTY_ILMOITUS = 'Seurantajakso on arvioitu ja yhteiset merkinnät hyväksytty.'

type Seurantajakso = {
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

const kentta = (label: string) => cy.get(SIVU).contains('label', label).closest('.form-group')
const arvio = () => kentta(ARVION_OTSIKKO).find('textarea')
const lahetaPainike = () => cy.get(SIVU).contains('button', /^\s*Tallenna ja lähetä\s*$/)

const seuraavaKeskustelupaiva = () => {
  const pvm = new Date()
  pvm.setDate(pvm.getDate() + 30)
  const vuosi = pvm.getFullYear()
  const kuukausi = pvm.getMonth() + 1
  const paiva = pvm.getDate()
  return {
    api: `${vuosi}-${String(kuukausi).padStart(2, '0')}-${String(paiva).padStart(2, '0')}`,
    fi: `${paiva}.${kuukausi}.${vuosi}`
  }
}

describe('Kouluttajan seurantajakson lopullinen hyväksyntä käyttöliittymässä', () => {
  let seurantajakso: Seurantajakso
  let keskustelupaiva: ReturnType<typeof seuraavaKeskustelupaiva>

  const tarkistaSailyvatTiedot = (tallennettu: Seurantajakso) => {
    expect(tallennettu).to.include({
      id: seurantajakso.id,
      alkamispaiva: seurantajakso.alkamispaiva,
      paattymispaiva: seurantajakso.paattymispaiva,
      omaArviointi: seurantajakso.omaArviointi,
      lisahuomioita: seurantajakso.lisahuomioita,
      seuraavanJaksonTavoitteet: seurantajakso.seuraavanJaksonTavoitteet,
      opintooikeusId: seurantajakso.opintooikeusId,
      seurantakeskustelunYhteisetMerkinnat: YHTEISET_MERKINNAT,
      seuraavanKeskustelunAjankohta: keskustelupaiva.api,
      erikoisalanTyoskentelyvalmiudet: ALKUPERAINEN_ARVIO.erikoisalanTyoskentelyvalmiudet,
      jatkotoimetJaRaportointi: ALKUPERAINEN_ARVIO.jatkotoimetJaRaportointi,
      korjausehdotus: null
    })
    // The ID-only setup POST does not provide a reliable trainer display name.
    expect(tallennettu.kouluttaja).to.include({
      id: seurantajakso.kouluttaja.id,
      nimi: KOULUTTAJA_NIMI
    })
    expect(tallennettu.koulutusjaksot).to.deep.eq(seurantajakso.koulutusjaksot)
  }

  const tarkistaOdottaaHyvaksyntaa = (tallennettu: Seurantajakso) => {
    tarkistaSailyvatTiedot(tallennettu)
    expect(tallennettu).to.include({
      ...ALKUPERAINEN_ARVIO,
      hyvaksytty: false,
      tila: 'ODOTTAA_HYVAKSYNTAA'
    })
  }

  const tarkistaHyvaksytty = (tallennettu: Seurantajakso, edistyminen: boolean) => {
    tarkistaSailyvatTiedot(tallennettu)
    expect(tallennettu).to.include({
      edistyminenTavoitteidenMukaista: edistyminen,
      huolenaiheet: edistyminen ? null : ALKUPERAINEN_ARVIO.huolenaiheet,
      kouluttajanArvio: TARKENNETTU_ARVIO,
      hyvaksytty: true
    })
  }

  const tarkistaYhteisetMerkinnatNakyvissa = () => {
    for (const rivi of MERKINTARIVIT) {
      kentta(MERKINNAT).should('be.visible').and('contain.text', rivi)
    }
    kentta(MERKINNAT).find('textarea').should('not.exist')
    kentta(SEURAAVA_KESKUSTELU).should('be.visible').and('contain.text', keskustelupaiva.fi)
    kentta(SEURAAVA_KESKUSTELU).find('input').should('not.exist')
  }

  const tarkistaHyvaksyttyNakyvissa = (edistyminen: boolean) => {
    cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_ILMOITUS).should('be.visible')
    tarkistaYhteisetMerkinnatNakyvissa()
    kentta(EDISTYMINEN)
      .should('be.visible')
      .and('contain.text', edistyminen ? 'Kyllä' : 'Ei, huolenaiheita on')
    kentta(EDISTYMINEN).find('input').should('not.exist')
    for (const [label, teksti] of [
      [ARVION_OTSIKKO, TARKENNETTU_ARVIO],
      ['Erikoisalan työskentelyvalmiudet', ALKUPERAINEN_ARVIO.erikoisalanTyoskentelyvalmiudet],
      ['Jatkotoimet ja raportointi', ALKUPERAINEN_ARVIO.jatkotoimetJaRaportointi]
    ]) {
      kentta(label).should('be.visible').and('contain.text', teksti)
      kentta(label).find('textarea').should('not.exist')
    }
    if (edistyminen) {
      cy.get(SIVU).contains('label', 'Huolenaiheet').should('not.exist')
    } else {
      kentta('Huolenaiheet')
        .should('be.visible')
        .and('contain.text', ALKUPERAINEN_ARVIO.huolenaiheet)
      kentta('Huolenaiheet').find('textarea').should('not.exist')
    }
    cy.get(SIVU).contains('a', 'Muokkaa arviointia').should('not.exist')
    cy.get(SIVU).contains('a', 'Muokkaa tietoja').should('not.exist')
    cy.get(SIVU).contains('button', 'Tallenna ja lähetä').should('not.exist')
    cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')
  }

  const avaaJakso = (rooli: 'kouluttaja' | 'erikoistuva-laakari', alias: string) => {
    // Reopening must use a fresh response, never an earlier GET queued before approval.
    cy.intercept({
      method: 'GET',
      url: `**/api/${rooli}/seurantakeskustelut/seurantajakso/${seurantajakso.id}`,
      times: 1
    }).as(alias)
    cy.visit(`/seurantakeskustelut/seurantajakso/${seurantajakso.id}`)
    return cy.wait(`@${alias}`).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      return response?.body as Seurantajakso
    })
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
  })

  beforeEach(() => {
    keskustelupaiva = seuraavaKeskustelupaiva()
    cy.loginAsErikoistuva()
    cy.createSeurantajaksoViaApi({ kouluttajaId: Cypress.expose('kouluttajaId') }).then((body) => {
      expect(body.id).to.be.a('number')
      expect(body.kouluttaja.id).to.eq(Cypress.expose('kouluttajaId'))
      seurantajakso = body
    })
    // Assess first, then add shared notes. Reversing this order would already approve the period.
    cy.task<string | null>('tokens:get', 'kouluttajaToken', { log: false }).then((token) => cy.loginAsKouluttaja(token ?? undefined))
    cy.then(() => cy.apiRequest({ method: 'GET', url: `${KOULUTTAJAN_API}/${seurantajakso.id}` }))
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        return cy.apiRequest({
          method: 'PUT',
          url: `${KOULUTTAJAN_API}/${seurantajakso.id}`,
          body: { ...body, ...ALKUPERAINEN_ARVIO }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body).to.include({ ...ALKUPERAINEN_ARVIO, hyvaksytty: false })
        expect(body.seurantakeskustelunYhteisetMerkinnat).to.be.null
      })
    cy.loginAsErikoistuva()
    cy.then(() => cy.apiRequest({ method: 'GET', url: `${ERIKOISTUVAN_API}/${seurantajakso.id}` }))
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.tila).to.eq('ODOTTAA_YHTEISIA_MERKINTOJA')
        return cy.apiRequest({
          method: 'PUT',
          url: `${ERIKOISTUVAN_API}/${seurantajakso.id}`,
          body: {
            ...body,
            seurantakeskustelunYhteisetMerkinnat: YHTEISET_MERKINNAT,
            seuraavanKeskustelunAjankohta: keskustelupaiva.api
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaSailyvatTiedot(body)
        expect(body).to.include({ ...ALKUPERAINEN_ARVIO, hyvaksytty: false })
      })
    cy.task<string | null>('tokens:get', 'kouluttajaToken', { log: false }).then((token) => cy.loginAsKouluttaja(token ?? undefined))
    const hyvaksymispyynto = cy.spy().as('hyvaksymispyynto')
    cy.then(() => {
      // Observe only the final UI approval; setup requests above are not counted.
      cy.intercept('PUT', `**${KOULUTTAJAN_API}/${seurantajakso.id}`, (request) => {
        hyvaksymispyynto(request.body)
      }).as('hyvaksyJakso')
      avaaJakso('kouluttaja', 'jaksoEnnenHyvaksyntaa').then(tarkistaOdottaaHyvaksyntaa)
    })
    cy.get(SIVU).contains('a', 'Muokkaa arviointia').should('be.visible').click()
    cy.get(SIVU).contains('h1', 'Seurantajakson yhteenveto').should('be.visible')
    arvio().should('be.visible').and('have.value', ALKUPERAINEN_ARVIO.kouluttajanArvio)
    kentta(EDISTYMINEN).find('input[type="radio"]:checked').should('have.value', 'false')
    tarkistaYhteisetMerkinnatNakyvissa()
    kentta('Oma arviointi seurantajaksolta').find('textarea').should('not.exist')
    lahetaPainike().should('be.visible').and('not.be.disabled')
  })

  after(() => {
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  for (const edistyminen of [false, true]) {
    it(
      edistyminen
        ? 'hyväksyy tarkennetun myönteisen arvion ja poistaa aiemmat huolenaiheet'
        : 'hyväksyy yhteiset merkinnät säilyttäen huolenaiheet ja jatkotoimet',
      () => {
        arvio().clear().type(TARKENNETTU_ARVIO)
        if (edistyminen) {
          kentta(EDISTYMINEN)
            .contains('label', /^\s*Kyllä\s*$/)
            .click()
          cy.get(SIVU).contains('label', 'Huolenaiheet').should('not.exist')
        }
        lahetaPainike().click()
        cy.get(VAHVISTUS)
          .should('be.visible')
          .and('contain.text', 'Vahvista lomakkeen lähetys')
          .and(
            'contain.text',
            'Lähetyksen jälkeen seurantajakso on arvioitu sekä hyväksytty ja tieto tästä lähetetään erikoistujalle.'
          )
        cy.get('@hyvaksymispyynto').should('not.have.been.called')
        cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
        cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
        cy.get('@hyvaksymispyynto').should('not.have.been.called')
        arvio().should('have.value', TARKENNETTU_ARVIO)
        kentta(EDISTYMINEN)
          .find('input[type="radio"]:checked')
          .should('have.value', String(edistyminen))
        tarkistaYhteisetMerkinnatNakyvissa()
        cy.apiRequest({ method: 'GET', url: `${KOULUTTAJAN_API}/${seurantajakso.id}` }).then(
          ({ status, body }) => {
            expect(status).to.eq(200)
            tarkistaOdottaaHyvaksyntaa(body)
          }
        )

        lahetaPainike().click()
        cy.get(VAHVISTUS).should('be.visible')
        cy.get(VAHVISTUS).contains('button', 'Tallenna ja lähetä').click()
        cy.wait('@hyvaksyJakso').then(({ request, response }) => {
          tarkistaSailyvatTiedot(request.body)
          expect(request.body).to.include({
            edistyminenTavoitteidenMukaista: edistyminen,
            kouluttajanArvio: TARKENNETTU_ARVIO,
            // Approval is performed by the server, not by toggling a client-side flag.
            hyvaksytty: false
          })
          if (!edistyminen) {
            expect(request.body.huolenaiheet).to.eq(ALKUPERAINEN_ARVIO.huolenaiheet)
          }
          expect(response?.statusCode).to.eq(200)
          tarkistaHyvaksytty(response?.body, edistyminen)
          // The PUT has no calculated workflow status; verify HYVAKSYTTY on fresh GETs below.
        })
        cy.get('@hyvaksymispyynto').should('have.been.calledOnce')
        cy.location('pathname').should(
          'eq',
          `/seurantakeskustelut/seurantajakso/${seurantajakso.id}`
        )
        tarkistaHyvaksyttyNakyvissa(edistyminen)

        avaaJakso('kouluttaja', 'hyvaksyttyUudelleenAvattuna').then((tallennettu) => {
          tarkistaHyvaksytty(tallennettu, edistyminen)
          expect(tallennettu.tila).to.eq('HYVAKSYTTY')
        })
        tarkistaHyvaksyttyNakyvissa(edistyminen)

        cy.loginAsErikoistuva()
        avaaJakso('erikoistuva-laakari', 'erikoistuvanHyvaksyttyJakso').then((tallennettu) => {
          tarkistaHyvaksytty(tallennettu, edistyminen)
          expect(tallennettu.tila).to.eq('HYVAKSYTTY')
        })
        tarkistaHyvaksyttyNakyvissa(edistyminen)
        cy.get(SIVU)
          .contains('Seurantajakso sisältää huolia.')
          .should(edistyminen ? 'not.exist' : 'be.visible')
        cy.get('@hyvaksymispyynto').should('have.been.calledOnce')
      }
    )
  }
})
