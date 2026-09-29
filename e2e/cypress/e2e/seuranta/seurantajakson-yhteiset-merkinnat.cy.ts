import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-modal'
const ERIKOISTUVAN_API = '/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso'
const KOULUTTAJAN_API = '/api/kouluttaja/seurantakeskustelut/seurantajakso'
const KOULUTTAJA_NIMI = 'Lassekalevi Hummaamistes'
const MERKINNAT = 'Yhteiset merkinnät keskustelusta ja jatkosuunnitelmista'
const SEURAAVA_KESKUSTELU = 'Seuraavan keskustelun ajankohta'
const ARVION_OTSIKKO =
  'Kouluttajan arviointi jaksosta, erikoistujan etenemisestä ja seurantakeskustelusta'
const MERKINTARIVIT = [
  'E2E: Keskustelussa käytiin läpi seurantajakson vahvuudet ja kehittämiskohteet.',
  'E2E: Sovittiin ohjatusta harjoittelusta ja edistymisen seurannasta.'
]
const YHTEISET_MERKINNAT = MERKINTARIVIT.join('\n\n')
const KOULUTTAJAN_ARVIO = {
  edistyminenTavoitteidenMukaista: false,
  huolenaiheet: 'E2E: Itsenäinen päätöksenteko tarvitsee vielä harjoittelua.',
  kouluttajanArvio: 'E2E: Osaaminen kehittyy, mutta erikoistuja tarvitsee vielä ohjausta.',
  erikoisalanTyoskentelyvalmiudet: 'E2E: Erikoistuja työskentelee ohjatusti vastaanotolla.',
  jatkotoimetJaRaportointi: 'E2E: Sovitaan viikoittainen ohjaus ja seurataan etenemistä.'
}

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
const merkinnat = () => kentta(MERKINNAT).find('textarea')
const keskustelupaiva = () => kentta(SEURAAVA_KESKUSTELU).find('input.date-input')
const lahetaPainike = () => cy.get(SIVU).contains('button', /^\s*Tallenna ja lähetä\s*$/)
const kirjoitaMerkinnat = () => merkinnat().type(MERKINTARIVIT.join('{enter}{enter}'))

const paivamaara = (paivia: number) => {
  const pvm = new Date()
  pvm.setDate(pvm.getDate() + paivia)
  const vuosi = pvm.getFullYear()
  const kuukausi = pvm.getMonth() + 1
  const paiva = pvm.getDate()
  return {
    api: `${vuosi}-${String(kuukausi).padStart(2, '0')}-${String(paiva).padStart(2, '0')}`,
    fi: `${paiva}.${kuukausi}.${vuosi}`
  }
}

const tarkistaEtteiLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@merkintapyynto').should('not.have.been.called')
}

const tarkistaKouluttajanArvioNakyvissa = () => {
  kentta('Edistyminen on ollut sovittujen osaamistavoitteiden mukaista')
    .should('be.visible')
    .and('contain.text', 'Ei, huolenaiheita on')
    .find('input')
    .should('not.exist')
  for (const [label, teksti] of [
    [ARVION_OTSIKKO, KOULUTTAJAN_ARVIO.kouluttajanArvio],
    ['Huolenaiheet', KOULUTTAJAN_ARVIO.huolenaiheet],
    ['Erikoisalan työskentelyvalmiudet', KOULUTTAJAN_ARVIO.erikoisalanTyoskentelyvalmiudet],
    ['Jatkotoimet ja raportointi', KOULUTTAJAN_ARVIO.jatkotoimetJaRaportointi]
  ]) {
    kentta(label).should('be.visible').and('contain.text', teksti)
    kentta(label).find('textarea').should('not.exist')
  }
}

const tarkistaMerkinnatNakyvissa = (keskustelu: ReturnType<typeof paivamaara> | null) => {
  for (const rivi of MERKINTARIVIT) {
    kentta(MERKINNAT).should('be.visible').and('contain.text', rivi)
  }
  merkinnat().should('not.exist')
  if (keskustelu) {
    kentta(SEURAAVA_KESKUSTELU).should('be.visible').and('contain.text', keskustelu.fi)
    keskustelupaiva().should('not.exist')
  } else {
    cy.get(SIVU).contains('label', SEURAAVA_KESKUSTELU).should('not.exist')
  }
  cy.get(SIVU)
    .contains('Seurantajakso on arvioitu ja yhteiset merkinnät hyväksytty.')
    .should('not.exist')
}

