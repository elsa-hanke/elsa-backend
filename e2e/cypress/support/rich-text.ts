import corpus from '../../../src/test/resources/security/rich-text-cases.json'

export const safeHtml = `<h2>Hyvää &amp; tärkeää – Åland</h2>
  <p><strong>Ensimmäinen uutinen</strong></p><ul><li>Lista</li></ul>
  <table><tbody><tr><td colspan="2">Tieto</td></tr></tbody></table>
  <p><a href="https://example.org/ohje">Ohje</a></p>`

const example = (name: string) => {
  const entry = corpus.find((item) => item.name === name)
  if (!entry) throw new Error(`Missing rich-text contract case: ${name}`)
  // Observe both the application window and TinyMCE's same-origin iframe.
  // No spaces: some corpus event handlers use unquoted attribute values.
  return entry.html.replace(/alert\(1\)/g,
    'window.__richTextXss?window.__richTextXss():parent.__richTextXss()')
}

export const dirtyHtml = `${safeHtml}${example('event-handlers')}${example('hex')}
  ${example('mathml-mutation')}${example('css')}`

export function observeXss(win: Cypress.AUTWindow) {
  Object.defineProperty(win, '__richTextXss', { value: cy.stub().as('xssExecuted') })
}

export function assertNoExecution() {
  cy.get('@xssExecuted').should('not.have.been.called')
}

/** Assertions inspect parsed browser DOM, never call the production sanitizer as an oracle. */
export function assertSafeContent($root: JQuery<HTMLElement>, editor = false) {
  expect($root.find('h2').text()).to.contain('Hyvää & tärkeää – Åland')
  expect($root.find('strong').text()).to.contain('Ensimmäinen uutinen')
  expect($root.find('li').text()).to.contain('Lista')
  expect($root.find('td[colspan="2"]').text()).to.contain('Tieto')
  expect($root.find('a[href="https://example.org/ohje"]').text()).to.contain('Ohje')
  expect($root.find('script, iframe, img, svg, math, object, embed, form, input, style')).to.have.length(0)
  $root.find('*').each((_, element) => {
    for (const attribute of Array.from(element.attributes)) {
      expect(attribute.name).not.to.match(/^on/i)
      // TinyMCE owns some temporary styling/data attributes in its editing document.
      if (!editor) expect(attribute.name).not.to.match(/^(style|id|target)$/i)
    }
  })
  $root.find('a').each((_, element) => {
    const href = element.getAttribute('href')
    if (element.textContent === 'safe') expect(href).to.eq(null)
    if (href) expect(href.replace(/[\u0000-\u0020]/g, '')).not.to.match(/^(javascript|vbscript|data):/i)
  })
}

export function assertSafeHtml(html: string) {
  // DOMParser creates an inert document: this inspects the server's HTML without
  // DOMPurify or jQuery's executable HTML insertion normalising it first.
  const document = new DOMParser().parseFromString(html, 'text/html')
  assertSafeContent(Cypress.$(document.body))
}

export function announcementContent() {
  return cy.contains('.ilmoitus h2', 'Hyvää & tärkeää – Åland').parent()
}

export function editorBody() {
  return cy.get<HTMLIFrameElement>('iframe.tox-edit-area__iframe')
    .should(($frame) => {
      expect($frame[0].contentDocument?.body).to.exist
      expect($frame[0].contentDocument?.body.textContent).to.contain('Hyvää & tärkeää – Åland')
    })
    .then(($frame) => cy.wrap(Cypress.$($frame[0].contentDocument!.body)))
}
