import {
  acceptRequirements,
  fillContactInformation,
  fillLicensingInformation,
  openYekGraduationRequest,
  setupYekGraduationRequest,
  submitGraduationRequest,
} from './valmistumispyynto-yek.helpers'

describe('YEK-valmistumispyynnön lähettäminen', () => {
  beforeEach(setupYekGraduationRequest)

  it('YEK-koulutettava täyttää ja lähettää valmistumispyynnön', () => {
    openYekGraduationRequest()
    acceptRequirements()
    fillContactInformation('+358401234567')
    fillLicensingInformation()

    submitGraduationRequest('POST', 'postYekValmistumispyynto')

    cy.contains('Valmistumispyyntö lähetetty onnistuneesti').should('be.visible')
    cy.contains(
      'Valmistumispyyntö odottaa opintohallinnon tarkistusta sekä vastuuhenkilön lopullista hyväksyntää.'
    ).should('be.visible')
  })
})

export {}
