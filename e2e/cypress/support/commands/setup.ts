import {
  E2E_ERIKOISTUVA_EMAIL,
  KOULUTTAJA_EMAIL,
  VASTUUHENKILO_EMAIL,
  VIRKAILIJA_EMAIL,
} from './credentials'

type SeedUser = {
  email: string
  etunimi: string
  sukunimi: string
}

type KoejaksoSetupOptions = {
  erikoistuvaEmail?: string
  cleanupSupportUsers?: boolean
  seedVirkailija?: boolean
  kouluttaja?: SeedUser
  vastuuhenkilo?: SeedUser
  virkailija?: SeedUser
  storeTokens?: boolean
}

type SeurantajaksoApiOptions = {
  kouluttajaId: number
  yhteisetMerkinnat?: string
}

type VirkailijaSetupOptions = {
  cleanupEmails?: string[]
}

const defaultKouluttaja: SeedUser = {
  email: KOULUTTAJA_EMAIL,
  etunimi: 'E2E',
  sukunimi: 'Kouluttaja',
}

const defaultVastuuhenkilo: SeedUser = {
  email: VASTUUHENKILO_EMAIL,
  etunimi: 'E2E',
  sukunimi: 'Vastuuhenkilo',
}

const defaultVirkailija: SeedUser = {
  email: VIRKAILIJA_EMAIL,
  etunimi: 'E2E',
  sukunimi: 'Virkailija',
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Clears cached Cypress sessions and removes the default erikoistuva test user.
       */
      resetErikoistuvaE2eState(email?: string): void

      /**
       * Clears cached Cypress sessions and koejakso-safe test data for an erikoistuva.
       */
      resetKoejaksoE2eState(erikoistuvaEmail?: string): void

      /**
       * Seeds the shared koejakso support users used by e2e specs.
       */
      seedKoejaksoSupportUsers(options?: KoejaksoSetupOptions): void

      /**
       * Seeds a single kouluttaja support user and optionally stores its token in Cypress.env.
       */
      seedKouluttajaUser(user: SeedUser, tokenEnvKey?: string): void

      /**
       * Seeds a single vastuuhenkilo support user and optionally stores its token in Cypress.env.
       */
      seedVastuuhenkiloUser(user: SeedUser, tokenEnvKey?: string): void

      /**
       * Resets koejakso state, seeds support users, logs in as erikoistuva, and seeds kouluttajavaltuutus.
       */
      prepareKoejaksoE2e(options?: KoejaksoSetupOptions): void

      /**
       * Creates a monitoring period through the API for a logged-in resident.
       * Optionally adds the shared discussion notes needed for return/approval scenarios.
       */
      createSeurantajaksoViaApi(options: SeurantajaksoApiOptions): Chainable<any>

      /**
       * Resets and seeds the shared university official used by user-management specs.
       */
      prepareVirkailijaE2e(options?: VirkailijaSetupOptions): void
    }
  }
}

Cypress.Commands.add('resetErikoistuvaE2eState', (email = E2E_ERIKOISTUVA_EMAIL) => {
  Cypress.session.clearAllSavedSessions()
  cy.task('db:cleanupErikoistuva', { email })
})

Cypress.Commands.add('resetKoejaksoE2eState', (erikoistuvaEmail = E2E_ERIKOISTUVA_EMAIL) => {
  Cypress.session.clearAllSavedSessions()
  cy.task('db:cleanupKoejakso', { erikoistuvaEmail })
  cy.task('db:cleanupErikoistuva', { email: erikoistuvaEmail })
})

const seedKouluttaja = (user: SeedUser, storeTokens: boolean) => {
  return cy.task('db:seedKouluttaja', user).then((result: any) => {
    Cypress.env('kouluttajaId', result?.kayttajaId)
    if (storeTokens) {
      Cypress.env('kouluttajaToken', result?.token)
    }
  })
}

Cypress.Commands.add('seedKouluttajaUser', (user: SeedUser, tokenEnvKey?: string) => {
  cy.task('db:seedKouluttaja', user).then((result: any) => {
    if (tokenEnvKey) {
      Cypress.env(tokenEnvKey, result?.token)
    }
  })
})

