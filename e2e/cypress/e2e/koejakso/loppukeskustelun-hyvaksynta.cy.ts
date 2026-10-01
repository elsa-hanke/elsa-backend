import {
  E2E_ERIKOISTUVA_EMAIL,
  ESIHENKILÖ_EMAIL,
  KOULUTTAJA_EMAIL
} from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-send'
const API = '/api/kouluttaja/koejakso/loppukeskustelu'
const ERIKOISTUVAN_API = '/api/erikoistuva-laakari/koejakso'
const HYVAKSYTTY_TEKSTI = 'Loppukeskustelu on hyväksytty kaikkien osapuolten toimesta.'
const ESITETAAN_HYVAKSYNTAA = 'Loppukeskustelu on käyty, esitämme koejakson hyväksymistä'
const OHJATAAN_JATKOTOIMIIN = 'Loppukeskustelu on käyty, ohjaus jatkotoimiin'
const JATKOTOIMET =
  'E2E: Sovitaan erikoisalan vastuuhenkilön kanssa lisäohjauksesta ja uudesta arvioinnista.'
const ESIHENKILO_NIMI = 'Tessa Testilä'

type Hyvaksyja = {
  id: number
  sopimusHyvaksytty: boolean
  kuittausaika: string | null
}

type Loppukeskustelu = {
  id: number
  esitetaanKoejaksonHyvaksymista: boolean | null
  jatkotoimenpiteet: string | null
  koejaksonPaattymispaiva: string
  erikoistuvanKuittausaika: string | null
  korjausehdotus: string | null
  lahikouluttaja: Hyvaksyja
  lahiesimies: Hyvaksyja
}

const hyvaksyPainike = () => cy.get(SIVU).contains('button', /^\s*Hyväksy ja lähetä\s*$/)
const jatkotoimienKentta = () =>
  cy.get(SIVU).contains('label', 'Selvitys jatkotoimista').closest('.form-group')
const vastuuhenkilonArvionLinkki = () =>
  cy
    .get(SIVU)
    .contains('h2', 'Erikoisalan vastuuhenkilön arvio koejaksosta')
    .parent()
    .contains('a', 'Pyydä arviointia')

const tarkistaArvioNakyvissa = (esitetaanHyvaksymista: boolean) => {
  cy.get(SIVU)
    .contains('p', esitetaanHyvaksymista ? ESITETAAN_HYVAKSYNTAA : OHJATAAN_JATKOTOIMIIN)
    .should('be.visible')
  if (esitetaanHyvaksymista) {
    cy.get(SIVU).contains('h5', 'Selvitys jatkotoimista').should('not.exist')
    cy.get(SIVU).contains('p', JATKOTOIMET).should('not.exist')
  } else {
    cy.get(SIVU).contains('h5', 'Selvitys jatkotoimista').should('be.visible')
    cy.get(SIVU).contains('p', JATKOTOIMET).should('be.visible')
  }
  cy.get(SIVU).find('textarea, input[type="radio"]').should('not.exist')
}

const tarkistaEtteiLahetetty = () => {
  cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
  cy.get('@hyvaksyntapyynto').should('not.have.been.called')
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
  return cy.wait('@hyvaksyLoppukeskustelu')
}

const avaaErikoistuvanKoejakso = (polku: string, alias: string) => {
  cy.intercept({ method: 'GET', url: `**${ERIKOISTUVAN_API}`, times: 1 }).as(alias)
  cy.visit(polku)
  return cy.wait(`@${alias}`).then(({ response }) => {
    expect(response?.statusCode).to.eq(200)
    return response?.body
  })
}

