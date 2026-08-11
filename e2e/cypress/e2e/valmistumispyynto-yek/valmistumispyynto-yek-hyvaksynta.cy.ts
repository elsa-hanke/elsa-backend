import {
  VASTUUHENKILO_EMAIL,
} from '../../support/commands/credentials'
import {
  acceptRequirements,
  fillContactInformation,
  fillLicensingInformation,
  openYekGraduationRequest,
  setupYekGraduationRequest,
  submitGraduationRequest,
} from './valmistumispyynto-yek.helpers'

describe('YEK-valmistumispyynnön hyväksyntä', () => {
  beforeEach(setupYekGraduationRequest)

  it('YEK-valmistumispyyntö tarkistetaan, hyväksytään ja yhteenveto voidaan ladata', () => {
    openYekGraduationRequest()
    acceptRequirements()
    fillContactInformation('+358401234567')
    fillLicensingInformation()

    submitGraduationRequest('POST', 'postYekValmistumispyynto').then(
      (valmistumispyyntoId) => {
        cy.loginAsVirkailija(Cypress.env('virkailijaToken'))
        cy.apiRequest({
          method: 'GET',
          url: `/api/virkailija/valmistumispyynnon-tarkistus/${valmistumispyyntoId}`,
        }).its('status').should('eq', 200)
        cy.apiRequest({
          method: 'PUT',
          url: `/api/virkailija/valmistumispyynnon-tarkistus/${valmistumispyyntoId}`,
          form: true,
          body: {
            yekSuoritettu: true,
            yekSuorituspaiva: '2020-01-01',
            terveyskeskustyoTarkistettu: true,
            kokonaistyoaikaTarkistettu: true,
            teoriakoulutusTarkistettu: true,
            keskenerainen: false,
            virkailijanYhteenveto: 'YEK E2E -tarkistus valmis.',
          },
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.valmistumispyynto.virkailijanKuittausaika).to.not.be.null
        })

        cy.loginAsVastuuhenkilo(Cypress.env('vastuuhenkiloToken'))
        cy.apiRequest({
          method: 'GET',
          url: `/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${valmistumispyyntoId}`,
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.valmistumispyynto.tila).to.eq('ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA')
        })
        cy.apiRequest({
          method: 'PUT',
          url: `/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${valmistumispyyntoId}`,
          body: {
            sahkoposti: VASTUUHENKILO_EMAIL,
            puhelinnumero: '+358401112233',
          },
          timeout: 120000,
        }).then(({ status, body }) => {
          expect(status).to.eq(200)
          expect(body.valmistumispyynto.tila).to.eq('HYVAKSYTTY')
          expect(body.valmistumispyynto.yhteenvetoAsiakirjaId).to.be.a('number')
          expect(body.valmistumispyynto.liitteetAsiakirjaId).to.be.a('number')

          cy.apiRequest({
            method: 'GET',
            url: `/api/vastuuhenkilo/valmistumispyynto/${valmistumispyyntoId}/asiakirja/${body.valmistumispyynto.yhteenvetoAsiakirjaId}`,
            encoding: 'binary',
          }).then(({ status: downloadStatus, headers, body: documentBody }) => {
            expect(downloadStatus).to.eq(200)
            expect(headers['content-type']).to.include('application/pdf')
            expect(documentBody.length).to.be.greaterThan(0)
          })
        })
      }
    )
  })
})

export {}
