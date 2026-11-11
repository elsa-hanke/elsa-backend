import type { OpintoOikeus } from '../../plugins/db-tasks/opintooikeus'
import { E2E_ERIKOISTUVA_EMAIL, SSN_ERIKOISTUVA } from '../../support/commands/credentials'

type Role = 'erikoistuva-laakari' | 'yek-koulutettava'

type Upload = {
  files: string[]
  deleted: string[]
  status?: number
}

const PARTIAL_SAVE_MESSAGE =
  'Osa liitemuutoksista on jo tallennettu. Päivitä sivu ja tarkista liitteet ennen uutta yritystä.'

const yekOpintooikeus: OpintoOikeus = {
  id: 0,
  erikoistuva_laakari_id: 0,
  erikoisala_id: 61,
  yliopisto_id: 5,
  opintoopas_id: 17,
  asetus_id: 5,
  kaytossa: true,
  myontamispaiva: '2019-01-01',
  paattymispaiva: '2099-12-31',
  viimeinen_katselupaiva: '2099-12-31',
  muokkausaika: '2022-09-07',
  osaamisen_arvioinnin_oppaan_pvm: '2022-09-07',
  yliopisto_opintooikeus_id: 'e2e-yek-tk-attachments',
  tila: 'AKTIIVINEN',
  terveyskeskuskoulutusjakso_suoritettu: false,
  muokkausoikeudet_virkailijoilla: false,
}

function approvalData(periodCount: number) {
  return {
    id: null,
    erikoistuvanNimi: 'E2E Erikoistuva',
    erikoistuvanErikoisala: 'Yleislääketiede',
    erikoistuvanOpiskelijatunnus: '12345',
    erikoistuvanSyntymaaika: '1981-02-21',
    erikoistuvanYliopisto: 'TAMPERE',
    terveyskeskuskoulutusjaksonKesto: 365,
    laillistamispaiva: '2019-01-01',
    laillistamispaivanLiite: 'JVBERi0xLjQK',
    laillistamispaivanLiitteenNimi: 'laillistaminen.pdf',
    laillistamispaivanLiitteenTyyppi: 'application/pdf',
    asetus: 'E2E',
    yleislaaketieteenVastuuhenkilonNimi: 'E2E Vastuuhenkilö',
    tila: 'UUSI',
    tyoskentelyjaksot: Array.from({ length: periodCount }, (_, index) => ({
      id: 1001 + index,
      tyoskentelypaikka: {
        nimi: `E2E terveyskeskus ${index + 1}`,
        tyyppi: 'TERVEYSKESKUS',
        kunta: { abbreviation: 'Helsinki' },
      },
      alkamispaiva: '2024-01-01',
      paattymispaiva: '2024-12-31',
      osaaikaprosentti: 100,
      kaytannonKoulutus: 'TERVEYSKESKUSTYO',
      hyvaksyttyAiempaanErikoisalaan: false,
      poissaolot: [],
      asiakirjat: Array.from({ length: 3 }, (_, fileIndex) => ({
        id: 2001 + index * 10 + fileIndex,
        nimi: `old-${index + 1}-${fileIndex + 1}.pdf`,
        lisattypvm: '2024-01-01T00:00:00',
      })),
    })),
  }
}

function selectPdf(periodIndex: number, fileName: string) {
  cy.get('input[type="file"]').eq(periodIndex).selectFile({
    contents: 'cypress/fixtures/test.pdf',
    fileName,
    mimeType: 'application/pdf',
  }, { force: true })
  cy.contains('.asiakirjat-table tr', fileName).should('be.visible')
}

function deleteExistingPdf(fileName: string) {
  cy.contains('.asiakirjat-table tr', fileName).find('button').last().click()
  cy.get('.modal-content').contains('button', 'Poista').click()
  cy.contains('.asiakirjat-table tr', fileName).should('not.exist')
}

function submitApproval() {
  cy.contains('form button', 'Lähetä').click()
  cy.get('#confirm-erikoistuja').should('be.visible').contains('button', 'Lähetä').click()
}

