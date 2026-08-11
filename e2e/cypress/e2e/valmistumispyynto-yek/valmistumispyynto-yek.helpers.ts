import { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'
import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import {
  VASTUUHENKILO_EMAIL,
  VIRKAILIJA_EMAIL,
} from '../../support/commands/credentials'

const YEK_ROLE = 'ROLE_YEK_KOULUTETTAVA'

export const CORRECTION_PROPOSAL = 'Täydennä valmistumispyynnön yhteystiedot.'

const yekOpintooikeus: OpintoOikeus = {
  asetus_id: 5,
  erikoisala_id: 61,
  erikoistuva_laakari_id: 0,
  kaytossa: true,
  muokkausaika: '2021-01-04',
  muokkausoikeudet_virkailijoilla: true,
  myontamispaiva: '2021-01-04',
  opintoopas_id: 17,
  opiskelijatunnus: '',
  osaamisen_arvioinnin_oppaan_pvm: '2022-09-07',
  paattymispaiva: '2029-05-05',
  terveyskeskuskoulutusjakso_suoritettu: false,
  yliopisto_opintooikeus_id: 'e2e-yek-valmistumispyynto',
  tila: 'AKTIIVINEN',
  viimeinen_katselupaiva: '2029-11-05',
  yliopisto_id: 5,
  id: 0,
}

export function setupYekGraduationRequest() {
  cy.resetErikoistuvaE2eState()
  cy.seedKoejaksoSupportUsers({
    cleanupSupportUsers: true,
    seedVirkailija: true,
    storeTokens: true,
    vastuuhenkilo: {
      email: VASTUUHENKILO_EMAIL,
      etunimi: 'Mia',
      sukunimi: 'Ålands',
    },
    virkailija: {
      email: VIRKAILIJA_EMAIL,
      etunimi: 'Daniel',
      sukunimi: 'Siekkinen',
    },
  })
  cy.loginAsErikoistuva()
  cy.task('db:seedOpintooikeus', {
    email: E2E_ERIKOISTUVA_EMAIL,
    opintoOikeus: { ...yekOpintooikeus },
    updateCurrent: false,
    generateId: true,
  })
  cy.task('db:grantYekGraduationRoles', {
    vastuuhenkiloEmail: VASTUUHENKILO_EMAIL,
    virkailijaEmail: VIRKAILIJA_EMAIL,
    yliopistoId: yekOpintooikeus.yliopisto_id,
  })
  cy.logout()
  selectYekRole()
}

export function openYekGraduationRequest() {
  cy.intercept('GET', '**/yek-koulutettava/valmistumispyynto').as(
    'getYekValmistumispyynto'
  )
  cy.intercept('GET', '**/yek-koulutettava/valmistumispyynto-suoritusten-tila').as(
    'getYekValmistumispyyntoSuoritustenTila'
  )

  cy.visit('/yekvalmistumispyynto')
  cy.wait('@getYekValmistumispyynto').its('response.statusCode').should('eq', 200)
  cy.wait('@getYekValmistumispyyntoSuoritustenTila')
    .its('response.statusCode')
    .should('eq', 200)
  cy.contains('h1', 'Valmistumispyyntö').should('be.visible')
}

export function acceptRequirements() {
  cy.get('input[type="checkbox"]').should('have.length', 3).each(($checkbox) => {
    cy.wrap($checkbox).check({ force: true })
  })
  cy.contains('button', 'Lähetä valmistumispyyntö').click()
}

export function fillContactInformation(phoneNumber: string) {
  cy.contains('label', 'Sähköpostiosoite')
    .parent()
    .find('input')
    .clear()
    .type(E2E_ERIKOISTUVA_EMAIL)
  cy.contains('label', 'Matkapuhelinnumero')
    .parent()
    .find('input')
    .clear()
    .type(phoneNumber)
}

export function fillLicensingInformation() {
  cy.contains('label', 'Valviran laillistamispäivä')
    .parent()
    .find('input.date-input')
    .first()
    .clear()
    .type('01.01.2020')
    .blur()
  cy.get('input[type="file"]')
    .should('have.length', 1)
    .selectFile('cypress/fixtures/test.pdf', { force: true })
}

export function submitGraduationRequest(method: 'POST' | 'PUT', alias: string) {
  cy.intercept(method, '**/yek-koulutettava/valmistumispyynto').as(alias)
  cy.contains('button', 'Lähetä valmistumispyyntö').click()
  cy.get('#confirm-send').find('button').contains('Lähetä valmistumispyyntö').click()

  return cy.wait(`@${alias}`).then(({ request, response }) => {
    expect(response?.statusCode).to.eq(method === 'POST' ? 201 : 200)
    expect(request.headers['content-type']).to.be.a('string').and.include('multipart/form-data')
    expect(response?.body?.id).to.be.a('number')
    return response?.body?.id as number
  })
}

function selectYekRole() {
  cy.visit('/kirjautuminen')
  cy.contains('Kirjaudu sisään (Suomi.fi)').click()
  cy.origin('https://testi.apro.tunnistus.fi', () => {
    cy.get('body').then(($body) => {
      if ($body.find('[name="_eventId_proceed"]').length > 0) {
        cy.get('[name="_eventId_proceed"]').click()
      } else {
        cy.get('#continue-button').click()
      }
    })
  })
  cy.location('origin', { timeout: 60000 }).should(
    'eq',
    new URL(Cypress.config('baseUrl') as string).origin
  )

  cy.request('/api/kayttaja').then(({ body }) => {
    if (body.activeAuthority === YEK_ROLE) {
      return
    }

    cy.getCookie('XSRF-TOKEN').then((cookie) => {
      cy.request({
        method: 'POST',
        url: '/api/vaihda-rooli',
        form: true,
        body: { rooli: YEK_ROLE },
        headers: { 'X-XSRF-TOKEN': cookie?.value ?? '' },
      }).its('status').should('eq', 204)
    })
  })
  cy.request('/api/kayttaja').its('body.activeAuthority').should('eq', YEK_ROLE)
}

