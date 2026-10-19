import { E2E_ERIKOISTUVA_EMAIL, KOULUTTAJA_EMAIL } from '../../support/commands/credentials'

const SIVU = 'main[role="main"]'
const VAHVISTUS = '#confirm-send'
const ARVIOINNIN_API = '/api/vastuuhenkilo/valmistumispyynnon-arviointi'
const ERIKOISTUVAN_API = '/api/erikoistuva-laakari/valmistumispyynto'
const OSAAMINEN = 'Erikoistujan osaaminen on riittävällä tasolla valmistumista varten'
const LISATIEDOT = 'Lisätiedot erikoistujalle'
const EI_RIITA = 'Ei, osaaminen ei riitä valmistumiseen'
const KORJAUSEHDOTUS =
  'E2E: Harjoittele itsenäistä päätöksentekoa ja sovi uusi osaamisen arviointi.'
const VASTUUHENKILON_NIMI = 'Mia Ålands'

// The assessment GET returns fewer fields than the PUT and the resident GET.
type OsaamisenArviointi = {
  id: number
  opintooikeusId: number
  erikoistujanNimi: string
  erikoistujanErikoisala: string
  tila: string
  vastuuhenkiloOsaamisenArvioijaNimi: string | null
  vastuuhenkiloOsaamisenArvioijaKuittausaika: string | null
  vastuuhenkiloOsaamisenArvioijaPalautusaika: string | null
  vastuuhenkiloOsaamisenArvioijaKorjausehdotus: string | null
}

type Valmistumispyynto = OsaamisenArviointi & {
  erikoistujanKuittausaika: string | null
}

const kentta = (label: string) => cy.get(SIVU).contains('label', label).closest('.form-group')
const lahetaPainike = () => cy.get(SIVU).contains('button', /^\s*Tallenna ja lähetä\s*$/)
const valitseOsaaminen = (riittava: boolean) =>
  kentta(OSAAMINEN)
    .contains('label', riittava ? /^\s*Kyllä\s*$/ : EI_RIITA)
    .click()
const paivamaaraFi = (paiva: string) => {
  const [vuosi, kuukausi, pvm] = paiva.split('-')
  return `${Number(pvm)}.${Number(kuukausi)}.${vuosi}`
}

