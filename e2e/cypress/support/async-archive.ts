type ExpectedArchive = {
  prefix: 'koejakso-sopimus' | 'koejakso-arvio' | 'valmistuminen'
  university: string
  caseId: number
  caseType: 'SOPIMUS' | 'KOEJAKSO' | 'VALMISTUMINEN'
  types: string[]
  metadata: Record<string, unknown>
  sourceIds?: Record<string, number>
}

type ArchiveRead = {
  count: number
  id: string
  university: string
  caseType: string
  status: string
  expectedDocumentCount: number
  externalId: string | null
  completedAt: string | null
  metadata: Record<string, unknown> & {
    documents: {
      sourceDocumentId: number
      snapshotDocumentId: number
      recordType: string
      jarjestysnumero: number
    }[]
  }
  schedule: {
    jobId: string
    picked: boolean
    executionTime: string
    due: boolean
    lastSuccess: string | null
    lastFailure: string | null
  }[]
  documents: {
    id: number
    type: string
    order: number
    size: number
    sha256: string
    snapshot: boolean
    sourceMatches: boolean
    storedSha256: string
  }[]
}

export function assertArchiveAbsent(prefix: string, university: string, caseId: number) {
  return cy
    .task('archive:read', { key: `${prefix}:${university}:${caseId}` }, { log: false })
    .should('eq', null)
}

export function assertArchiveProcessed(expected: ExpectedArchive) {
  const key = `${expected.prefix}:${expected.university}:${expected.caseId}`
  // Poll the committed result: Cypress tasks themselves are not retried by should().
  const readCompleted = (remaining = 60): Cypress.Chainable<ArchiveRead> =>
    cy.task<ArchiveRead | null>('archive:read', { key, sourceIds: expected.sourceIds }, { log: false })
      .then((job): Cypress.Chainable<ArchiveRead> => {
        if (job?.status === 'LAHETETTY' && job.schedule.length === 0) return cy.wrap(job, { log: false })
        if (remaining === 0 || job?.status === 'VAATII_TOIMENPITEITA') {
          throw new Error(`Archive ${key} did not complete: ${job?.status ?? 'missing'}`)
        }
        return cy.wait(500, { log: false }).then(() => readCompleted(remaining - 1))
      })
  return readCompleted()
    .then((result) => {
      expect(result, `committed archive job for ${key}`).not.to.eq(null)
      const job = result!
      expect(job.count).to.eq(1)
      expect(job.university).to.eq(expected.university)
      expect(job.caseType).to.eq(expected.caseType)
      expect(job.id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
      expect(job.status).to.eq('LAHETETTY')
      expect(job.externalId).to.eq(`e2e:${expected.university}:${job.id}`)
      expect(Number.isFinite(Date.parse(job.completedAt!))).to.eq(true)
      expect(job.schedule).to.have.length(0)
      expect(job.metadata).to.include({ ...expected.metadata, university: expected.university })
      expect(job.expectedDocumentCount).to.eq(expected.types.length)
      expect(job.documents.map((document) => document.type)).to.deep.eq(expected.types)
      expect(job.metadata.documents).to.have.length(expected.types.length)
      job.documents.forEach((document, order) => {
        const identity = job.metadata.documents[order]
        expect(identity).to.include({
          snapshotDocumentId: document.id, recordType: document.type, jarjestysnumero: order
        })
        expect(identity.sourceDocumentId).to.be.greaterThan(0)
        expect(identity.sourceDocumentId).not.to.eq(document.id)
        expect(document.order).to.eq(order)
        expect(document.snapshot).to.eq(true)
        expect(document.sourceMatches, `snapshot ${document.id} retains source bytes`).to.eq(true)
        expect(document.size).to.be.greaterThan(0)
        expect(document.sha256).to.eq(document.storedSha256)
      })
    })
}
