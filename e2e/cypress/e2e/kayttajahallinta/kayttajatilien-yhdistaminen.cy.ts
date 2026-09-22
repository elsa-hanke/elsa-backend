import {
  E2E_ERIKOISTUVA_EMAIL,
  ESIHENKILÖ_EMAIL,
  KOULUTTAJA_EMAIL,
  VIRKAILIJA_EMAIL,
  MERGE_RETAINED_EMAIL,
  MERGE_SOURCE_EMAIL,
  SSN_ESIHENKILO,
  SSN_VIRKAILIJA
} from '../../support/commands/credentials'

const MAIN = 'main[role="main"]'
const OWNER_API = '/api/erikoistuva-laakari/koejakso'
const APPROVAL_API = '/api/kouluttaja/koejakso/aloituskeskustelu'
const OBJECTIVES = 'E2E yhdistämisen jälkeen säilyvät osaamistavoitteet'
const TRAINER = /^\s*Kouluttaja\s*\*/
const SUPERVISOR = /Lähiesihenkil[oö] tai vastaava/

type FixtureUser = { id: number; token: string }
type Fixture = { retained: FixtureUser; source: FixtureUser; admin: FixtureUser }
type Assessor = { id: number; sopimusHyvaksytty: boolean; kuittausaika: string | null }
type InitialDiscussion = {
  id: number
  lahetetty: boolean
  koejaksonOsaamistavoitteet: string
  lahikouluttaja: Assessor
  lahiesimies: Assessor
}

const field = (label: string | RegExp) => cy.get(MAIN).contains('label', label).parent()
const selectAssessor = (label: RegExp, email: string) => {
  field(label).find('.multiselect').click()
  cy.get('.multiselect--active .multiselect__option').contains(email).click()
}
const selectedAssessor = (label: RegExp, name: string) =>
  field(label).find('.multiselect__single').should('contain.text', name)

const loginInvitedUser = (role: string, ssn: string, token: string) => {
  cy.session(
    ['account-merge', role, token],
    () => {
      cy.loginWithSuomifi(ssn, undefined, token)
      cy.location('origin', { timeout: 60000 }).should(
        'eq',
        new URL(Cypress.config('baseUrl') as string).origin
      )
      cy.location('pathname').should('not.eq', '/kirjautuminen')
      cy.get(MAIN).should('exist')
    },
    {
      validate: () => {
        cy.visit('/etusivu')
        cy.location('pathname').should('not.eq', '/kirjautuminen')
      }
    }
  )
}