describe('Loppukeskustelun hyväksyminen kouluttajan ja lähiesihenkilön käyttöliittymässä', () => {
  let lomake: Loppukeskustelu
  let esihenkiloId: number
  let esihenkiloToken: string

  const tarkistaTallennus = (
    tallennettu: Loppukeskustelu,
    esitetaanHyvaksymista: boolean | null,
    kouluttajanKuittausaika: string | null,
    esihenkilonKuittausaika: string | null
  ) => {
    expect(tallennettu).to.include({
      id: lomake.id,
      esitetaanKoejaksonHyvaksymista: esitetaanHyvaksymista,
      jatkotoimenpiteet: esitetaanHyvaksymista === false ? JATKOTOIMET : null,
      koejaksonPaattymispaiva: lomake.koejaksonPaattymispaiva,
      erikoistuvanKuittausaika: lomake.erikoistuvanKuittausaika,
      korjausehdotus: null
    })
    expect(tallennettu.lahikouluttaja, 'kouluttajan hyväksyntä').to.include({
      id: Cypress.env('kouluttajaId'),
      sopimusHyvaksytty: kouluttajanKuittausaika !== null,
      kuittausaika: kouluttajanKuittausaika
    })
    expect(tallennettu.lahiesimies, 'lähiesihenkilön hyväksyntä').to.include({
      id: esihenkiloId,
      sopimusHyvaksytty: esihenkilonKuittausaika !== null,
      kuittausaika: esihenkilonKuittausaika
    })
  }

  const avaaArvioijanLomake = (alias: string) => {
    // Each visit has a distinct one-use alias so waits cannot consume an earlier response.
    cy.intercept({ method: 'GET', url: `**${API}/${lomake.id}`, times: 1 }).as(alias)
    cy.visit(`/koejakso/loppukeskustelu/${lomake.id}`)
    return cy.wait(`@${alias}`).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      // Earlier assessment data is added by GET; POST/PUT only return the final discussion.
      expect(response?.body.edistyminenTavoitteidenMukaista).to.eq(true)
      return response?.body as Loppukeskustelu
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
    // Seed only prerequisite stages; the final-discussion approvals below use the UI.
    cy.task('db:ensureValiarviointiHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    cy.apiRequest({ method: 'GET', url: ERIKOISTUVAN_API })
      .then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.valiarvioinninTila).to.eq('HYVAKSYTTY')
        expect(body.valiarviointi.edistyminenTavoitteidenMukaista).to.eq(true)
        expect(body.loppukeskustelunTila).to.eq('UUSI')
        expect(body.vastuuhenkilonArvionTila).to.eq('EI_AKTIIVINEN')
        const arvio = body.valiarviointi
        const paattymispaiva = body.aloituskeskustelu.koejaksonPaattymispaiva
        expect(paattymispaiva).to.match(/^\d{4}-\d{2}-\d{2}$/)
        return cy.apiRequest({
          method: 'POST',
          url: `${ERIKOISTUVAN_API}/loppukeskustelu`,
          body: {
            erikoistuvanNimi: arvio.erikoistuvanNimi,
            erikoistuvanErikoisala: arvio.erikoistuvanErikoisala,
            erikoistuvanYliopisto: arvio.erikoistuvanYliopisto,
            erikoistuvanOpiskelijatunnus: arvio.erikoistuvanOpiskelijatunnus,
            koejaksonPaattymispaiva: paattymispaiva,
            esitetaanKoejaksonHyvaksymista: null,
            jatkotoimenpiteet: null,
            korjausehdotus: null,
            lahikouluttaja: { id: Cypress.env('kouluttajaId'), sopimusHyvaksytty: false },
            lahiesimies: { id: esihenkiloId, sopimusHyvaksytty: false }
          }
        })
      })
      .then(({ status, body }) => {
        expect(status).to.eq(201)
        expect(body.id).to.be.a('number')
        expect(body.koejaksonPaattymispaiva).to.match(/^\d{4}-\d{2}-\d{2}$/)
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
      nimi: 'esittää hyväksyntää ja poistaa aiemmin kirjoitetut jatkotoimet hyväksynnästä',
      esitetaanHyvaksymista: true
    },
    {
      nimi: 'vaatii selvityksen jatkotoimista ja säilyttää sen molempien hyväksynnöissä',
      esitetaanHyvaksymista: false
    }
  ]

  tapaukset.forEach(({ nimi, esitetaanHyvaksymista }) => {
    it(nimi, () => {
      let kouluttajanKuittausaika: string
      let esihenkilonKuittausaika: string
      const arvioTeksti = esitetaanHyvaksymista ? ESITETAAN_HYVAKSYNTAA : OHJATAAN_JATKOTOIMIIN
      const jatkotoimenpiteet = esitetaanHyvaksymista ? null : JATKOTOIMET

      cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
      const hyvaksyntapyynto = cy.spy().as('hyvaksyntapyynto')
      // Observe both real approval requests without replacing their responses.
      cy.intercept('PUT', `**${API}`, (request) => {
        hyvaksyntapyynto(request.body)
      }).as('hyvaksyLoppukeskustelu')
      avaaArvioijanLomake('kouluttajanArvioEnnenHyvaksyntaa').then((tallennettu) =>
        tarkistaTallennus(tallennettu, null, null, null)
      )

      hyvaksyPainike().click()
      cy.get(SIVU).find('.invalid-feedback:visible').should('contain.text', 'Pakollinen tieto')
      tarkistaEtteiLahetetty()

      cy.get(SIVU).contains('label', OHJATAAN_JATKOTOIMIIN).click()
      jatkotoimienKentta().find('textarea').should('be.visible').and('have.value', '')
      hyvaksyPainike().click()
      jatkotoimienKentta()
        .find('.invalid-feedback:visible')
        .should('contain.text', 'Pakollinen tieto')
      tarkistaEtteiLahetetty()
      jatkotoimienKentta().find('textarea').type(JATKOTOIMET)

      if (esitetaanHyvaksymista) {
        // Changing the recommendation must not submit the now-hidden follow-up text.
        cy.get(SIVU).contains('label', ESITETAAN_HYVAKSYNTAA).click()
        cy.get(SIVU).contains('label', 'Selvitys jatkotoimista').should('not.exist')
      }

      peruutaVahvistus(0)
      cy.get(SIVU)
        .contains('label', arvioTeksti)
        .parent()
        .find('input[type="radio"]')
        .should('be.checked')
      if (!esitetaanHyvaksymista) {
        jatkotoimienKentta().find('textarea').should('have.value', JATKOTOIMET)
      }
      cy.apiRequest({ method: 'GET', url: `${API}/${lomake.id}` }).then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaTallennus(body, null, null, null)
      })

      vahvistaHyvaksynta().then(({ request, response }) => {
        expect(request.body).to.include({
          id: lomake.id,
          esitetaanKoejaksonHyvaksymista: esitetaanHyvaksymista,
          jatkotoimenpiteet,
          koejaksonPaattymispaiva: lomake.koejaksonPaattymispaiva
        })
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.lahikouluttaja.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        kouluttajanKuittausaika = response?.body.lahikouluttaja.kuittausaika
        tarkistaTallennus(response?.body, esitetaanHyvaksymista, kouluttajanKuittausaika, null)
      })
      cy.get('@hyvaksyntapyynto').should('have.been.calledOnce')
      cy.location('pathname').should('eq', '/koejakso')
      avaaArvioijanLomake('kouluttajanArvioHyvaksynnanJalkeen').then((tallennettu) => {
        tarkistaTallennus(tallennettu, esitetaanHyvaksymista, kouluttajanKuittausaika, null)
      })
      tarkistaArvioNakyvissa(esitetaanHyvaksymista)
      hyvaksyPainike().should('not.exist')

      cy.loginAsErikoistuva()
      avaaErikoistuvanKoejakso('/koejakso', 'koejaksoOdottaaEsihenkiloa').then((koejakso) => {
        tarkistaTallennus(
          koejakso.loppukeskustelu,
          esitetaanHyvaksymista,
          kouluttajanKuittausaika,
          null
        )
        expect(koejakso.loppukeskustelunTila).to.eq('ODOTTAA_HYVAKSYNTAA')
        expect(koejakso.vastuuhenkilonArvionTila).to.eq('EI_AKTIIVINEN')
      })
      vastuuhenkilonArvionLinkki().should('be.visible').and('have.attr', 'aria-disabled', 'true')

      cy.loginAsEsihenkilo(esihenkiloToken)
      avaaArvioijanLomake('esihenkilonArvioEnnenHyvaksyntaa').then((tallennettu) => {
        tarkistaTallennus(tallennettu, esitetaanHyvaksymista, kouluttajanKuittausaika, null)
      })
      tarkistaArvioNakyvissa(esitetaanHyvaksymista)
      peruutaVahvistus(1)
      cy.apiRequest({ method: 'GET', url: `${API}/${lomake.id}` }).then(({ status, body }) => {
        expect(status).to.eq(200)
        tarkistaTallennus(body, esitetaanHyvaksymista, kouluttajanKuittausaika, null)
      })

      vahvistaHyvaksynta().then(({ request, response }) => {
        expect(request.body).to.include({
          id: lomake.id,
          esitetaanKoejaksonHyvaksymista: esitetaanHyvaksymista,
          jatkotoimenpiteet,
          korjausehdotus: null
        })
        expect(request.body.lahikouluttaja.kuittausaika).to.eq(kouluttajanKuittausaika)
        expect(request.body.lahiesimies.id).to.eq(esihenkiloId)
        expect(response?.statusCode).to.eq(200)
        expect(response?.body.lahiesimies.kuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        esihenkilonKuittausaika = response?.body.lahiesimies.kuittausaika
        tarkistaTallennus(
          response?.body,
          esitetaanHyvaksymista,
          kouluttajanKuittausaika,
          esihenkilonKuittausaika
        )
      })
      cy.get('@hyvaksyntapyynto').should('have.been.calledTwice')
      cy.location('pathname').should('eq', '/koejakso')
      avaaArvioijanLomake('esihenkilonArvioHyvaksynnanJalkeen').then((tallennettu) => {
        tarkistaTallennus(
          tallennettu,
          esitetaanHyvaksymista,
          kouluttajanKuittausaika,
          esihenkilonKuittausaika
        )
      })
      cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
      tarkistaArvioNakyvissa(esitetaanHyvaksymista)
      hyvaksyPainike().should('not.exist')
      cy.get(SIVU).contains('button', 'Palauta muokattavaksi').should('not.exist')

      cy.loginAsErikoistuva()
      avaaErikoistuvanKoejakso(
        '/koejakso/loppukeskustelu',
        'erikoistuvanHyvaksyttyLoppukeskustelu'
      ).then((koejakso) => {
        tarkistaTallennus(
          koejakso.loppukeskustelu,
          esitetaanHyvaksymista,
          kouluttajanKuittausaika,
          esihenkilonKuittausaika
        )
        expect(koejakso.loppukeskustelunTila).to.eq('HYVAKSYTTY')
        // Both signed recommendations proceed to the responsible person's assessment.
        expect(koejakso.vastuuhenkilonArvionTila).to.eq('UUSI')
        expect(koejakso.valiarviointi.edistyminenTavoitteidenMukaista).to.eq(true)
      })
      cy.get(SIVU).contains('.alert-success', HYVAKSYTTY_TEKSTI).should('be.visible')
      tarkistaArvioNakyvissa(esitetaanHyvaksymista)
      const [vuosi, kuukausi, paiva] = lomake.koejaksonPaattymispaiva.split('-')
      cy.get(SIVU)
        .contains('h5', 'Koejakson päättymispäivä')
        .next('p')
        .should('contain.text', `${Number(paiva)}.${Number(kuukausi)}.${vuosi}`)
      cy.get(SIVU)
        .find('.hyvaksynta-pvm')
        .parent()
        .contains('p', ESIHENKILO_NIMI)
        .should('be.visible')
      cy.get(SIVU).contains('button', 'Tyhjennä lomake').should('not.exist')

      avaaErikoistuvanKoejakso('/koejakso', 'koejaksoLoppukeskustelunJalkeen').then((koejakso) => {
        expect(koejakso.loppukeskustelunTila).to.eq('HYVAKSYTTY')
        expect(koejakso.vastuuhenkilonArvionTila).to.eq('UUSI')
      })
      vastuuhenkilonArvionLinkki().should('be.visible').and('not.have.attr', 'aria-disabled')
      vastuuhenkilonArvionLinkki().should('have.attr', 'href', '/koejakso/vastuuhenkilon-arvio')
    })
  })
})
