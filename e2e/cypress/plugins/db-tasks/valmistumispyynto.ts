import type { Client } from 'pg'

import { dbClient, withDb } from './db-client'

const YEK_ERIKOISALA_ID = 61
const TEST_LICENSE_DOCUMENT = Buffer.from('%PDF-1.4\n%%EOF\n')

interface YekStudyRight {
  erikoistuva_laakari_id: number
  laillistamistodistus_id: number | null
  opintooikeus_id: number
}

export const valmistumispyyntoTasks = {
  async 'db:seedReturnedYekValmistumispyynto'({
    email,
    correctionProposal,
  }: {
    email: string
    correctionProposal: string
  }): Promise<number> {
    return withDb(dbClient, async (client: Client) => {
      const studyRightResult = await client.query<YekStudyRight>(
        `SELECT el.id AS erikoistuva_laakari_id,
                el.laillistamistodistus_id,
                o.id AS opintooikeus_id
         FROM opintooikeus o
         JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
         JOIN kayttaja k ON k.id = el.kayttaja_id
         JOIN jhi_user u ON u.id = k.user_id
         WHERE u.email = $1
           AND o.kaytossa = true
           AND o.erikoisala_id = $2
         LIMIT 1`,
        [email, YEK_ERIKOISALA_ID]
      )
      const studyRight = studyRightResult.rows[0]
      if (!studyRight) {
        throw new Error(`Could not find an active YEK study right for ${email}`)
      }

      let licenseDocumentId = studyRight.laillistamistodistus_id
      if (licenseDocumentId) {
        await client.query(`UPDATE asiakirja_data SET data = $1 WHERE id = $2`, [
          TEST_LICENSE_DOCUMENT,
          licenseDocumentId,
        ])
      } else {
        const documentResult = await client.query<{ id: number }>(
          `INSERT INTO asiakirja_data (data) VALUES ($1) RETURNING id`,
          [TEST_LICENSE_DOCUMENT]
        )
        licenseDocumentId = Number(documentResult.rows[0].id)
      }

      await client.query(
        `UPDATE erikoistuva_laakari
         SET laillistamispaiva = DATE '2020-01-01',
             laillistamistodistus_id = $1,
             laillistamispaivan_liitetiedoston_nimi = 'yek-laillistamistodistus.pdf',
             laillistamispaivan_liitetiedoston_tyyppi = 'application/pdf'
         WHERE id = $2`,
        [licenseDocumentId, studyRight.erikoistuva_laakari_id]
      )

      await client.query(
        `DELETE FROM valmistumispyynnon_tarkistus
         WHERE valmistumispyynto_id IN (
           SELECT id FROM valmistumispyynto WHERE opintooikeus_id = $1
         )`,
        [studyRight.opintooikeus_id]
      )
      await client.query(
        `DELETE FROM valmistumispyynto WHERE opintooikeus_id = $1`,
        [studyRight.opintooikeus_id]
      )
      const requestResult = await client.query<{ id: number }>(
        `INSERT INTO valmistumispyynto (
           opintooikeus_id,
           erikoistujan_kuittausaika,
           virkailijan_palautusaika,
           virkailijan_korjausehdotus,
           muokkauspaiva
         ) VALUES ($1, NULL, CURRENT_DATE, $2, CURRENT_DATE)
         RETURNING id`,
        [studyRight.opintooikeus_id, correctionProposal]
      )

      return Number(requestResult.rows[0].id)
    })
  },
}
