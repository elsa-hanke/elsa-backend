import { E2E_ERIKOISTUVA_EMAIL } from '../../support/commands'
import {
  acceptRequirements,
  CORRECTION_PROPOSAL,
  fillContactInformation,
  fillLicensingInformation,
  openYekGraduationRequest,
  setupYekGraduationRequest,
  submitGraduationRequest,
} from './valmistumispyynto-yek.helpers'

describe('Palautetun YEK-valmistumispyynnön lähettäminen', () => {
  beforeEach(setupYekGraduationRequest)

  it('YEK-koulutettava lähettää virkailijan palauttaman pyynnön uudelleen', () => {
    cy.task('db:seedReturnedYekValmistumispyynto', {
      email: E2E_ERIKOISTUVA_EMAIL,
      correctionProposal: CORRECTION_PROPOSAL,
    }).then((requestId) => {
      openYekGraduationRequest()
      cy.contains('Valmistumispyyntö on palautettu takaisin virkailijan toimesta.').should(
        'be.visible'
      )
      cy.contains(CORRECTION_PROPOSAL).should('be.visible')

      acceptRequirements()
      fillContactInformation('+358409876543')
      fillLicensingInformation()
      submitGraduationRequest('PUT', 'putYekValmistumispyynto').should('eq', requestId)

      cy.contains('Valmistumispyyntö lähetetty onnistuneesti').should('be.visible')
      cy.contains(
        'Valmistumispyyntö odottaa opintohallinnon tarkistusta sekä vastuuhenkilön lopullista hyväksyntää.'
      ).should('be.visible')
    })
  })
})

export {}
