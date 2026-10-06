import { dbClient, withDb } from './db-client'
import { getOpintooikeusId } from './db-helpers'

/** Test-only legacy values bypass save validation so PDF fallback validation is exercised. */
export const pdfTextTasks = {
  async 'db:setLegacyPdfText'({
    email,
    id,
    target,
    text
  }: {
    email: string
    id: number
    target: 'contract-place' | 'review-summary'
    text: string
  }): Promise<null> {
    return withDb(dbClient, async (client) => {
      const oid = await getOpintooikeusId(client, email)
      if (!oid) throw new Error('PDF text test resident has no active study right')
      let query: string
      switch (target) {
        case 'contract-place':
          query = `UPDATE koulutussopimuksen_koulutuspaikka SET nimi = $1
            WHERE koulutussopimus_id = $2 AND koulutussopimus_id IN
              (SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $3)`
          break
        case 'review-summary':
          query = `UPDATE koejakson_vastuuhenkilon_arvio SET virkailijan_yhteenveto = $1
            WHERE id = $2 AND opintooikeus_id = $3`
          break
        default:
          throw new Error('Unknown legacy PDF text target')
      }
      const result = await client.query(query, [text, id, oid])
      if (result.rowCount !== 1)
        throw new Error(`Expected one legacy text row, got ${result.rowCount}`)
      return null
    })
  },

  async 'db:pdfTextState'({
    email,
    kayttajaIds
  }: {
    email: string
    kayttajaIds: number[]
  }): Promise<Record<string, unknown>> {
    return withDb(dbClient, async (client) => {
      const oid = await getOpintooikeusId(client, email)
      if (!oid) throw new Error('PDF text test resident has no active study right')
      // Full form rows catch persisted text, signatures, corrections and dates after HTTP 400.
      // Table names are a fixed whitelist; test input is always passed as query parameters.
      const tables = [
        'koejakson_koulutussopimus',
        'koejakson_aloituskeskustelu',
        'koejakson_valiarviointi',
        'koejakson_loppukeskustelu',
        'koejakson_vastuuhenkilon_arvio'
      ]
      const state: Record<string, unknown> = {}
      for (const table of tables) {
        state[table] = (
          await client.query(`SELECT * FROM ${table} WHERE opintooikeus_id = $1 ORDER BY id`, [oid])
        ).rows
      }
      state.kouluttajat = (
        await client.query(
          `SELECT * FROM koulutussopimuksen_kouluttaja WHERE koulutussopimus_id IN
          (SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1) ORDER BY id`,
          [oid]
        )
      ).rows
      state.koulutuspaikat = (
        await client.query(
          `SELECT * FROM koulutussopimuksen_koulutuspaikka WHERE koulutussopimus_id IN
          (SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1) ORDER BY id`,
          [oid]
        )
      ).rows
      state.contacts = (
        await client.query(
          `SELECT k.id, k.nimike, u.email, u.phone_number FROM kayttaja k
          JOIN jhi_user u ON u.id = k.user_id WHERE k.id = ANY($1::bigint[]) OR k.id IN
          (SELECT el.kayttaja_id FROM erikoistuva_laakari el
            JOIN opintooikeus o ON o.erikoistuva_laakari_id = el.id WHERE o.id = $2)
          ORDER BY k.id`,
          [kayttajaIds, oid]
        )
      ).rows
      state.asiakirjat = (
        await client.query(
          `SELECT id, nimi, tyyppi FROM asiakirja WHERE opintooikeus_id = $1 ORDER BY id`,
          [oid]
        )
      ).rows
      return JSON.parse(JSON.stringify(state))
    })
  }
}