describe('Käyttäjätilien yhdistäminen säilyttää toisen erikoistujan koejaksolomakkeet', () => {
  let fixture: Fixture

  const cleanup = () => {
    cy.task('db:cleanupKoejakso', { erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL })
    cy.task('db:cleanupAccountMergeUsers')
  }

  beforeEach(() => {
    cleanup()
    // These existing test identities must not remain linked to another spec's accounts.
    cy.task('db:cleanupKouluttaja', { email: ESIHENKILÖ_EMAIL })
    cy.task('db:cleanupVirkailija', { email: VIRKAILIJA_EMAIL })
    cy.prepareKoejaksoE2e({ cleanupSupportUsers: true, storeTokens: true })
    cy.task<Fixture>('db:seedAccountMergeUsers', { ownerEmail: E2E_ERIKOISTUVA_EMAIL }).then(
      (result) => {
        fixture = result
        expect(fixture.source.id).not.to.eq(fixture.retained.id)
      }
    )
    cy.task('db:seedKouluttajavaltuutus', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: MERGE_SOURCE_EMAIL
    })
  })

  afterEach(cleanup)

  const mergeThroughUi = () => {
    loginInvitedUser('admin', SSN_VIRKAILIJA, fixture.admin.token)
    const requests = cy.spy().as('mergeRequests')
    cy.intercept('PATCH', '**/api/tekninen-paakayttaja/yhdista-kayttajatilit', (request) => {
      requests(request.body)
    }).as('mergeAccounts')
    cy.visit('/kayttajahallinta/yhdista-kayttajatileja')
    cy.get(MAIN).find('input[placeholder="Hae erikoistujaa / kouluttajaa"]').type('Testilä')
    for (const email of [MERGE_RETAINED_EMAIL, MERGE_SOURCE_EMAIL]) {
      // Bootstrap visually hides the native radio, and its label contains a navigation link.
      cy.get(MAIN).contains('tr', email).find('input[type="radio"]').check({ force: true })
    }
    field(/^\s*Sähköpostiosoite/)
      .find('input')
      .type(MERGE_RETAINED_EMAIL)
    field(/^\s*Vahvista sähköpostiosoite/)
      .find('input')
      .type(MERGE_RETAINED_EMAIL)
    const button = () => cy.get(MAIN).contains('button', /^\s*Yhdistä käyttäjätilit\s*$/)
    button().should('not.be.disabled').click()
    cy.get('#confirm-yhdista-kayttajatilit')
      .should('be.visible')
      .contains('button', 'Peruuta')
      .click()
    cy.get('body').find('#confirm-yhdista-kayttajatilit:visible').should('have.length', 0)
    cy.get('@mergeRequests').should('not.have.been.called')
    cy.task<Array<{ id: number }>>('db:readAccountMergeUsers').then((users) => {
      expect(users.map((user) => user.id)).to.include.members([
        fixture.source.id,
        fixture.retained.id
      ])
    })
    button().click()
    cy.get('#confirm-yhdista-kayttajatilit').contains('button', 'Yhdistä käyttäjätilit').click()
    cy.wait('@mergeAccounts').then(({ request, response }) => {
      expect(request.body).to.deep.eq({
        ensimmainenKayttajaId: fixture.retained.id,
        toinenKayttajaId: fixture.source.id,
        yhteinenSahkoposti: MERGE_RETAINED_EMAIL
      })
      expect(response?.statusCode).to.eq(200)
      expect(response?.body).to.be.an('array').and.not.be.empty
      expect(response?.body.every((stage: { onnistui: boolean }) => stage.onnistui)).to.eq(true)
    })
    cy.get('@mergeRequests').should('have.been.calledOnce')
    cy.contains('Käyttäjätilien yhdistäminen onnistui').should('be.visible')
    cy.location('pathname').should('eq', '/kayttajahallinta')
    cy.task<Array<{ id: number; email: string; roles: string[] }>>('db:readAccountMergeUsers').then(
      (users) => {
        expect(users.map((user) => user.id)).not.to.include(fixture.source.id)
        const retained = users.find((user) => user.id === fixture.retained.id)
        expect(retained?.email).to.eq(MERGE_RETAINED_EMAIL)
        expect(retained?.roles).to.have.members(['ROLE_ERIKOISTUVA_LAAKARI', 'ROLE_KOULUTTAJA'])
      }
    )
  }

  const saveAndReopenInitialDraft = (samePerson: boolean) => {
    cy.visit('/koejakso/aloituskeskustelu')
    selectAssessor(TRAINER, samePerson ? MERGE_SOURCE_EMAIL : KOULUTTAJA_EMAIL)
    selectAssessor(SUPERVISOR, MERGE_SOURCE_EMAIL)
    field(/Koejakson osaamistavoitteet/)
      .find('textarea')
      .clear()
      .type(OBJECTIVES)
    cy.intercept('POST', `**${OWNER_API}/aloituskeskustelu`).as('saveInitialDraft')
    cy.get(MAIN).contains('button', 'Tallenna keskeneräisenä').click()
    cy.get('#confirm-save').contains('button', 'Tallenna keskeneräisenä').click()
    cy.wait('@saveInitialDraft').its('response.statusCode').should('eq', 201)
    cy.location('pathname').should('eq', '/koejakso')
    cy.visit('/koejakso/aloituskeskustelu')
    selectedAssessor(TRAINER, samePerson ? 'Tessa Testilä' : 'Lassekalevi Hummaamistes')
    selectedAssessor(SUPERVISOR, 'Tessa Testilä')
    field(/Koejakson osaamistavoitteet/)
      .find('textarea')
      .should('have.value', OBJECTIVES)
    return cy.apiRequest({ method: 'GET', url: OWNER_API }).then(({ body }) => {
      const draft = body.aloituskeskustelu as InitialDiscussion
      expect(draft.lahetetty).to.eq(false)
      expect(draft.lahikouluttaja.id).to.eq(
        samePerson ? fixture.source.id : Cypress.env('kouluttajaId')
      )
      expect(draft.lahiesimies.id).to.eq(fixture.source.id)
      return draft
    })
  }

  it('yhdistää koulutussopimuksen ja aloituskeskustelun luonnoksiin valitun kouluttajan', () => {
    let contractId: number
    let discussionId: number
    cy.visit('/koejakso/koulutussopimus')
    field('Toimipaikan nimi').find('input').first().clear().type('E2E säilyvä koulutuspaikka')
    selectAssessor(TRAINER, MERGE_SOURCE_EMAIL)
    cy.intercept('POST', `**${OWNER_API}/koulutussopimus`).as('saveContractDraft')
    cy.get(MAIN).contains('button', 'Tallenna keskeneräisenä').click()
    cy.get('#confirm-save').contains('button', 'Tallenna keskeneräisenä').click()
    cy.wait('@saveContractDraft').then(({ response }) => {
      expect(response?.statusCode).to.eq(201)
      expect(response?.body.lahetetty).to.eq(false)
      expect(response?.body.kouluttajat[0].kayttajaId).to.eq(fixture.source.id)
      contractId = response?.body.id
    })
    cy.location('pathname').should('eq', '/koejakso')
    saveAndReopenInitialDraft(true).then((draft) => {
      discussionId = draft.id
    })
    cy.then(mergeThroughUi)
    cy.loginAsErikoistuva()
    cy.apiRequest({ method: 'GET', url: OWNER_API }).then(({ body }) => {
      expect(body.koulutussopimus).to.include({ id: contractId, lahetetty: false })
      expect(body.koulutussopimus.kouluttajat[0]).to.include({
        kayttajaId: fixture.retained.id,
        sopimusHyvaksytty: false
      })
      expect(body.aloituskeskustelu).to.include({
        id: discussionId,
        lahetetty: false,
        koejaksonOsaamistavoitteet: OBJECTIVES
      })
      for (const role of ['lahikouluttaja', 'lahiesimies']) {
        expect(body.aloituskeskustelu[role]).to.include({
          id: fixture.retained.id,
          sopimusHyvaksytty: false,
          kuittausaika: null
        })
      }
    })
    cy.visit('/koejakso/koulutussopimus')
    selectedAssessor(TRAINER, 'Tessa Testilä')
    field('Toimipaikan nimi')
      .find('input')
      .first()
      .should('have.value', 'E2E säilyvä koulutuspaikka')
    cy.visit('/koejakso/aloituskeskustelu')
    selectedAssessor(TRAINER, 'Tessa Testilä')
    selectedAssessor(SUPERVISOR, 'Tessa Testilä')
    field(/Koejakson osaamistavoitteet/)
      .find('textarea')
      .should('have.value', OBJECTIVES)
  })

  it('yhdistää lähiesihenkilön ennen kouluttajan hyväksyntää ja sallii hyväksynnän yhdistetyllä tilillä', () => {
    let discussion: InitialDiscussion
    cy.task('db:ensureKoulutussopimusHyvaksytty', {
      erikoistuvaEmail: E2E_ERIKOISTUVA_EMAIL,
      kouluttajaEmail: KOULUTTAJA_EMAIL
    })
    saveAndReopenInitialDraft(false).then((draft) => {
      const localDate = (date: Date) =>
        `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
          date.getDate()
        ).padStart(2, '0')}`
      const start = new Date()
      const end = new Date(start)
      end.setFullYear(end.getFullYear() + 1)
      // Submission is setup; the merge and the retained user's approval use real UI requests.
      cy.apiRequest({
        method: 'PUT',
        url: `${OWNER_API}/aloituskeskustelu`,
        body: {
          ...draft,
          erikoistuvanSahkoposti: E2E_ERIKOISTUVA_EMAIL,
          koejaksonSuorituspaikka: 'E2E Testisairaala',
          koejaksonAlkamispaiva: localDate(start),
          koejaksonPaattymispaiva: localDate(end),
          suoritettuKokoaikatyossa: true,
          lahetetty: true
        }
      }).then(({ status, body }) => {
        expect(status).to.eq(200)
        discussion = body
      })
    })
    cy.then(mergeThroughUi)
    cy.loginAsErikoistuva()
    cy.apiRequest({ method: 'GET', url: OWNER_API }).then(({ body }) => {
      expect(body.aloituskeskustelu).to.include({
        id: discussion.id,
        lahetetty: true,
        koejaksonOsaamistavoitteet: OBJECTIVES
      })
      expect(body.aloituskeskustelu.lahikouluttaja).to.include({
        id: Cypress.env('kouluttajaId'),
        sopimusHyvaksytty: false,
        kuittausaika: null
      })
      expect(body.aloituskeskustelu.lahiesimies).to.include({
        id: fixture.retained.id,
        sopimusHyvaksytty: false,
        kuittausaika: null
      })
      discussion = body.aloituskeskustelu
    })
    cy.loginAsKouluttaja(Cypress.env('kouluttajaToken'))
    cy.then(() => cy.apiRequest({ method: 'PUT', url: APPROVAL_API, body: discussion })).then(
      ({ status, body }) => {
        expect(status).to.eq(200)
        expect(body.lahikouluttaja.sopimusHyvaksytty).to.eq(true)
        expect(body.lahiesimies.sopimusHyvaksytty).to.eq(false)
      }
    )
    cy.then(() => loginInvitedUser('retained', SSN_ESIHENKILO, fixture.retained.token))
    cy.visit('/etusivu')
    cy.get('#navbar-top .user-dropdown > a').click()
    cy.intercept('POST', '**/api/vaihda-rooli').as('switchRole')
    cy.get('#navbar-top .dropdown-menu')
      .contains('a', /^\s*Kouluttaja\s*$/)
      .click()
    cy.wait('@switchRole').its('response.statusCode').should('eq', 204)
    cy.then(() => cy.visit(`/koejakso/aloituskeskustelu/${discussion.id}`))
    cy.get(MAIN).contains('p', OBJECTIVES).should('be.visible')
    cy.intercept('PUT', `**${APPROVAL_API}`).as('approveAsRetained')
    cy.get(MAIN).contains('button', 'Hyväksy ja lähetä').click()
    cy.get('#confirm-send').contains('button', 'Hyväksy ja lähetä').click()
    cy.wait('@approveAsRetained').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.lahiesimies).to.include({
        id: fixture.retained.id,
        sopimusHyvaksytty: true
      })
    })
    cy.loginAsErikoistuva()
    cy.intercept('GET', `**${OWNER_API}`).as('approvedForOwner')
    cy.visit('/koejakso/aloituskeskustelu')
    cy.wait('@approvedForOwner').then(({ response }) => {
      expect(response?.statusCode).to.eq(200)
      expect(response?.body.aloituskeskustelunTila).to.eq('HYVAKSYTTY')
      expect(response?.body.aloituskeskustelu.id).to.eq(discussion.id)
    })
    cy.get(MAIN)
      .contains('.alert-success', 'Aloituskeskustelu on hyväksytty kaikkien osapuolten toimesta.')
      .should('be.visible')
    cy.get(MAIN).contains('p', OBJECTIVES).should('be.visible')
  })
})
