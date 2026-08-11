import { Client } from 'pg'
import { dbClient, withDb } from './db-client'
import {addRoletoUser, getErikoistujaLaakariId} from './db-helpers'

export type OpintoOikeus = {
  id: number,
  myontamispaiva: string,
  paattymispaiva: string,
  opiskelijatunnus?: string,
  osaamisen_arvioinnin_oppaan_pvm: string,
  erikoistuva_laakari_id: number,
  yliopisto_id: number,
  erikoisala_id: number,
  opintoopas_id: number,
  asetus_id: number,
  kaytossa: boolean,
  yliopisto_opintooikeus_id: string,
  tila: string,
  muokkausaika: string,
  terveyskeskuskoulutusjakso_suoritettu: boolean,
  muokkausoikeudet_virkailijoilla: boolean,
  viimeinen_katselupaiva: string
}


export const opintoOikeusTasks = {
  async 'db:seedOpintooikeus'({
    email,
    opintoOikeus,
    updateCurrent = false,
    generateId = false,
  }: {
    email: string
    opintoOikeus: OpintoOikeus
    updateCurrent?: boolean
    generateId?: boolean
  }): Promise<number> {
    return withDb(dbClient, async (client: Client) => {
      if (!opintoOikeus) {
        throw new Error('db:seedOpintooikeus requires { email, opintoOikeus }')
      }


      const erikoistuva = await getErikoistujaLaakariId(client, email)

      if (!erikoistuva) {
        throw new Error(`Could not find erikoistuva with email ${email}`)
      }

      if( opintoOikeus.erikoisala_id === 61 ) {
        await addRoletoUser(client, email, 'ROLE_YEK_KOULUTETTAVA')
      }

      console.log(opintoOikeus)

      opintoOikeus.erikoistuva_laakari_id = erikoistuva
      if (updateCurrent) {
        const currentOpintooikeus = await client.query(
          `SELECT id FROM public.opintooikeus WHERE erikoistuva_laakari_id = $1 AND kaytossa = true LIMIT 1`,
          [erikoistuva]
        )
        const currentId: number | undefined = currentOpintooikeus.rows[0]?.id
        if (!currentId) {
          throw new Error(`Could not find active opintooikeus for ${email}`)
        }

        await client.query(
          `UPDATE public.opintooikeus
           SET opintooikeuden_myontamispaiva = $2,
               opintooikeuden_paattymispaiva = $3,
               opiskelijatunnus = $4,
               osaamisen_arvioinnin_oppaan_pvm = $5,
               yliopisto_id = $6,
               erikoisala_id = $7,
               opintoopas_id = $8,
               asetus_id = $9,
               kaytossa = $10,
               yliopisto_opintooikeus_id = $11,
               tila = $12,
               muokkausaika = $13,
               terveyskeskuskoulutusjakso_suoritettu = $14,
               muokkausoikeudet_virkailijoilla = $15,
               viimeinen_katselupaiva = $16
           WHERE id = $1`,
          [
            currentId,
            opintoOikeus.myontamispaiva,
            opintoOikeus.paattymispaiva,
            opintoOikeus.opiskelijatunnus,
            opintoOikeus.osaamisen_arvioinnin_oppaan_pvm,
            opintoOikeus.yliopisto_id,
            opintoOikeus.erikoisala_id,
            opintoOikeus.opintoopas_id,
            opintoOikeus.asetus_id,
            opintoOikeus.kaytossa,
            opintoOikeus.yliopisto_opintooikeus_id,
            opintoOikeus.tila,
            opintoOikeus.muokkausaika,
            opintoOikeus.terveyskeskuskoulutusjakso_suoritettu,
            opintoOikeus.muokkausoikeudet_virkailijoilla,
            opintoOikeus.viimeinen_katselupaiva
          ]
        )
        await client.query(
          `UPDATE public.erikoistuva_laakari SET aktiivinen_opintooikeus = $1 WHERE id = $2`,
          [currentId, erikoistuva]
        )
        return currentId
      }

      if ( opintoOikeus.kaytossa ) {
        await client.query(`UPDATE public.opintooikeus SET kaytossa = false WHERE erikoistuva_laakari_id = $1`, [erikoistuva])
      }

      if (generateId) {
        const idResult = await client.query<{ id: string }>(
          `SELECT nextval(
             pg_get_serial_sequence('public.opintooikeus', 'id')
           ) AS id`
        )
        opintoOikeus.id = Number(idResult.rows[0].id)
      }

      const sql = "INSERT INTO public.opintooikeus (id, opintooikeuden_myontamispaiva, opintooikeuden_paattymispaiva, opiskelijatunnus, " +
        "osaamisen_arvioinnin_oppaan_pvm, erikoistuva_laakari_id, yliopisto_id, erikoisala_id, opintoopas_id, asetus_id, kaytossa, yliopisto_opintooikeus_id, tila, muokkausaika, terveyskeskuskoulutusjakso_suoritettu, muokkausoikeudet_virkailijoilla, viimeinen_katselupaiva) " +
        "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17) returning id";

      const res = await client.query<{ id: number }>(sql, [
        opintoOikeus.id,
        opintoOikeus.myontamispaiva,
        opintoOikeus.paattymispaiva,
        opintoOikeus.opiskelijatunnus,
        opintoOikeus.osaamisen_arvioinnin_oppaan_pvm,
        opintoOikeus.erikoistuva_laakari_id,
        opintoOikeus.yliopisto_id,
        opintoOikeus.erikoisala_id,
        opintoOikeus.opintoopas_id,
        opintoOikeus.asetus_id,
        opintoOikeus.kaytossa,
        opintoOikeus.yliopisto_opintooikeus_id,
        opintoOikeus.tila,
        opintoOikeus.muokkausaika,
        opintoOikeus.terveyskeskuskoulutusjakso_suoritettu,
        opintoOikeus.muokkausoikeudet_virkailijoilla,
        opintoOikeus.viimeinen_katselupaiva
      ])

      const insertedId = Number(res.rows[0].id)
      if (opintoOikeus.kaytossa) {
        await client.query(
          `UPDATE public.erikoistuva_laakari
           SET aktiivinen_opintooikeus = $1
           WHERE id = $2`,
          [insertedId, erikoistuva]
        )
      }

      console.log(insertedId)
      return insertedId

    })
  },

  async 'db:cleanupOpintooikeus'({
    email,
    id,
  }: {
    email?: string
    id?: number
  }): Promise<null> {
    return withDb(dbClient, async (client: Client) => {
      if (typeof id === 'number') {
        await client.query(`DELETE FROM opintooikeus WHERE id = $1`, [id])
        return null
      }

      if (email) {
        const erikoistuvaId = await getErikoistujaLaakariId(client, email)
        if (!erikoistuvaId) return null

        await client.query(`DELETE FROM opintooikeus WHERE erikoistuva_laakari_id = $1`, [erikoistuvaId])
        return null
      }

      throw new Error('db:cleanupOpintooikeus requires either { id } or { email }')
    })
  },

}