function exerciseUploads(role: Role, failSecond: boolean) {
  const yek = role === 'yek-koulutettava'
  const path = yek
    ? '/yektyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta-yek'
    : '/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta'
  const periodCount = yek && failSecond ? 2 : 1
  const uploads: Upload[] = []
  const events: string[] = []
  let uploadCount = 0

  cy.intercept('GET', `**/api/${role}/tyoskentelyjaksot/terveyskeskuskoulutusjakso`, {
    statusCode: 200,
    body: approvalData(periodCount),
  }).as('approvalForm')
  cy.intercept('GET', `**/api/${role}/asiakirjat/nimet`, []).as('documentNames')
  cy.intercept('PUT', `**/api/${role}/tyoskentelyjaksot/*/asiakirjat`, (request) => {
    uploadCount += 1
    request.reply({ statusCode: failSecond && uploadCount === 2 ? 500 : 200, body: [] })
  }).as('saveDocuments')
  cy.intercept(`**/api/${role}/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta`, {
    statusCode: 200,
    body: {},
  }).as('sendApproval')

  cy.visit(path, {
    onBeforeLoad(win) {
      // Read the browser's original FormData. Cypress' network interception may
      // decode multipart file bytes, so request.body is not reliable here.
      const send = win.XMLHttpRequest.prototype.send
      cy.stub(win.XMLHttpRequest.prototype, 'send').callsFake(function (
        this: XMLHttpRequest,
        body?: Document | XMLHttpRequestBodyInit | null
      ) {
        if (body instanceof win.FormData) {
          const files = body.getAll('addedFiles').map((value) => (value as File).name)
          const deleted = body.getAll('deletedFiles').map(String)
          if (files.length || deleted.length) {
            const index = uploads.push({ files, deleted }) - 1
            events.push(`sent-${index}`)
            this.addEventListener('loadend', () => {
              uploads[index].status = this.status
              events.push(`finished-${index}`)
            }, { once: true })
          }
        }
        return send.call(this, body)
      })
    },
  })
  cy.wait(['@approvalForm', '@documentNames'])
  cy.get('.asiakirjat-table').should('have.length', periodCount)

  selectPdf(0, 'first-new.pdf')
  if (yek && failSecond) {
    selectPdf(1, 'second-new.pdf')
  } else {
    selectPdf(0, 'second-new.pdf')
  }
  deleteExistingPdf('old-1-1.pdf')
  submitApproval()

  cy.wait('@saveDocuments').its('response.statusCode').should('eq', 200)
  if (yek && !failSecond) {
    cy.wait('@sendApproval').its('response.statusCode').should('eq', 200)
    cy.location('pathname').should('eq', '/yektyoskentelyjaksot')
    cy.then(() => {
      expect(uploads).to.deep.equal([{
        files: ['first-new.pdf', 'second-new.pdf'],
        deleted: ['2001'],
        status: 200,
      }])
    })
    return
  }

  cy.wait('@saveDocuments').its('response.statusCode').should('eq', failSecond ? 500 : 200)
  if (failSecond) {
    cy.contains('.toast-body', PARTIAL_SAVE_MESSAGE).should('be.visible')
    cy.location('pathname').should('eq', path)
    cy.get('@sendApproval.all').should('have.length', 0)
  } else {
    cy.wait('@sendApproval').its('response.statusCode').should('eq', 200)
    cy.location('pathname').should('eq', '/tyoskentelyjaksot')
  }

  cy.then(() => {
    expect(events).to.deep.equal(['sent-0', 'finished-0', 'sent-1', 'finished-1'])
    if (yek) {
      expect(uploads).to.deep.equal([
        { files: ['first-new.pdf'], deleted: ['2001'], status: 200 },
        { files: ['second-new.pdf'], deleted: [], status: 500 },
      ])
    } else {
      expect(uploads).to.deep.equal([
        { files: ['first-new.pdf'], deleted: ['2001'], status: 200 },
        { files: ['second-new.pdf'], deleted: [], status: failSecond ? 500 : 200 },
      ])
    }
  })
}

describe('Terveyskeskuskoulutusjakson hyväksyntäpyynnön liitteet', () => {
  beforeEach(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
  })

  after(() => {
    cy.resetErikoistuvaE2eState()
  })

  it('lähettää kaksi uutta liitettä peräkkäin ja poistaa vanhan vain kerran', () => {
    exerciseUploads('erikoistuva-laakari', false)
  })

  it('näyttää osittaisen tallennuksen, kun toinen liitepyyntö epäonnistuu', () => {
    exerciseUploads('erikoistuva-laakari', true)
  })
})

describe('YEK terveyskeskuskoulutusjakson hyväksyntäpyynnön liitteet', () => {
  beforeEach(() => {
    cy.resetErikoistuvaE2eState()
    cy.loginAsErikoistuva()
    cy.logout()
    cy.task('db:seedOpintooikeus', {
      email: E2E_ERIKOISTUVA_EMAIL,
      opintoOikeus: { ...yekOpintooikeus },
      updateCurrent: true,
    })
    cy.task('db:prepareYekOnlyDocumentUser')
    cy.clearAllCookies()
    cy.clearAllLocalStorage()
    cy.loginWithSuomifi(SSN_ERIKOISTUVA)
    cy.location('origin', { timeout: 60000 }).should(
      'eq', new URL(Cypress.config('baseUrl') as string).origin
    )
    cy.request('/api/kayttaja').its('body.activeAuthority').should('eq', 'ROLE_YEK_KOULUTETTAVA')
  })

  after(() => {
    cy.resetErikoistuvaE2eState()
  })

  it('lähettää kaksi uutta liitettä ja poiston saman jakson yhdessä pyynnössä', () => {
    exerciseUploads('yek-koulutettava', false)
  })

  it('näyttää osittaisen tallennuksen, kun toisen jakson pyyntö epäonnistuu', () => {
    exerciseUploads('yek-koulutettava', true)
  })
})