describe('Valmistumispyynnön osaamisen arviointi vastuuhenkilön käyttöliittymässä', () => {
  let valmistumispyynto: Valmistumispyynto

  const tarkistaSailyvatTiedot = (tallennettu: OsaamisenArviointi) => {
    expect(tallennettu).to.include({
      id: valmistumispyynto.id,
      opintooikeusId: valmistumispyynto.opintooikeusId,
      erikoistujanNimi: valmistumispyynto.erikoistujanNimi,
      erikoistujanErikoisala: valmistumispyynto.erikoistujanErikoisala
    })
  }

  const tarkistaOdottaaArviointia = (tallennettu: OsaamisenArviointi) => {
    tarkistaSailyvatTiedot(tallennettu)
    expect(tallennettu).to.include({
      tila: 'ODOTTAA_VASTUUHENKILON_TARKASTUSTA',
      vastuuhenkiloOsaamisenArvioijaKuittausaika: null,
      vastuuhenkiloOsaamisenArvioijaPalautusaika: null,
      vastuuhenkiloOsaamisenArvioijaKorjausehdotus: null
    })
  }

  const tarkistaPaatos = (
    tallennettu: OsaamisenArviointi,
    riittava: boolean,
    arviointipaiva: string
  ) => {
    tarkistaSailyvatTiedot(tallennettu)
    expect(tallennettu).to.include({
      tila: riittava ? 'ODOTTAA_VIRKAILIJAN_TARKASTUSTA' : 'VASTUUHENKILON_TARKASTUS_PALAUTETTU',
      vastuuhenkiloOsaamisenArvioijaNimi: VASTUUHENKILON_NIMI,
      vastuuhenkiloOsaamisenArvioijaKuittausaika: riittava ? arviointipaiva : null,
      vastuuhenkiloOsaamisenArvioijaPalautusaika: riittava ? null : arviointipaiva,
      vastuuhenkiloOsaamisenArvioijaKorjausehdotus: riittava ? null : KORJAUSEHDOTUS
    })
  }

  const tarkistaMyohemmatVaiheetKesken = (tallennettu: Valmistumispyynto) => {
    expect(tallennettu).to.include({
      virkailijanKuittausaika: null,
      virkailijanPalautusaika: null,
      vastuuhenkiloHyvaksyjaKuittausaika: null,
      vastuuhenkiloHyvaksyjaPalautusaika: null,
      yhteenvetoAsiakirjaId: null,
      liitteetAsiakirjaId: null,
      erikoistujanTiedotAsiakirjaId: null
    })
  }

  const avaaArviointi = (alias: string) => {
    // Each visit observes its own GET, including reopening after a decision.
    cy.intercept({
      method: 'GET',
      url: `**${ARVIOINNIN_API}/${valmistumispyynto.id}`,
      times: 1
    }).as(alias)
    cy.visit(`/valmistumispyynnon-arviointi/${valmistumispyynto.id}`)
    return cy.wait(`@${alias}`).then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      return response?.body as OsaamisenArviointi
    })
  }

  const tarkistaArviointiTallentamatta = () => {
    cy.get('@arviointipyynto').should('not.have.been.called')
    cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
    cy.apiRequest({
      method: 'GET',
      url: `${ARVIOINNIN_API}/${valmistumispyynto.id}`
    }).then(({ status, body }) => {
      expect(status).to.eq(200)
      tarkistaOdottaaArviointia(body)
    })
  }

  const avaaVahvistus = () => {
    lahetaPainike().should('be.visible').and('not.be.disabled').click()
    cy.get(VAHVISTUS)
      .should('be.visible')
      .and('contain.text', 'Vahvista lomakkeen lähetys')
      .and('contain.text', 'Vahvistuksen jälkeen erikoistujan osaaminen on arvioitu.')
    cy.get('@arviointipyynto').should('not.have.been.called')
  }

  const tarkistaPaatosNakyvissa = (riittava: boolean, arviointipaiva: string) => {
    cy.get(SIVU)
      .contains(
        '.alert-dark',
        riittava
          ? 'Erikoistujan osaaminen on arvioitu. Saat tiedon sähköpostiisi'
          : 'Valmistumispyyntö on palautettu takaisin erikoistujalle osaamisen arvioijan toimesta.'
      )
      .should('be.visible')
    kentta(OSAAMINEN)
      .should('be.visible')
      .and('contain.text', riittava ? 'Kyllä' : EI_RIITA)
    kentta(OSAAMINEN).find('input').should('not.exist')
    if (riittava) {
      cy.get(SIVU).contains('label', LISATIEDOT).should('not.exist')
    } else {
      kentta(LISATIEDOT).should('be.visible').and('contain.text', KORJAUSEHDOTUS)
      kentta(LISATIEDOT).find('textarea').should('not.exist')
    }
    cy.get(SIVU).find('.hyvaksynta-pvm').should('contain.text', paivamaaraFi(arviointipaiva))
    cy.get(SIVU)
      .contains('h5', 'Vastuuhenkilön nimi ja nimike')
      .parent()
      .find('p')
      .should('contain.text', VASTUUHENKILON_NIMI)
    lahetaPainike().should('not.exist')
    cy.get(SIVU).contains('a', 'Palaa valmistumispyyntöihin').should('be.visible')
  }

  before(() => {
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, seedVirkailija: true, storeTokens: true })
    cy.task('db:ensureLoppukeskusteluHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
  })

  beforeEach(() => {
    cy.loginAsErikoistuva()
    cy.task('db:cleanupValmistumispyynto', { email: E2E_ERIKOISTUVA_EMAIL })
    cy.submitValmistumispyyntoViaUi().then((id) => {
      return cy.apiRequest({ method: 'GET', url: ERIKOISTUVAN_API }).then(({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.id).to.eq(id)
        expect(body.erikoistujanKuittausaika).to.match(/^\d{4}-\d{2}-\d{2}$/)
        valmistumispyynto = body
        tarkistaOdottaaArviointia(body)
        tarkistaMyohemmatVaiheetKesken(body)
      })
    })
    cy.get(SIVU).contains('Valmistumispyyntö lähetetty').should('be.visible')

    cy.task<string | null>('tokens:get', 'vastuuhenkiloToken', { log: false }).then((token) => cy.loginAsVastuuhenkilo(token ?? undefined))
    const arviointipyynto = cy.spy().as('arviointipyynto')
    cy.then(() => {
      // Observe the real UI request; no approval response is stubbed.
      cy.intercept('PUT', `**${ARVIOINNIN_API}/${valmistumispyynto.id}`, (request) => {
        arviointipyynto(request.body)
      }).as('tallennaArvio')
      avaaArviointi('arviointiEnnenTallennusta').then(tarkistaOdottaaArviointia)
    })
    cy.get(SIVU).contains('h2', 'Osaamisen arviointi').should('be.visible')
    kentta(OSAAMINEN).find('input[type="radio"]').should('have.length', 2)
    kentta(OSAAMINEN).find('input:checked').should('not.exist')
    cy.get(SIVU).contains('label', LISATIEDOT).should('not.exist')
    lahetaPainike().should('be.visible').and('not.be.disabled')
  })

  after(() => {
    cy.task('db:cleanupErikoistuva', { email: E2E_ERIKOISTUVA_EMAIL })
  })

  it('vaatii osaamisen arvioinnin ja kielteiselle päätökselle lisätiedot', () => {
    lahetaPainike().click()
    kentta(OSAAMINEN).contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    tarkistaArviointiTallentamatta()

    valitseOsaaminen(false)
    lahetaPainike().click()
    kentta(LISATIEDOT).contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    tarkistaArviointiTallentamatta()

    // requiredIf in Vuelidate 0.7 accepts whitespace. Check an emptied field here;
    // rejecting whitespace-only explanations needs a separate production change.
    kentta(LISATIEDOT).find('textarea').type(KORJAUSEHDOTUS).clear().should('have.value', '')
    lahetaPainike().click()
    kentta(LISATIEDOT).contains('.invalid-feedback', 'Pakollinen tieto').should('be.visible')
    tarkistaArviointiTallentamatta()
  })

  for (const riittava of [true, false]) {
    it(
      riittava
        ? 'hyväksyy osaamisen vasta vahvistuksesta ja näyttää erikoistuvalle virkailijan tarkastusta odottavan pyynnön'
        : 'palauttaa pyynnön vasta vahvistuksesta ja näyttää erikoistuvalle säilyneen korjausehdotuksen',
      () => {
        let arviointipaiva: string
        valitseOsaaminen(riittava)
        if (!riittava) {
          kentta(LISATIEDOT).find('textarea').type(KORJAUSEHDOTUS)
        }

        avaaVahvistus()
        cy.get(VAHVISTUS).contains('button', 'Peruuta').click()
        tarkistaArviointiTallentamatta()
        kentta(OSAAMINEN).find('input:checked').should('have.value', String(riittava))
        if (!riittava) {
          kentta(LISATIEDOT).find('textarea').should('have.value', KORJAUSEHDOTUS)
        }

        avaaVahvistus()
        cy.get(VAHVISTUS).contains('button', 'Tallenna ja lähetä').click()
        cy.wait('@tallennaArvio').then(({ request, response }) => {
          expect(request.body).to.include({
            id: valmistumispyynto.id,
            osaaminenRiittavaValmistumiseen: riittava,
            korjausehdotus: riittava ? null : KORJAUSEHDOTUS
          })
          expect(response?.statusCode).to.eq(200)
          const tallennettu = response?.body as Valmistumispyynto
          const paiva = riittava
            ? tallennettu.vastuuhenkiloOsaamisenArvioijaKuittausaika
            : tallennettu.vastuuhenkiloOsaamisenArvioijaPalautusaika
          expect(paiva).to.match(/^\d{4}-\d{2}-\d{2}$/)
          arviointipaiva = paiva as string
          tarkistaPaatos(tallennettu, riittava, arviointipaiva)
          tarkistaMyohemmatVaiheetKesken(tallennettu)
          expect(tallennettu.erikoistujanKuittausaika).to.eq(
            riittava ? valmistumispyynto.erikoistujanKuittausaika : null
          )
        })
        cy.get('@arviointipyynto').should('have.been.calledOnce')
        cy.contains('Osaamisen arviointi tallennettu').should('be.visible')
        cy.get('body').find(`${VAHVISTUS}:visible`).should('have.length', 0)
        cy.then(() => {
          tarkistaPaatosNakyvissa(riittava, arviointipaiva)
          avaaArviointi('arviointiUudelleen').then((tallennettu) => {
            tarkistaPaatos(tallennettu, riittava, arviointipaiva)
          })
          tarkistaPaatosNakyvissa(riittava, arviointipaiva)
        })

        cy.loginAsErikoistuva()
        cy.intercept({ method: 'GET', url: `**${ERIKOISTUVAN_API}`, times: 1 }).as(
          'erikoistujanPyynto'
        )
        cy.visit('/valmistumispyynto')
        cy.wait('@erikoistujanPyynto').then(({ response }) => {
          expect(response?.statusCode).to.eq(200)
          const tallennettu = response?.body as Valmistumispyynto
          tarkistaPaatos(tallennettu, riittava, arviointipaiva)
          tarkistaMyohemmatVaiheetKesken(tallennettu)
          expect(tallennettu.erikoistujanKuittausaika).to.eq(
            riittava ? valmistumispyynto.erikoistujanKuittausaika : null
          )
        })
        cy.then(() => {
          if (riittava) {
            cy.get(SIVU)
              .contains(`Vastuuhenkilö tarkistanut osaamisen ${paivamaaraFi(arviointipaiva)}`)
              .should('be.visible')
            cy.get(SIVU)
              .contains('Opintohallinto ei ole vielä tarkistanut valmistumispyyntöä.')
              .should('be.visible')
            cy.get(SIVU)
              .contains('Vastuuhenkilö ei ole vielä hyväksynyt valmistumispyyntöä.')
              .should('be.visible')
            cy.get(SIVU).contains('button', 'Tee valmistumispyyntö').should('not.exist')
          } else {
            cy.get(SIVU)
              .find('.alert-danger')
              .should('be.visible')
              .and(
                'contain.text',
                'Valmistumispyyntö on palautettu takaisin vastuuhenkilön toimesta, joka arvioi osaamisen.'
              )
              .and('contain.text', KORJAUSEHDOTUS)
            cy.get(SIVU).contains('button', 'Tee valmistumispyyntö').should('be.visible')
          }
        })
      }
    )
  }
})
