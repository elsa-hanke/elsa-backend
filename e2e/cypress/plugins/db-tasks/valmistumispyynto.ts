import type { Client } from 'pg'

import { dbClient, withDb } from './db-client'
import { fetchUserIdsByEmailOrLogin } from './user-utils'

const YEK_ERIKOISALA_ID = 61

interface YekStudyRight {
  opintooikeus_id: number
}

export const valmistumispyyntoTasks = {
  async 'db:seedOwnerlessGraduationAssessment'({ valmistumispyyntoId, reviewerEmail }: {
    valmistumispyyntoId: number
    reviewerEmail: string
  }): Promise<{ assessmentId: number; toolId: number; blockId: number; categoryId: number }> {
    return withDb(dbClient, async (client: Client) => {
      const reviewer = await fetchUserIdsByEmailOrLogin(client, reviewerEmail)
      if (!reviewer) throw new Error('Graduation assessment reviewer is missing')
      // One statement keeps fixture creation atomic. The tool is deliberately only
      // referenced by its answer, exercising the lazy-load path from the incident.
      const result = await client.query<{ assessment_id: number; tool_id: number; block_id: number; category_id: number }>(
        `WITH context AS (
           SELECT tj.id AS period_id, op.arviointiasteikko_id AS scale_id, o.erikoisala_id
           FROM valmistumispyynto vp
           JOIN opintooikeus o ON o.id = vp.opintooikeus_id
           JOIN opintoopas op ON op.id = o.opintoopas_id
           JOIN tyoskentelyjakso tj ON tj.opintooikeus_id = o.id
           WHERE vp.id = $1 ORDER BY tj.id LIMIT 1
         ), category AS (
           INSERT INTO arvioitavan_kokonaisuuden_kategoria (nimi, jarjestysnumero, erikoisala_id)
           SELECT 'E2E historical assessment category', 1, erikoisala_id FROM context RETURNING id
         ), assessment_block AS (
           INSERT INTO arvioitava_kokonaisuus (nimi, voimassaolo_alkaa, kategoria_id)
           SELECT 'E2E historical assessment block', CURRENT_DATE, id FROM category RETURNING id
         ), tool AS (
           INSERT INTO arviointityokalu
             (nimi, versio, kaytossa, tila, luontiaika, muokkausaika, kayttaja_id)
           SELECT 'E2E historical ownerless tool', 21, false, 'JULKAISTU', NOW(), NOW(), NULL
           FROM context RETURNING id
         ), question AS (
           INSERT INTO arviointityokalu_kysymys
             (otsikko, pakollinen, tyyppi, arviointityokalu_id, jarjestysnumero)
           SELECT 'E2E historical question', true, 'TEKSTIKENTTAKYSYMYS', id, 1
           FROM tool RETURNING id
         ), assessment AS (
           INSERT INTO suoritusarviointi
             (tapahtuman_ajankohta, arvioitava_tapahtuma, pyynnon_aika, arviointi_aika,
              lukittu, keskenerainen, arvioinnin_antaja_id, tyoskentelyjakso_id, arviointiasteikko_id)
           SELECT CURRENT_DATE, 'E2E historical ownerless assessment', CURRENT_DATE,
                  CURRENT_DATE, true, false, $2, period_id, scale_id
           FROM context RETURNING id
         ), block AS (
           INSERT INTO suoritusarvioinnin_arvioitava_kokonaisuus
             (suoritusarviointi_id, arvioitava_kokonaisuus_id)
           SELECT assessment.id, assessment_block.id FROM assessment CROSS JOIN assessment_block
         ), answer AS (
           INSERT INTO suoritusarvioinnin_arviointityokalun_vastaus
             (suoritusarviointi_id, arviointityokalu_id, arviointityokalu_kysymys_id, teksti_vastaus)
           SELECT assessment.id, tool.id, question.id, 'E2E historical answer'
           FROM assessment CROSS JOIN tool CROSS JOIN question
         )
         SELECT assessment.id AS assessment_id, tool.id AS tool_id,
                assessment_block.id AS block_id, category.id AS category_id
         FROM assessment CROSS JOIN tool CROSS JOIN assessment_block CROSS JOIN category`,
        [valmistumispyyntoId, reviewer.kayttajaId]
      )
      if (!result.rows[0]) throw new Error('Graduation fixture requires a working period and a study guide for the request')
      return {
        assessmentId: Number(result.rows[0].assessment_id),
        toolId: Number(result.rows[0].tool_id),
        blockId: Number(result.rows[0].block_id),
        categoryId: Number(result.rows[0].category_id),
      }
    })
  },

  async 'db:readGraduationAssessmentPdf'({ valmistumispyyntoId }: {
    valmistumispyyntoId: number
  }): Promise<string> {
    return withDb(dbClient, async (client: Client) => {
      // This internal archive document is not downloadable through the approver's API.
      const result = await client.query<{ data: Buffer }>(
        `SELECT d.data
         FROM valmistumispyynto vp
         JOIN asiakirja a ON a.id = vp.erikoistujan_tiedot_asiakirja_id
         JOIN asiakirja_data d ON d.id = a.asiakirja_data_id
         WHERE vp.id = $1 AND a.tyyppi = 'application/pdf'`,
        [valmistumispyyntoId]
      )
      if (!result.rows[0]?.data?.length) {
        throw new Error(`Graduation assessment PDF is missing for request ${valmistumispyyntoId}`)
      }
      return result.rows[0].data.toString('base64')
    })
  },

  async 'db:cleanupOwnerlessGraduationAssessment'({ assessmentId, toolId, blockId, categoryId }: {
    assessmentId: number
    toolId: number
    blockId: number
    categoryId: number
  }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      await client.query('BEGIN')
      try {
        await client.query('DELETE FROM suoritusarvioinnin_arviointityokalun_vastaus WHERE suoritusarviointi_id = $1', [assessmentId])
        await client.query('DELETE FROM suoritusarvioinnin_arvioitava_kokonaisuus WHERE suoritusarviointi_id = $1', [assessmentId])
        await client.query('DELETE FROM suoritusarviointi WHERE id = $1', [assessmentId])
        await client.query('DELETE FROM arviointityokalu_kysymys WHERE arviointityokalu_id = $1', [toolId])
        await client.query('DELETE FROM arviointityokalu WHERE id = $1', [toolId])
        await client.query('DELETE FROM arvioitava_kokonaisuus WHERE id = $1', [blockId])
        await client.query('DELETE FROM arvioitavan_kokonaisuuden_kategoria WHERE id = $1', [categoryId])
        await client.query('COMMIT')
        return null
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      }
    })
  },

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
