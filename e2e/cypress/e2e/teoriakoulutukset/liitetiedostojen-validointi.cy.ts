import { fillRequiredFields } from '../../support/teoriakoulutus'

const API = '/api/erikoistuva-laakari'
const FORM = '/teoriakoulutukset/uusi'
const FIXTURES = 'cypress/fixtures/file-validation/'
const PREFIX = 'Uuden teoriakoulutuksen lisääminen epäonnistui: '
const messages = {
  invalidPdf:
    'Liitetiedostoa ei voitu käsitellä. Tarkista, että saat tiedoston aukeamaan normaalisti ennen lataamista ELSA-palveluun. Jos PDF-tiedosto avautuu normaalisti, tallenna se uudelleen PDF-muodossa ja yritä uudelleen.',
  password:
    'PDF-tiedosto vaatii salasanan avaamiseen. Tallenna tiedostosta kopio, joka avautuu ilman salasanaa, ja yritä uudelleen. Tarvittaessa pyydä tiedoston toimittajalta salasanasuojaamaton kopio.',
  duplicate:
    'Samanniminen tiedosto on jo olemassa. Tarkista tiedosto ja sen nimi. Jos toinen samanniminen tiedosto on jo lisätty ELSA-palveluun, anna tiedostolle toinen nimi, ja lataa se sitten uudelleen.',
  empty:
    'Liitetiedosto on tyhjä. Tarkista, että saat tiedoston aukeamaan normaalisti ennen lataamista ELSA-palveluun. Valitse tiedosto, jossa on sisältöä, ja yritä uudelleen.',
  unsupported: 'Sallitut tiedostoformaatit: .pdf, .png, .jpeg ja .jpg.',
  tooSmall: 'Tiedosto on liian pieni (PDF väh. 10 kt, muut tiedostot väh. 100 tavua).',
  tooLarge: 'Maksimi tiedostokoko yksittäiselle asiakirjalle: 20 Mt',
  totalSize: 'Asiakirjojen yhteenlaskettu koko ylitti sallitun maksimikoon: 100 Mt'
}

const selectFixture = (name: string) =>
  cy.get('main input[type="file"]').selectFile(FIXTURES + name, { force: true })
const save = () => cy.contains('button', 'Tallenna teoriakoulutus').click()
const alert = () => cy.get('main .alert-danger')

