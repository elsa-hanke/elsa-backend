import type { Client } from 'pg'

import { dbClient, withDb } from './db-client'
import { fetchUserIdsByEmailOrLogin } from './user-utils'

const YEK_ERIKOISALA_ID = 61

interface YekStudyRight {
  opintooikeus_id: number
}

export const valmistumispyyntoTasks = {
  async 'db:grantYekGraduationRoles'({
    vastuuhenkiloEmail,
    virkailijaEmail,
    yliopistoId,
  }: {
    vastuuhenkiloEmail: string
    virkailijaEmail: string
    yliopistoId: number
  }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      const vastuuhenkilo = await fetchUserIdsByEmailOrLogin(client, vastuuhenkiloEmail)
      const virkailija = await fetchUserIdsByEmailOrLogin(client, virkailijaEmail)
      if (!vastuuhenkilo || !virkailija) {
        throw new Error('YEK graduation support users must be seeded before roles are granted')
      }

      await client.query(
        `DELETE FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
         WHERE kayttaja_yliopisto_erikoisala_id IN (
           SELECT id FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1
         )`,
        [vastuuhenkilo.kayttajaId]
      )
      await client.query(
        'DELETE FROM kayttaja_yliopisto_erikoisala WHERE kayttaja_id = $1',
        [vastuuhenkilo.kayttajaId]
      )
      const linkResult = await client.query<{ id: number }>(
        `INSERT INTO kayttaja_yliopisto_erikoisala
           (kayttaja_id, yliopisto_id, erikoisala_id)
         VALUES ($1, $2, $3)
         RETURNING id`,
        [vastuuhenkilo.kayttajaId, yliopistoId, YEK_ERIKOISALA_ID]
      )
      await client.query(
        `INSERT INTO rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi
           (kayttaja_yliopisto_erikoisala_id, vastuuhenkilon_tehtavatyyppi_id)
         SELECT $1, id
         FROM vastuuhenkilon_tehtavatyyppi
         WHERE nimi = 'YEK_VALMISTUMINEN'`,
        [linkResult.rows[0].id]
      )

      await client.query(
        'DELETE FROM rel_kayttaja__yliopisto WHERE kayttaja_id = $1',
        [virkailija.kayttajaId]
      )
      await client.query(
        `INSERT INTO rel_kayttaja__yliopisto (kayttaja_id, yliopisto_id)
         VALUES ($1, $2)`,
        [virkailija.kayttajaId, yliopistoId]
      )
      return null
    })
  },

  async 'db:seedReturnedYekValmistumispyynto'({
    email,
    correctionProposal,
  }: {
    email: string
    correctionProposal: string
  }): Promise<number> {
    return withDb(dbClient, async (client: Client) => {
      const studyRightResult = await client.query<YekStudyRight>(
        `SELECT o.id AS opintooikeus_id
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
