import {
  VASTUUHENKILO_EMAIL,
} from '../../support/commands/credentials'
import {
  APPROVAL_IN_PROGRESS_ALIAS,
  APPROVAL_IN_PROGRESS_TOAST,
  stubApprovalInProgress,
} from '../../support/approval-in-progress'
import { downloadPdfPages, expectTextInOrder } from '../../support/pdf'
import {
  acceptRequirements,
  fillContactInformation,
  fillLicensingInformation,
  openYekGraduationRequest,
  setupYekGraduationRequest,
  submitGraduationRequest,
} from './valmistumispyynto-yek.helpers'

/** Virkailija has checked the request, so it is waiting for the vastuuhenkilö's approval. */
function checkAsVirkailijaAndLoginAsVastuuhenkilo(valmistumispyyntoId: number) {
  cy.task<string | null>('tokens:get', 'virkailijaToken', { log: false }).then((token) => cy.loginAsVirkailija(token ?? undefined))
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

  cy.task<string | null>('tokens:get', 'vastuuhenkiloToken', { log: false }).then((token) => cy.loginAsVastuuhenkilo(token ?? undefined))
  cy.apiRequest({
    method: 'GET',
    url: `/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${valmistumispyyntoId}`,
  }).then(({ status, body }) => {
    expect(status).to.eq(200)
    expect(body.valmistumispyynto.tila).to.eq('ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA')
  })
}

describe('YEK-valmistumispyynnön hyväksyntä', () => {
  beforeEach(setupYekGraduationRequest)

  it('YEK-valmistumispyyntö tarkistetaan, hyväksytään ja yhteenveto voidaan ladata', () => {
    openYekGraduationRequest()
    acceptRequirements()
    fillContactInformation('+358401234567')
    fillLicensingInformation()

    submitGraduationRequest('POST', 'postYekValmistumispyynto').then(
      (valmistumispyyntoId) => {
        cy.task<string | null>('tokens:get', 'virkailijaToken', { log: false }).then((token) => cy.loginAsVirkailija(token ?? undefined))
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

        cy.task<string | null>('tokens:get', 'vastuuhenkiloToken', { log: false }).then((token) => cy.loginAsVastuuhenkilo(token ?? undefined))
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

          // Regression guard for PDF generation: the summary keeps its sections and data.
          downloadPdfPages(
            `/api/vastuuhenkilo/valmistumispyynto/${valmistumispyyntoId}/asiakirja/${body.valmistumispyynto.yhteenvetoAsiakirjaId}`
          ).then((pages) => {
            expect(pages.length).to.be.greaterThan(0)
            const text = pages.join(' ')
            expectTextInOrder(
              text,
              'YEK-koulutuksen valmistumisen yhteenveto',
              'Koulutettava lääkäri',
              'Teoriakoulutus',
              'Työskentelyjaksot',
              'Muut tarkistukset',
              'Tarkistanut'
            )
            expect(text).to.not.include('Erikoistumiskoulutuksen valmistumisen yhteenveto')
          })

          // The attachments PDF is a valid, parseable PDF.
          downloadPdfPages(
            `/api/vastuuhenkilo/valmistumispyynto/${valmistumispyyntoId}/asiakirja/${body.valmistumispyynto.liitteetAsiakirjaId}`
          ).its('length').should('be.greaterThan', 0)
        })
      }
    )
  })

  it('YEK-valmistumispyyntö: päällekkäisen hyväksynnän syy "Hyväksyntä on jo käynnissä" näytetään', () => {
    openYekGraduationRequest()
    acceptRequirements()
    fillContactInformation('+358401234567')
    fillLicensingInformation()

    submitGraduationRequest('POST', 'postYekValmistumispyynto').then((valmistumispyyntoId) => {
      checkAsVirkailijaAndLoginAsVastuuhenkilo(valmistumispyyntoId)

      // Another approval of the same valmistumispyyntö is already running: the server refuses
      // with 409 and the approver sees the root cause, not a generic failure.
      stubApprovalInProgress(valmistumispyyntoId)

      cy.visit(`/valmistumispyynnon-hyvaksynta-yek/${valmistumispyyntoId}`)
      cy.contains('button', 'Hyväksy valmistumispyyntö').click()
      cy.get('#confirm-send').should('be.visible').contains('button', 'Hyväksy').click()

      cy.wait(`@${APPROVAL_IN_PROGRESS_ALIAS}`).its('response.statusCode').should('eq', 409)
      cy.contains('.toast-body', APPROVAL_IN_PROGRESS_TOAST).should('be.visible')
      // The page stays open, so the approval can be retried.
      cy.location('pathname').should(
        'eq',
        `/valmistumispyynnon-hyvaksynta-yek/${valmistumispyyntoId}`
      )
    })
  })
})

export {}
