import { dbClient } from './db-client'

/**
 * Tasks for cleaning up an erikoistuva lääkäri (resident physician) test user
 * and all of their associated data.
 *
 * db:cleanupErikoistuva – Removes the user and every related row in FK-dependency
 *                         order.  Safe to call when the user does not exist.
 *                         Call before first-login tests so the "Aloita palvelun
 *                         käyttö" screen is always shown on the next login.
 */
export const erikoistuvaLaakariTasks = {
  async 'db:cleanupErikoistuva'({ email }: { email: string }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const result = await client.query(
        `SELECT u.id AS user_id, k.id AS kayttaja_id, el.id AS el_id
         FROM jhi_user u
         LEFT JOIN kayttaja k ON k.user_id = u.id
         LEFT JOIN erikoistuva_laakari el ON el.kayttaja_id = k.id
         WHERE u.email = $1`,
        [email]
      )
      if (result.rows.length === 0) return null

      const { user_id, kayttaja_id, el_id } = result.rows[0]

      if (el_id) {
        // Kerätään työskentelypaikka-id:t ennen työskentelyjaksojen poistoa
        const paikkaResult = await client.query(
          `SELECT tyoskentelypaikka_id FROM tyoskentelyjakso
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        const paikkaIds: number[] = paikkaResult.rows.map(
          (r: { tyoskentelypaikka_id: number }) => r.tyoskentelypaikka_id
        )

        // Poistetaan koejakson rivit ennen opintooikeuksia (FK: koejakso_* -> opintooikeus)
        // sekä niiden apu-/liitostaulut ennen päätauluja.
        await client.query(
          `DELETE FROM asiakirja
           WHERE koejakson_vastuuhenkilon_arvio_id IN (
             SELECT id FROM koejakson_vastuuhenkilon_arvio
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_vastuuhenkilon_arvio
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_loppukeskustelu
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_valiarviointi_kehittamistoimenpidekategoriat
           WHERE valiarviointi_id IN (
             SELECT id FROM koejakson_valiarviointi
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_valiarviointi
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_kehittamistoimenpiteet
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_aloituskeskustelu
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koulutussopimuksen_kouluttaja
           WHERE koulutussopimus_id IN (
             SELECT id FROM koejakson_koulutussopimus
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koulutussopimuksen_koulutuspaikka
           WHERE koulutussopimus_id IN (
             SELECT id FROM koejakson_koulutussopimus
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koejakson_koulutussopimus
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )

        // Poistetaan valmistumispyynnöt ennen opintooikeuksia (FK: valmistumispyynto -> opintooikeus)
        await client.query(
          `DELETE FROM valmistumispyynnon_tarkistus
           WHERE valmistumispyynto_id IN (
             SELECT id FROM valmistumispyynto
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM valmistumispyynto
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        // Poistetaan kaikki opintooikeuteen liittyvät asiakirjat ennen opintooikeus-rivejä.
        await client.query(
          `DELETE FROM asiakirja
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )

        // Poistetaan koulutusjakso-rivit ennen koulutussuunnitelma-rivejä
        // (FK: koulutusjakso.koulutussuunnitelma_id → koulutussuunnitelma.id)
        await client.query(
          `DELETE FROM koulutusjakso
           WHERE koulutussuunnitelma_id IN (
             SELECT id FROM koulutussuunnitelma
             WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM koulutussuunnitelma
           WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        // Poistetaan suoritusarvioinnin arvioitavat kokonaisuudet ennen suoritusarviointeja (FK)
        await client.query(
          `DELETE FROM suoritusarvioinnin_arvioitava_kokonaisuus WHERE suoritusarviointi_id IN (
             SELECT id FROM suoritusarviointi WHERE tyoskentelyjakso_id IN (
               SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
                 SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
               )
             )
           )`,
          [el_id]
        )
        // Poistetaan suoritusarvioinnin kommentit ennen suoritusarviointeja (FK)
        await client.query(
          `DELETE FROM suoritusarvioinnin_kommentti WHERE suoritusarviointi_id IN (
             SELECT id FROM suoritusarviointi WHERE tyoskentelyjakso_id IN (
               SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
                 SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
               )
             )
           )`,
          [el_id]
        )
        // Poistetaan suoritusarvioinnit ennen tyoskentelyjakso-rivejä (FK)
        await client.query(
          `DELETE FROM suoritusarviointi WHERE tyoskentelyjakso_id IN (
             SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        // Poistetaan keskeytysaika-rivit ennen tyoskentelyjakso-rivejä (FK)
        await client.query(
          `DELETE FROM keskeytysaika WHERE tyoskentelyjakso_id IN (
             SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        // Poistetaan asiakirja-rivit ennen tyoskentelyjakso-rivejä (FK)
        await client.query(
          `DELETE FROM asiakirja WHERE tyoskentelyjakso_id IN (
             SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id IN (
               SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
             )
           )`,
          [el_id]
        )
        // Poistetaan työskentelyjaksot ennen opintooikeus-rivejä (FK)
        await client.query(
          `DELETE FROM tyoskentelyjakso WHERE opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        // Poistetaan työskentelypaikat, jotka olivat ko. työskentelyjaksoilla
        if (paikkaIds.length > 0) {
          await client.query(
            `DELETE FROM tyoskentelypaikka WHERE id = ANY($1::bigint[])`,
            [paikkaIds]
          )
        }
        await client.query(
          `DELETE FROM opintooikeus_herate WHERE erikoistuva_laakari_id = $1`,
          [el_id]
        )
        // Poistetaan kouluttajavaltuutukset ennen opintooikeus-rivejä (FK: valtuuttaja_opintooikeus_id)
        await client.query(
          `DELETE FROM kouluttajavaltuutus WHERE valtuuttaja_opintooikeus_id IN (
             SELECT id FROM opintooikeus WHERE erikoistuva_laakari_id = $1
           )`,
          [el_id]
        )
        await client.query(
          `DELETE FROM opintooikeus WHERE erikoistuva_laakari_id = $1`,
          [el_id]
        )
        await client.query(
          `DELETE FROM erikoistuva_laakari WHERE id = $1`,
          [el_id]
        )
      }

      if (kayttaja_id) {
        await client.query(`DELETE FROM kayttaja WHERE id = $1`, [kayttaja_id])
      }

      await client.query(
        `DELETE FROM jhi_user_authority WHERE user_id = $1`,
        [user_id]
      )
      await client.query(`DELETE FROM jhi_user WHERE id = $1`, [user_id])

      return null
    } finally {
      await client.end()
    }
  },
}
