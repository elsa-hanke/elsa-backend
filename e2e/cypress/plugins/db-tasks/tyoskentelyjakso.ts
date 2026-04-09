import { dbClient } from './db-client'

/**
 * Tasks for seeding a työskentelyjakso (work period) for e2e test users.
 *
 * db:seedTyoskentelyjakso – Creates a työskentelyjakso for the user's active
 *                           opinto-oikeus so the arviointipyyntö form has
 *                           selectable options.  Idempotent.
 */
export const tyoskentelyjaksotTasks = {
  async 'db:seedTyoskentelyjakso'({ email }: { email: string }): Promise<{ tyoskentelyjaksoId: number } | null> {
    const client = dbClient()
    await client.connect()
    try {
      // Haetaan käyttäjän aktiivinen opinto-oikeus
      const opintooikeusResult = await client.query(
        `SELECT o.id AS opintooikeus_id
         FROM opintooikeus o
         JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
         JOIN kayttaja k ON k.id = el.kayttaja_id
         JOIN jhi_user u ON u.id = k.user_id
         WHERE u.email = $1 AND o.kaytossa = true
         LIMIT 1`,
        [email]
      )
      if (opintooikeusResult.rows.length === 0) return null

      const { opintooikeus_id } = opintooikeusResult.rows[0]

      // Idempotentti: ohitetaan, jos työskentelyjakso on jo olemassa
      const existing = await client.query(
        `SELECT id FROM tyoskentelyjakso WHERE opintooikeus_id = $1 LIMIT 1`,
        [opintooikeus_id]
      )
      if (existing.rows.length > 0) {
        return { tyoskentelyjaksoId: existing.rows[0].id }
      }

      // Haetaan jokin olemassa oleva kunta
      const kuntaResult = await client.query(`SELECT id FROM kunta LIMIT 1`)
      const kuntaId = kuntaResult.rows[0].id

      // Luodaan työskentelypaikka
      const tyoskentelypaikkaId: number = (
        await client.query(
          `INSERT INTO tyoskentelypaikka (nimi, tyyppi, kunta_id)
           VALUES ('E2E Testisairaala', 'YLIOPISTOLLINEN_SAIRAALA', $1)
           RETURNING id`,
          [kuntaId]
        )
      ).rows[0].id

      // Luodaan työskentelyjakso kattamaan laaja aikaväli testipäivämääriä varten
      const tyoskentelyjaksoId: number = (
        await client.query(
          `INSERT INTO tyoskentelyjakso
             (alkamispaiva, paattymispaiva, osaaikaprosentti, kaytannon_koulutus,
              hyvaksytty_aiempaan_erikoisalaan, liitetty_koejaksoon,
              liitetty_terveyskeskuskoulutusjaksoon,
              tyoskentelypaikka_id, opintooikeus_id)
           VALUES ('2020-01-01', NULL, 100, 'OMAN_ERIKOISALAN_KOULUTUS',
                   false, false, false, $1, $2)
           RETURNING id`,
          [tyoskentelypaikkaId, opintooikeus_id]
        )
      ).rows[0].id

      return { tyoskentelyjaksoId }
    } finally {
      await client.end()
    }
  },
}

