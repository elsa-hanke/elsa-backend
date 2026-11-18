import {
  acceptRequirements,
  fillContactInformation,
  fillLicensingInformation,
  openYekValmistumispyynto,
  setupYekValmistumispyynto,
  submitValmistumispyynto,
} from './valmistumispyynto-yek.helpers'

describe('YEK-valmistumispyynnön lähettäminen', () => {
  beforeEach(setupYekValmistumispyynto)

  it('YEK-koulutettava täyttää ja lähettää valmistumispyynnön', () => {
    openYekValmistumispyynto()
    acceptRequirements()
    fillContactInformation('+358401234567')
    fillLicensingInformation()

    submitValmistumispyynto('POST', 'postYekValmistumispyynto')

    cy.contains('Valmistumispyyntö lähetetty onnistuneesti').should('be.visible')
    cy.contains(
      'Valmistumispyyntö odottaa opintohallinnon tarkistusta sekä vastuuhenkilön lopullista hyväksyntää.'
    ).should('be.visible')
  })
})

export {}
