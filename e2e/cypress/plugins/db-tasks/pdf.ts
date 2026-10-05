import { PDFParse } from 'pdf-parse'

/**
 * Node-side PDF helpers for Cypress. The browser side cannot parse PDFs, so specs download
 * the file with cy.request (base64) and hand it to this task to get the text of every page.
 */
export const pdfTasks = {
  async pdfPages({ base64 }: { base64: string }): Promise<string[]> {
    const parser = new PDFParse({ data: new Uint8Array(Buffer.from(base64, 'base64')) })
    try {
      const result = await parser.getText()
      return result.pages.map((page) => page.text.replace(/\s+/g, ' ').trim())
    } finally {
      await parser.destroy()
    }
  },
}
