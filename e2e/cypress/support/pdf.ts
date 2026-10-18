/**
 * Downloads a PDF through the API (with the XSRF token) and returns the whitespace-normalised
 * text of each page. Also asserts that the response really is a PDF.
 */
export function downloadPdfPages(url: string): Cypress.Chainable<string[]> {
  return cy
    .apiRequest({ method: 'GET', url, encoding: 'base64' })
    .then(({ status, headers, body }) => {
      expect(status).to.eq(200)
      expect(headers['content-type']).to.include('application/pdf')
      return cy.task<string[]>('pdfPages', { base64: body as string })
    })
}

/**
 * Asserts that every fragment is found in the text and that the fragments appear in the
 * given order (each fragment is searched for after the previous one).
 */
export function expectTextInOrder(text: string, ...fragments: string[]): void {
  let from = 0
  fragments.forEach((fragment) => {
    const index = text.indexOf(fragment, from)
    expect(index, `"${fragment}" after position ${from}`).to.be.at.least(0)
    from = index + fragment.length
  })
}