// Real backend responses are used for PDF and duplicate-name errors. Client-side
// failures reject the selected attachments before an upload request is sent.
describe('Teoriakoulutuksen liitetiedostojen virheilmoitukset', () => {
  let courseName: string
  let createdCourseIds: number[]
  let createdAttachmentIds: number[]

  before(() => cy.resetErikoistuvaE2eState())

  beforeEach(() => {
    createdCourseIds = []
    createdAttachmentIds = []
    courseName = `E2E liitevalidointi ${Date.now()}`
    cy.loginAsErikoistuva()
    const requests = cy.spy().as('saveRequests')
    cy.intercept('POST', `${API}/teoriakoulutukset`, (req) => {
      requests()
      req.continue((res) => {
        if (res.statusCode === 201) createdCourseIds.push(res.body.id)
      })
    }).as('saveCourse')
    openForm()
  })

  afterEach(() => {
    // Delete only records created by this test, including when a later assertion fails.
    cy.then(() => {
      createdCourseIds.forEach((id) => {
        cy.apiRequest({
          method: 'DELETE',
          url: `${API}/teoriakoulutukset/${id}`
        })
          .its('status')
          .should('eq', 204)
      })
      createdAttachmentIds.forEach((id) => {
        cy.apiRequest({ method: 'DELETE', url: `${API}/asiakirjat/${id}` })
          .its('status')
          .should('eq', 204)
      })
    })
  })

  function openForm() {
    cy.intercept('GET', `${API}/asiakirjat/nimet`).as('reservedNames')
    cy.visit(FORM)
    cy.wait('@reservedNames').its('response.statusCode').should('eq', 200)
    fillRequiredFields(courseName, 'E2E Testipaikka')
  }

  function expectRejectedSave(key: string, message: string) {
    cy.wait('@saveCourse').then(({ response }) => {
      expect(response?.statusCode).to.eq(400)
      expect(response?.body.message).to.eq(`error.dataillegal.${key}`)
    })
    cy.get('.toast-body').should('contain.text', PREFIX + message)
    cy.location('pathname').should('eq', FORM)
    cy.contains('button', 'Tallenna teoriakoulutus').should('not.be.disabled')
    cy.contains('label', 'Koulutuksen nimi')
      .parent()
      .find('input')
      .first()
      .should('have.value', courseName)
    cy.apiRequest({ method: 'GET', url: `${API}/teoriakoulutukset` }).then(({ body }) => {
      expect(
        body.teoriakoulutukset.filter(
          (course: { koulutuksenNimi: string }) => course.koulutuksenNimi === courseName
        )
      ).to.have.length(0)
    })
  }

  ;[
    ['invalid-content.pdf', 'pdf-tiedostoa-ei-voitu-kasitella', messages.invalidPdf],
    ['truncated.pdf', 'pdf-tiedostoa-ei-voitu-kasitella', messages.invalidPdf],
    ['no-pages.pdf', 'pdf-tiedostoa-ei-voitu-kasitella', messages.invalidPdf],
    ['password-required.pdf', 'pdf-tiedosto-vaatii-salasanan', messages.password]
  ].forEach(([file, key, message]) => {
    it(`näyttää backendin täsmällisen virheen: ${file}`, () => {
      selectFixture(file)
      cy.contains('.asiakirjat-table', file).should('be.visible')
      save()
      expectRejectedSave(key, message)
      cy.contains('.asiakirjat-table', file).should('be.visible')
    })
  })
  ;[
    ['empty.pdf', messages.empty],
    ['unsupported.txt', messages.unsupported],
    ['too-small.pdf', messages.tooSmall]
  ].forEach(([file, message]) => {
    it(`näyttää selaimen täsmällisen virheen: ${file}`, () => {
      selectFixture(file)
      alert().should('be.visible').and('contain.text', message)
      cy.get('.asiakirjat-table').should('not.exist')
      cy.get('@saveRequests').should('not.have.been.called')
      if (file === 'empty.pdf') alert().should('not.contain.text', messages.tooSmall)
    })
  })

  it('näyttää liian suuren tiedoston virheen', () => {
    cy.get('main input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.alloc(21 * 1024 * 1024),
        fileName: 'too-large.pdf',
        mimeType: 'application/pdf'
      },
      { force: true }
    )
    alert().should('contain.text', messages.tooLarge)
    cy.get('.asiakirjat-table').should('not.exist')
    cy.get('@saveRequests').should('not.have.been.called')
  })

  it('näyttää tiedostojen yhteenlasketun koon virheen', () => {
    const files = Array.from({ length: 6 }, (_, index) => ({
      contents: Cypress.Buffer.alloc(17 * 1024 * 1024),
      fileName: `total-size-${index + 1}.pdf`,
      mimeType: 'application/pdf'
    }))
    cy.get('main input[type="file"]').selectFile(files, { force: true })
    alert().should('contain.text', messages.totalSize).and('not.contain.text', messages.tooLarge)
    cy.get('.asiakirjat-table').should('not.exist')
    cy.get('@saveRequests').should('not.have.been.called')
  })

  it('näyttää tyhjän ja liian pienen tiedoston nimet oikeissa virheissä', () => {
    cy.get('main input[type="file"]').selectFile(
      [FIXTURES + 'empty.pdf', FIXTURES + 'too-small.pdf'],
      { force: true }
    )
    alert()
      .contains('span', messages.empty)
      .should('contain.text', 'empty.pdf')
      .and('not.contain.text', 'too-small.pdf')
    alert()
      .contains('span', messages.tooSmall)
      .should('contain.text', 'too-small.pdf')
      .and('not.contain.text', 'empty.pdf')
    cy.get('@saveRequests').should('not.have.been.called')
  })

  it('näyttää samannimisen liitteen virheen jo tiedostoa valittaessa', () => {
    cy.fixture('test.pdf', null).then((contents) => {
      const file = {
        contents,
        fileName: 'duplicate-in-form.pdf',
        mimeType: 'application/pdf'
      }
      cy.get('main input[type="file"]').selectFile(file, { force: true })
      cy.get('main input[type="file"]').selectFile(file, { force: true })
    })
    alert().should('contain.text', messages.duplicate)
    cy.get('.asiakirjat-table tbody tr').should('have.length', 1)
    cy.get('@saveRequests').should('not.have.been.called')
  })

  function reserveNameAfterFormWasOpened(name: string) {
    // Simulate another tab saving this name after this form fetched reserved names.
    // This exercises real server-side duplicate detection without stubbing a response.
    cy.fixture('test.pdf', 'base64').then((data) => {
      cy.getCookie('XSRF-TOKEN').then((cookie) => {
        cy.window().then(async (win) => {
          const form = new win.FormData()
          form.append('files', Cypress.Blob.base64StringToBlob(data, 'application/pdf'), name)
          const response = await win.fetch(`${API}/asiakirjat`, {
            method: 'POST',
            headers: {
              'X-XSRF-TOKEN': decodeURIComponent(cookie?.value ?? '')
            },
            body: form
          })
          expect(response.status).to.eq(201)
          const attachments = await response.json()
          createdAttachmentIds.push(
            ...attachments.map((attachment: { id: number }) => attachment.id)
          )
        })
      })
    })
  }

  it('näyttää backendin samannimisen tiedoston virheen vanhentuneella nimilistalla', () => {
    const name = 'duplicate-saved-in-another-tab.pdf'
    reserveNameAfterFormWasOpened(name)
    cy.fixture('test.pdf', null).then((contents) => {
      cy.get('main input[type="file"]').selectFile(
        {
          contents,
          fileName: name,
          mimeType: 'application/pdf'
        },
        { force: true }
      )
    })
    cy.contains('.asiakirjat-table', name).should('be.visible')
    save()
    expectRejectedSave('samanniminen-tiedosto-on-jo-olemassa', messages.duplicate)
  })

  it('näyttää muualla tallennetun tiedoston nimivirheen jo valittaessa', () => {
    const name = 'duplicate-existing-attachment.pdf'
    reserveNameAfterFormWasOpened(name)
    openForm()
    cy.fixture('test.pdf', null).then((contents) => {
      cy.get('main input[type="file"]').selectFile(
        {
          contents,
          fileName: name,
          mimeType: 'application/pdf'
        },
        { force: true }
      )
    })
    alert().should('contain.text', messages.duplicate)
    cy.get('.asiakirjat-table').should('not.exist')
    cy.get('@saveRequests').should('not.have.been.called')
  })

  it('hyväksyy ilman salasanaa avautuvan suojatun PDF:n ja näyttää sen uudelleen avattaessa', () => {
    selectFixture('copy-restricted.pdf')
    save()
    cy.wait('@saveCourse').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body.id).to.be.a('number')
      cy.location('pathname').should('not.eq', FORM)
      cy.visit(`/teoriakoulutukset/${response?.body.id}`)
    })
    cy.contains(courseName).should('be.visible')
    cy.contains('.asiakirjat-table', 'copy-restricted.pdf').should('be.visible')
  })
})