const seedVastuuhenkilo = (user: SeedUser, storeTokens: boolean) => {
  return cy.task('db:seedVastuuhenkilo', user).then((result: any) => {
    Cypress.env('vastuuhenkiloId', result?.kayttajaId)
    if (storeTokens) {
      Cypress.env('vastuuhenkiloToken', result?.token)
    }
  })
}

Cypress.Commands.add('seedVastuuhenkiloUser', (user: SeedUser, tokenEnvKey?: string) => {
  cy.task('db:seedVastuuhenkilo', user).then((result: any) => {
    if (tokenEnvKey) {
      Cypress.env(tokenEnvKey, result?.token)
    }
  })
})

const seedVirkailija = (user: SeedUser, storeTokens: boolean) => {
  return cy.task('db:seedVirkailija', user).then((result: any) => {
    Cypress.env('virkailijaId', result?.kayttajaId)
    if (storeTokens) {
      Cypress.env('virkailijaToken', result?.token)
    }
  })
}

Cypress.Commands.add('seedKoejaksoSupportUsers', (options: KoejaksoSetupOptions = {}) => {
  const kouluttaja = options.kouluttaja ?? defaultKouluttaja
  const vastuuhenkilo = options.vastuuhenkilo ?? defaultVastuuhenkilo
  const virkailija = options.virkailija ?? defaultVirkailija
  const storeTokens = options.storeTokens ?? false

  if (options.cleanupSupportUsers) {
    cy.task('db:cleanupKouluttaja', { email: kouluttaja.email })
    cy.task('db:cleanupVastuuhenkilo', { email: vastuuhenkilo.email })
    if (options.seedVirkailija) {
      cy.task('db:cleanupVirkailija', { email: virkailija.email })
    }
  }

  seedKouluttaja(kouluttaja, storeTokens)
  seedVastuuhenkilo(vastuuhenkilo, storeTokens)

  if (options.seedVirkailija) {
    seedVirkailija(virkailija, storeTokens)
  }
})

Cypress.Commands.add('prepareKoejaksoE2e', (options: KoejaksoSetupOptions = {}) => {
  const erikoistuvaEmail = options.erikoistuvaEmail ?? E2E_ERIKOISTUVA_EMAIL
  const kouluttaja = options.kouluttaja ?? defaultKouluttaja

  cy.resetKoejaksoE2eState(erikoistuvaEmail)
  cy.seedKoejaksoSupportUsers(options)
  cy.loginAsErikoistuva()
  cy.task('db:seedKouluttajavaltuutus', {
    erikoistuvaEmail,
    kouluttajaEmail: kouluttaja.email,
  })
})

Cypress.Commands.add('createSeurantajaksoViaApi', (options: SeurantajaksoApiOptions) => {
  const requestBody = {
    alkamispaiva: '2025-01-01',
    paattymispaiva: '2025-06-30',
    omaArviointi: 'E2E oma arviointi seurantajaksolta.',
    lisahuomioita: 'E2E lisähuomiot.',
    seuraavanJaksonTavoitteet: 'E2E seuraavan jakson tavoitteet.',
    kouluttaja: { id: options.kouluttajaId },
    koulutusjaksot: [],
  }

  return cy
    .apiRequest({
      method: 'POST',
      url: '/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso',
      body: requestBody,
    })
    .then(({ status, body }) => {
      expect(status).to.eq(201)

      if (!options.yhteisetMerkinnat) {
        return body
      }

      return cy
        .apiRequest({
          method: 'PUT',
          url: `/api/erikoistuva-laakari/seurantakeskustelut/seurantajakso/${body.id}`,
          body: {
            ...body,
            seurantakeskustelunYhteisetMerkinnat: options.yhteisetMerkinnat,
          },
        })
        .then(({ status: updateStatus, body: updatedBody }) => {
          expect(updateStatus).to.eq(200)
          return updatedBody
        })
    })
})

Cypress.Commands.add('prepareVirkailijaE2e', (options: VirkailijaSetupOptions = {}) => {
  Cypress.session.clearAllSavedSessions()
  for (const email of options.cleanupEmails ?? []) {
    cy.task('db:cleanupVirkailija', { email })
  }
  cy.task('db:cleanupVirkailija', { email: VIRKAILIJA_EMAIL })
  cy.task('db:seedVirkailija', defaultVirkailija).then((result: any) => {
    Cypress.env('virkailijaId', result?.kayttajaId)
    Cypress.env('virkailijaToken', result?.token)
  })
})

export {}