describe('Erikoistuvan seurantakeskustelun yhteiset merkinnät käyttöliittymässä', () => {
  let seurantajakso: Seurantajakso

  const tarkistaAiemmatTiedot = (tallennettu: Seurantajakso) => {
    expect(tallennettu).to.include({
      id: seurantajakso.id,
      alkamispaiva: seurantajakso.alkamispaiva,
      paattymispaiva: seurantajakso.paattymispaiva,
      omaArviointi: seurantajakso.omaArviointi,
      lisahuomioita: seurantajakso.lisahuomioita,
      seuraavanJaksonTavoitteet: seurantajakso.seuraavanJaksonTavoitteet,
      opintooikeusId: seurantajakso.opintooikeusId,
      ...KOULUTTAJAN_ARVIO,
      hyvaksytty: false,
      korjausehdotus: null
    })
    // The setup POST contains only a trainer ID; its display name is not a reliable baseline.
    expect(tallennettu.kouluttaja).to.include({
      id: seurantajakso.kouluttaja.id,
      nimi: KOULUTTAJA_NIMI
    })
    expect(tallennettu.koulutusjaksot).to.deep.eq(seurantajakso.koulutusjaksot)
  }

  const tarkistaOdottaaMerkintoja = (tallennettu: Seurantajakso) => {
    tarkistaAiemmatTiedot(tallennettu)
    expect(tallennettu).to.include({
      seurantakeskustelunYhteisetMerkinnat: null,
      seuraavanKeskustelunAjankohta: null,
      tila: 'ODOTTAA_YHTEISIA_MERKINTOJA'
    })
  }

  const tarkistaTallennetutMerkinnat = (
    tallennettu: Seurantajakso,
    seuraavaPaiva: string | null
  ) => {
    tarkistaAiemmatTiedot(tallennettu)
    expect(tallennettu).to.include({
      seurantakeskustelunYhteisetMerkinnat: YHTEISET_MERKINNAT,
      seuraavanKeskustelunAjankohta: seuraavaPaiva
    })
  }

  const avaaJakso = (rooli: 'erikoistuva-laakari' | 'kouluttaja', alias: string) => {
    // Each reopening must read the saved state, not a queued response from before submission.
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

  const tarkistaEtteiTallennettu = () => {
    cy.apiRequest({ method: 'GET', url: `${ERIKOISTUVAN_API}/${seurantajakso.id}` }).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaOdottaaMerkintoja(body)
      }
    )
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.createSeurantajaksoViaApi({ kouluttajaId: Cypress.env('kouluttajaId') }).then((body) => {
      expect(body.id).to.be.a('number')
      expect(body.kouluttaja.id).to.eq(Cypress.env('kouluttajaId'))
      seurantajakso = body
    })
    // Initial assessment is setup; only the trainee's shared-note submission is under test.
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    cy.then(() => cy.apiRequest({ method: 'GET', url: `${KOULUTTAJAN_API}/${seurantajakso.id}` }))
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.tila).to.eq('ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA')
        return cy.apiRequest({
          method: 'PUT',
          url: `${KOULUTTAJAN_API}/${seurantajakso.id}`,
          body: { ...body, ...KOULUTTAJAN_ARVIO }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaAiemmatTiedot(body)
        expect(body.seurantakeskustelunYhteisetMerkinnat).to.be.null
        expect(body.seuraavanKeskustelunAjankohta).to.be.null
      })
    cy.loginAsErikoistuva()
    const merkintapyynto = cy.spy().as('merkintapyynto')
    cy.then(() => {
      cy.intercept('PUT', `**${ERIKOISTUVAN_API}/${seurantajakso.id}`, (request) => {
        merkintapyynto(request.body)
      }).as('tallennaMerkinnat')
      avaaJakso('erikoistuva-laakari', 'jaksoEnnenMerkintoja').then(tarkistaOdottaaMerkintoja)
    })
    cy.get(SIVU).contains('a', 'Muokkaa tietoja').should('be.visible').click()
    cy.get(SIVU).contains('h1', 'Muokkaa seurantajaksoa').should('be.visible')
    merkinnat().should('be.visible').and('have.value', '').and('not.be.disabled')
    keskustelupaiva().should('have.value', '')
    tarkistaKouluttajanArvioNakyvissa()
  })

  after(() => {
    // Shared notes prevent API deletion; use the existing FK-safe database cleanup.
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('vaatii yhteiset merkinnät ja estää virheellisen tai menneen keskustelupäivän', () => {
    lahetaPainike().click()
    merkinnat().should('have.class', 'is-invalid')
    kentta(MERKINNAT).contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    tarkistaEtteiLahetetty()

    kirjoitaMerkinnat()
    keskustelupaiva().type('ei-pvm').blur()
    lahetaPainike().click()
    keskustelupaiva().should('have.class', 'is-invalid')
    kentta(SEURAAVA_KESKUSTELU)
      .contains('.invalid-feedback', 'Anna päivämäärä muodossa pp.kk.vvvv')
      .should('be.visible')
    tarkistaEtteiLahetetty()

    keskustelupaiva().clear().type(paivamaara(-7).fi).blur()
    lahetaPainike().click()
    keskustelupaiva().should('have.class', 'is-invalid')
    kentta(SEURAAVA_KESKUSTELU)
      .contains('.invalid-feedback', 'Päivämäärä ei voi olla menneisyydessä')
      .should('be.visible')
    tarkistaEtteiLahetetty()

    // The date is optional: clearing an invalid date must allow confirmation again.
    keskustelupaiva().clear().blur()
    lahetaPainike().click()
    cy.get(VAHVISTUS).should('be.visible')
    cy.get('@merkintapyynto').should('not.have.been.called')
    cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
    tarkistaEtteiLahetetty()
    merkinnat().should('have.value', YHTEISET_MERKINNAT)
    tarkistaEtteiTallennettu()
  })

  for (const paivaMukana of [false, true]) {
    it(
      paivaMukana
        ? 'lähettää merkinnät ja seuraavan keskustelupäivän kouluttajalle vasta vahvistuksesta'
        : 'lähettää merkinnät ilman keskustelupäivää ja jättää lopullisen hyväksynnän odottamaan',
      () => {
        // Relative dates keep the optional-date case valid in future local and CI runs.
        const keskustelu = paivaMukana ? paivamaara(30) : null
        kirjoitaMerkinnat()
        if (keskustelu) {
          keskustelupaiva().type(keskustelu.fi).blur()
        }
        lahetaPainike().click()
        cy.get(VAHVISTUS)
          .should('be.visible')
          .and('contain.text', 'Vahvista lomakkeen lähetys')
          .and(
            'contain.text',
            'Seurantakeskustelun yhteiset merkinnät lähetetään kouluttajalle. ' +
              'Saat ilmoituksen, kun seurantajakso on hyväksytty.'
          )
        cy.get('@merkintapyynto').should('not.have.been.called')
        cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
        tarkistaEtteiLahetetty()
        merkinnat().should('have.value', YHTEISET_MERKINNAT)
        keskustelupaiva().should('have.value', keskustelu?.fi ?? '')
        tarkistaEtteiTallennettu()

        lahetaPainike().click()
        cy.get(VAHVISTUS).should('be.visible')
        cy.get(VAHVISTUS).contains('button', 'Tallenna ja lähetä').click()
        cy.wait('@tallennaMerkinnat').then(({ request, response }) => {
          tarkistaTallennetutMerkinnat(request.body, keskustelu?.api ?? null)
          expect(response?.statusCode).to.eq(200)
          tarkistaTallennetutMerkinnat(response?.body, keskustelu?.api ?? null)
          // Only GET responses calculate the workflow status.
        })
        cy.get('@merkintapyynto').should('have.been.calledOnce')
        cy.location('pathname').should(
          'eq',
          `/seurantakeskustelut/seurantajakso/${seurantajakso.id}`
        )
        tarkistaMerkinnatNakyvissa(keskustelu)

        avaaJakso('erikoistuva-laakari', 'merkinnatUudelleenAvattuna').then((tallennettu) => {
          tarkistaTallennetutMerkinnat(tallennettu, keskustelu?.api ?? null)
          expect(tallennettu.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
        })
        cy.get(SIVU)
          .contains(
            '.alert',
            'Seurantakeskustelun yhteiset merkinnät on lähetetty kouluttajalle hyväksyttäväksi.'
          )
          .should('be.visible')
        tarkistaMerkinnatNakyvissa(keskustelu)
        tarkistaKouluttajanArvioNakyvissa()
        cy.get(SIVU).contains('a', 'Muokkaa tietoja').should('not.exist')
        cy.get(SIVU).contains('button', 'Tallenna ja lähetä').should('not.exist')

        cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
        avaaJakso('kouluttaja', 'kouluttajanMerkinnat').then((tallennettu) => {
          tarkistaTallennetutMerkinnat(tallennettu, keskustelu?.api ?? null)
          expect(tallennettu.tila).to.eq('ODOTTAA_HYVAKSYNTAA')
        })
        tarkistaMerkinnatNakyvissa(keskustelu)
        tarkistaKouluttajanArvioNakyvissa()
        cy.get(SIVU)
          .contains('a', 'Muokkaa arviointia')
          .should('be.visible')
          .and(
            'have.attr',
            'href',
            `/seurantakeskustelut/seurantajakso/${seurantajakso.id}/muokkaa`
          )
        cy.get('@merkintapyynto').should('have.been.calledOnce')
      }
    )
  }
})
