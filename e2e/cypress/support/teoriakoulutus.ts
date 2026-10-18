export function fillRequiredFields(name: string, place: string) {
  cy.contains('label', 'Koulutuksen nimi')
    .parent()
    .find('input[type="text"]')
    .first()
    .clear()
    .type(name)

  cy.contains('label', 'Paikka').parent().find('input[type="text"]').first().clear().type(place)

  cy.contains('label', 'Alkamispäivä')
    .parent()
    .find('input.date-input, input[type="text"]')
    .first()
    .clear()
    .type('01.03.2025')
    .blur()

  cy.contains('label', 'Päättymispäivä')
    .parent()
    .find('input.date-input, input[type="text"]')
    .first()
    .clear()
    .type('02.03.2025')
    .blur()

  cy.contains('label', 'Erikoistumiseen hyväksyttävä tuntimäärä')
    .parent()
    .find('input')
    .first()
    .clear()
    .type('8')
}
