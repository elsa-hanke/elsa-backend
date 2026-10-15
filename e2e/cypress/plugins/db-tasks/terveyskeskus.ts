import type { Client } from 'pg'
import { dbClient, withDb } from './db-client'

const studyRights = `SELECT o.id, o.yliopisto_id, o.erikoisala_id, el.id AS el_id
  FROM opintooikeus o
  JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
  JOIN kayttaja k ON k.id = el.kayttaja_id JOIN jhi_user u ON u.id = k.user_id
  WHERE u.email = $1`

export const terveyskeskusTasks = {
  // Approval rows must be removed before the shared fixture deletes study rights.
  async 'db:cleanupTerveyskeskusE2e'({ email }: { email: string }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      await client.query(
        `DELETE FROM terveyskeskuskoulutusjakson_hyvaksynta
        WHERE opintooikeus_id IN (SELECT id FROM (${studyRights}) rights)`,
        [email]
      )
      const licence = await client.query<{ id: number }>(
        `SELECT DISTINCT el.laillistamistodistus_id AS id FROM erikoistuva_laakari el
         WHERE el.id IN (SELECT el_id FROM (${studyRights}) rights)
           AND el.laillistamispaivan_liitetiedoston_nimi = 'e2e-tk-laillistaminen.pdf'`,
        [email]
      )
      await client.query(
        `UPDATE erikoistuva_laakari
        SET laillistamistodistus_id = NULL, laillistamispaiva = NULL,
            laillistamispaivan_liitetiedoston_nimi = NULL, laillistamispaivan_liitetiedoston_tyyppi = NULL
        WHERE id IN (SELECT el_id FROM (${studyRights}) rights)
          AND laillistamispaivan_liitetiedoston_nimi = 'e2e-tk-laillistaminen.pdf'
        `,
        [email]
      )
      if (licence.rows.length) {
        await client.query('DELETE FROM asiakirja_data WHERE id = ANY($1::bigint[])', [
          licence.rows.map((row) => row.id)
        ])
      }
      // The ordinary fixture cleanup removes work periods and their attachments.
      return null
    })
  },

  async 'db:prepareTerveyskeskusE2e'({
    email,
    yek,
    vastuuhenkiloEmail,
    virkailijaEmail,
    pdfBase64
  }: {
    email: string
    yek: boolean
    vastuuhenkiloEmail: string
    virkailijaEmail: string
    pdfBase64: string
  }): Promise<{ opintooikeusId: number; tyoskentelyjaksoId: number }> {
    return withDb(dbClient, async (client: Client) => {
      await client.query('BEGIN')
      try {
        const rights = await client.query<{
          id: number
          yliopisto_id: number
          erikoisala_id: number
          el_id: number
        }>(`${studyRights} AND o.kaytossa = true AND (o.erikoisala_id = 61) = $2`, [email, yek])
        if (rights.rowCount !== 1)
          throw new Error('Expected exactly one active EL/YEK study right for the TK fixture')
        const right = rights.rows[0]
        const users = await client.query<{ email: string; id: number }>(
          `SELECT u.email, k.id FROM kayttaja k JOIN jhi_user u ON u.id = k.user_id WHERE u.email IN ($1, $2)`,
          [vastuuhenkiloEmail, virkailijaEmail]
        )
        const approver = users.rows.find((user) => user.email === vastuuhenkiloEmail)
        const clerk = users.rows.find((user) => user.email === virkailijaEmail)
        if (!approver || !clerk) throw new Error('TK support users must be seeded first')
        const links = await client.query<{ id: number }>(
          `SELECT id FROM kayttaja_yliopisto_erikoisala
          WHERE kayttaja_id = $1 AND yliopisto_id = $2 AND erikoisala_id = $3`,
          [approver.id, right.yliopisto_id, right.erikoisala_id]
        )
        if (links.rowCount !== 1)
          throw new Error('Expected the seeded approver university/specialty link')
        const duty = yek
          ? 'YEK_TERVEYSKESKUSKOULUTUSJAKSO'
          : 'TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN'
        const duties = await client.query<{ id: number }>(
          'SELECT id FROM vastuuhenkilon_tehtavatyyppi WHERE nimi = $1',
          [duty]
        )
        if (duties.rowCount !== 1) throw new Error(`Missing reference duty ${duty}`)
        await client.query(
          `INSERT INTO rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
          (kayttaja_yliopisto_erikoisala_id, vastuuhenkilon_tehtavatyyppi_id)
          SELECT $1, $2 WHERE NOT EXISTS (SELECT 1 FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
            WHERE kayttaja_yliopisto_erikoisala_id = $1 AND vastuuhenkilon_tehtavatyyppi_id = $2)`,
          [links.rows[0].id, duties.rows[0].id]
        )
        await client.query(
          `INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id)
          SELECT $1, $2 WHERE NOT EXISTS (SELECT 1 FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1 AND yliopisto_id = $2)`,
          [clerk.id, right.yliopisto_id]
        )
        const pdf = Buffer.from(pdfBase64, 'base64')
        if (!pdf.subarray(0, 5).equals(Buffer.from('%PDF-')))
          throw new Error('TK fixture must use a real PDF')
        const place = await client.query<{
          id: number
        }>(`INSERT INTO tyoskentelypaikka (nimi, tyyppi, kunta_id)
          VALUES ('E2E terveyskeskus', 'TERVEYSKESKUS', (SELECT id FROM kunta ORDER BY id LIMIT 1)) RETURNING id`)
        const period = await client.query<{ id: number }>(
          `INSERT INTO tyoskentelyjakso
          (alkamispaiva, paattymispaiva, osaaikaprosentti, kaytannon_koulutus, hyvaksytty_aiempaan_erikoisalaan,
           liitetty_koejaksoon, liitetty_terveyskeskuskoulutusjaksoon, tyoskentelypaikka_id, opintooikeus_id)
          VALUES ('2025-01-01', '2025-12-31', 100, $1, false, false, false, $2, $3) RETURNING id`,
          [yek ? 'OMAN_ERIKOISALAN_KOULUTUS' : 'TERVEYSKESKUSTYO', place.rows[0].id, right.id]
        )
        for (const name of ['tyotodistus', 'koulutustodistus', 'arviointi']) {
          const data = await client.query<{ id: number }>(
            'INSERT INTO asiakirja_data (data) VALUES ($1) RETURNING id',
            [pdf]
          )
          await client.query(
            `INSERT INTO asiakirja (opintooikeus_id, tyoskentelyjakso_id, nimi, tyyppi, lisattypvm, asiakirja_data_id)
            VALUES ($1, $2, $3, 'application/pdf', NOW(), $4)`,
            [right.id, period.rows[0].id, `e2e-tk-${name}.pdf`, data.rows[0].id]
          )
        }
        await client.query('COMMIT')
        return { opintooikeusId: Number(right.id), tyoskentelyjaksoId: Number(period.rows[0].id) }
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  }
}
