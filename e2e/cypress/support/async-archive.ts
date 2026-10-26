type ExpectedArchive = {
  prefix: 'koejakso-sopimus' | 'koejakso-arvio' | 'valmistuminen'
  university: string
  caseId: number
  caseType: 'SOPIMUS' | 'KOEJAKSO' | 'VALMISTUMINEN'
  types: string[]
  metadata: Record<string, unknown>
  sourceIds?: Record<string, number>
}

type DocumentReceipt = {
  id: number
  type: string
  order: number
  size: number
  sha256: string
  contentType: string
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
  scheduled: number
  documents: (DocumentReceipt & {
    snapshot: boolean
    sourceMatches: boolean
    storedSha256: string
  })[]
  receipt: null | {
    job_id: string
    delivery_key: string
    university: string
    case_type: string
    invocation_count: number
    metadata: Record<string, unknown>
    documents: DocumentReceipt[]
  }
}

export const assertFreshArchiveStack = () =>
  cy.task('archive:assertFreshStack', null, { log: false })

export function assertArchiveAbsent(prefix: string, university: string, caseId: number) {
  return cy
    .task('archive:read', { key: `${prefix}:${university}:${caseId}` }, { log: false })
    .should('eq', null)
}

export function assertArchiveDelivered(expected: ExpectedArchive) {
  const key = `${expected.prefix}:${expected.university}:${expected.caseId}`
  const deadline = Date.now() + 60000
  const poll = (): Cypress.Chainable<ArchiveRead> =>
    cy
      .task<ArchiveRead | null>(
        'archive:read',
        { key, sourceIds: expected.sourceIds },
        { log: false }
      )
      .then((result) => {
        if (result?.status === 'VAATII_TOIMENPITEITA')
          throw new Error(`Archive job ${result.id} requires attention`)
        if (result?.status === 'LAHETETTY' && result.receipt && result.scheduled === 0)
          return cy.wrap(result, { log: false })
        if (Date.now() >= deadline) throw new Error(`Archive delivery did not complete for ${key}`)
        // Repeat the actual DB query: .should() alone does not rerun cy.task().
        return cy.wait(250, { log: false }).then(poll)
      })
  return poll().then((result) => {
    expect(result.count).to.eq(1)
    expect(result.university).to.eq(expected.university)
    expect(result.caseType).to.eq(expected.caseType)
    expect(result.id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
    expect(result.externalId).to.eq(`e2e:${result.id}`)
    expect(result.completedAt).not.to.eq(null)
    expect(result.expectedDocumentCount).to.eq(expected.types.length)
    const receipt = result.receipt!
    expect(receipt).to.include({
      job_id: result.id,
      delivery_key: result.id,
      university: expected.university,
      case_type: expected.caseType,
      invocation_count: 1
    })
    expect(receipt.metadata).to.include({ ...expected.metadata, university: expected.university })
    expect(result.documents.map((document) => document.type)).to.deep.eq(expected.types)
    expect(receipt.documents).to.have.length(expected.types.length)
    result.documents.forEach((document, order) => {
      expect(document.order).to.eq(order)
      expect(document.snapshot).to.eq(true)
      expect(document.sourceMatches, `snapshot ${document.id} retains source bytes`).to.eq(true)
      expect(document.size).to.be.greaterThan(0)
      expect(document.sha256).to.eq(document.storedSha256)
      expect(receipt.documents[order]).to.deep.eq({
        id: document.id,
        type: document.type,
        order,
        size: document.size,
        sha256: document.sha256,
        contentType: document.contentType
      })
    })
  })
}
