import { dbClient } from './db-client'

/**
 * Tehtävät koejakson tietokantatilan siementämiseen e2e-testejä varten.
 *
 * db:seedKouluttajavaltuutus              – Luo kouluttajavaltuutuksen erikoistuvalle (jos puuttuu)
 * db:cleanupKoejakso                      – Poistaa kaikki koejakson lomakkeet opintooikeudelta
 * db:ensureKoulutussopimusLahetetty       – Varmistaa, että koulutussopimus on lähetetty
 * db:ensureKoulutussopimusHyvaksytty      – Varmistaa, että kouluttaja on hyväksynyt koulutussopimuksen
 * db:ensureAloituskeskusteluHyvaksytty    – Varmistaa, että aloituskeskustelu on hyväksytty
 * db:ensureValiarviointiHyvaksytty        – Varmistaa, että väliarviointi on hyväksytty
 * db:ensureLoppukeskusteluHyvaksytty      – Varmistaa, että loppukeskustelu on hyväksytty
 *
 * Taulunimet (JPA @Table-annotaatioista):
 *   koejakson_koulutussopimus
 *   koulutussopimuksen_kouluttaja         (FK: koulutussopimus_id, kouluttaja_id, sopimus_hyvaksytty)
 *   koejakson_aloituskeskustelu
 *   koejakson_valiarviointi
 *   koejakson_kehittamistoimenpiteet
 *   koejakson_loppukeskustelu
 *   koejakson_vastuuhenkilon_arvio
 */
export const koejaksoTasks = {
  /**
   * Luo kouluttajavaltuutuksen erikoistujalle tietylle kouluttajalle.
   */
  async 'db:seedKouluttajavaltuutus'({
    erikoistuvaEmail,
    kouluttajaEmail,
  }: {
    erikoistuvaEmail: string
    kouluttajaEmail: string
  }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const opintooikeusResult = await client.query(
        `SELECT o.id
         FROM opintooikeus o
         JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
         JOIN kayttaja k ON k.id = el.kayttaja_id
         JOIN jhi_user u ON u.id = k.user_id
         WHERE u.email = $1 AND o.kaytossa = true
         LIMIT 1`,
        [erikoistuvaEmail]
      )
      if (opintooikeusResult.rows.length === 0) return null
      const opintooikeusId: number = opintooikeusResult.rows[0].id

      const kouluttajaResult = await client.query(
        `SELECT k.id FROM kayttaja k JOIN jhi_user u ON u.id = k.user_id WHERE u.email = $1`,
        [kouluttajaEmail]
      )
      if (kouluttajaResult.rows.length === 0) return null
      const kouluttajaId: number = kouluttajaResult.rows[0].id

      await client.query(
        `INSERT INTO kouluttajavaltuutus (alkamispaiva, paattymispaiva, valtuutuksen_luontiaika, valtuutuksen_muokkausaika, valtuuttaja_opintooikeus_id, valtuutettu_id)
         SELECT CURRENT_DATE, CURRENT_DATE + INTERVAL '2 years', NOW(), NOW(), $1, $2
         WHERE NOT EXISTS (
           SELECT 1 FROM kouluttajavaltuutus
           WHERE valtuuttaja_opintooikeus_id = $1 AND valtuutettu_id = $2
         )`,
        [opintooikeusId, kouluttajaId]
      )
      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Poistaa kaikki koejakson lomakerivit erikoistuvalta.
   * Turvallinen kutsua useaan kertaan – ei heittele virheitä, jos rivejä ei ole.
   */
  async 'db:cleanupKoejakso'({ erikoistuvaEmail }: { erikoistuvaEmail: string }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const opintooikeusResult = await client.query(
        `SELECT o.id
         FROM opintooikeus o
         JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
         JOIN kayttaja k ON k.id = el.kayttaja_id
         JOIN jhi_user u ON u.id = k.user_id
         WHERE u.email = $1 AND o.kaytossa = true LIMIT 1`,
        [erikoistuvaEmail]
      )
      if (opintooikeusResult.rows.length === 0) return null
      const oid: number = opintooikeusResult.rows[0].id

      await client.query(
        `DELETE FROM asiakirja
         WHERE koejakson_vastuuhenkilon_arvio_id IN (
           SELECT id FROM koejakson_vastuuhenkilon_arvio WHERE opintooikeus_id = $1
         )`,
        [oid]
      )
      await client.query(`DELETE FROM koejakson_vastuuhenkilon_arvio   WHERE opintooikeus_id = $1`, [oid])
      await client.query(`DELETE FROM koejakson_loppukeskustelu        WHERE opintooikeus_id = $1`, [oid])
      await client.query(
        `DELETE FROM koejakson_valiarviointi_kehittamistoimenpidekategoriat
         WHERE valiarviointi_id IN (
           SELECT id FROM koejakson_valiarviointi WHERE opintooikeus_id = $1
         )`,
        [oid]
      )
      await client.query(`DELETE FROM koejakson_valiarviointi          WHERE opintooikeus_id = $1`, [oid])
      await client.query(`DELETE FROM koejakson_kehittamistoimenpiteet WHERE opintooikeus_id = $1`, [oid])
      await client.query(`DELETE FROM koejakson_aloituskeskustelu      WHERE opintooikeus_id = $1`, [oid])
      // koulutussopimuksen liitostaulut on poistettava ennen koulutussopimusta
      await client.query(
        `DELETE FROM koulutussopimuksen_kouluttaja
         WHERE koulutussopimus_id IN (
           SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1
         )`,
        [oid]
      )
      await client.query(
        `DELETE FROM koulutussopimuksen_koulutuspaikka
         WHERE koulutussopimus_id IN (
           SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1
         )`,
        [oid]
      )
      await client.query(`DELETE FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1`, [oid])
      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Varmistaa, että koulutussopimus on lähetetty.
   * Idempotentti – ei ylikirjoita olemassa olevaa hyväksyttyä sopimusta.
   */
  async 'db:ensureKoulutussopimusLahetetty'({
    erikoistuvaEmail,
    kouluttajaEmail,
  }: {
    erikoistuvaEmail: string
    kouluttajaEmail: string
  }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      const kouluttajaId: number = await getKayttajaId(client, kouluttajaEmail)
      if (!kouluttajaId) return null

      // Idempotentti upsert – jos sopimus on jo olemassa, ei ylikirjoiteta
      await client.query(
        `INSERT INTO koejakson_koulutussopimus
           (opintooikeus_id, koejakson_alkamispaiva, lahetetty,
            muokkauspaiva, vastuuhenkilo_hyvaksynyt,
            erikoistuvan_allekirjoitusaika)
         SELECT $1, CURRENT_DATE, true,
                CURRENT_DATE, true,
                CURRENT_DATE
         WHERE NOT EXISTS (
           SELECT 1 FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1
         )`,
        [oid]
      )

      // Lisätään kouluttaja koulutussopimuksen kouluttajalistalle
      const sopimus = await client.query(
        `SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1`, [oid]
      )
      const sopimusId: number = sopimus.rows[0].id

      await client.query(
        `INSERT INTO koulutussopimuksen_kouluttaja (koulutussopimus_id, kouluttaja_id, sopimus_hyvaksytty)
         SELECT $1, $2, false
         WHERE NOT EXISTS (
           SELECT 1 FROM koulutussopimuksen_kouluttaja
           WHERE koulutussopimus_id = $1 AND kouluttaja_id = $2
         )`,
        [sopimusId, kouluttajaId]
      )

      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Varmistaa, että kouluttaja on hyväksynyt koulutussopimuksen.
   * Kutsuu ensin db:ensureKoulutussopimusLahetetty.
   */
  async 'db:ensureKoulutussopimusHyvaksytty'({
    erikoistuvaEmail,
    kouluttajaEmail,
  }: {
    erikoistuvaEmail: string
    kouluttajaEmail: string
  }): Promise<null> {
    await koejaksoTasks['db:ensureKoulutussopimusLahetetty']({ erikoistuvaEmail, kouluttajaEmail })

    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      const kouluttajaId: number = await getKayttajaId(client, kouluttajaEmail)
      if (!kouluttajaId) return null

      // Merkitään kouluttaja hyväksyneeksi
      await client.query(
        `UPDATE koulutussopimuksen_kouluttaja
         SET sopimus_hyvaksytty = true, kuittausaika = CURRENT_DATE
         WHERE koulutussopimus_id = (SELECT id FROM koejakson_koulutussopimus WHERE opintooikeus_id = $1)
           AND kouluttaja_id = $2`,
        [oid, kouluttajaId]
      )

      // Merkitään vastuuhenkilö hyväksyneeksi (tarvitaan jotta aloituskeskustelu/valiarviointi/loppukeskustelu aukeavat)
      await client.query(
        `UPDATE koejakson_koulutussopimus
         SET vastuuhenkilo_hyvaksynyt = true, vastuuhenkilon_kuittausaika = CURRENT_DATE
         WHERE opintooikeus_id = $1`,
        [oid]
      )

      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Varmistaa, että aloituskeskustelu on hyväksytty molemmilta osapuolilta.
   * Kutsuu ensin db:ensureKoulutussopimusHyvaksytty.
   */
  async 'db:ensureAloituskeskusteluHyvaksytty'({
    erikoistuvaEmail,
    kouluttajaEmail,
  }: {
    erikoistuvaEmail: string
    kouluttajaEmail: string
  }): Promise<null> {
    await koejaksoTasks['db:ensureKoulutussopimusHyvaksytty']({ erikoistuvaEmail, kouluttajaEmail })

    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      const kouluttajaId: number = await getKayttajaId(client, kouluttajaEmail)
      if (!kouluttajaId) return null

      await client.query(
        `INSERT INTO koejakson_aloituskeskustelu
           (opintooikeus_id, koejakson_suorituspaikka,
            koejakson_alkamispaiva, koejakson_paattymispaiva,
            lahikouluttaja_id, lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika,
            lahiesimies_id, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
            lahetetty, muokkauspaiva, erikoistuvan_kuittausaika)
         SELECT $1, 'E2E Testisairaala',
                CURRENT_DATE, CURRENT_DATE + INTERVAL '6 months',
                $2, true, CURRENT_DATE,
                $2, true, CURRENT_DATE,
                true, CURRENT_DATE, CURRENT_DATE
         WHERE NOT EXISTS (SELECT 1 FROM koejakson_aloituskeskustelu WHERE opintooikeus_id = $1)`,
        [oid, kouluttajaId]
      )
      await client.query(
        `UPDATE koejakson_aloituskeskustelu
         SET lahikouluttaja_id            = $2,
             lahikouluttaja_hyvaksynyt    = true,
             lahikouluttajan_kuittausaika = CURRENT_DATE,
             lahiesimies_id              = $2,
             lahiesimies_hyvaksynyt       = true,
             lahiesimiehen_kuittausaika   = CURRENT_DATE,
             lahetetty                   = true,
             erikoistuvan_kuittausaika   = CURRENT_DATE
         WHERE opintooikeus_id = $1`,
        [oid, kouluttajaId]
      )

      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Varmistaa, että väliarviointi on hyväksytty molemmilta osapuolilta.
   * Kutsuu ensin db:ensureAloituskeskusteluHyvaksytty.
   */
  async 'db:ensureValiarviointiHyvaksytty'({
    erikoistuvaEmail,
    kouluttajaEmail,
  }: {
    erikoistuvaEmail: string
    kouluttajaEmail: string
  }): Promise<null> {
    await koejaksoTasks['db:ensureAloituskeskusteluHyvaksytty']({ erikoistuvaEmail, kouluttajaEmail })

    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      const kouluttajaId: number = await getKayttajaId(client, kouluttajaEmail)
      if (!kouluttajaId) return null

      await client.query(
        `INSERT INTO koejakson_valiarviointi
           (opintooikeus_id, edistyminen_tavoitteiden_mukaista,
            lahikouluttaja_id, lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika,
            lahiesimies_id, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
            muokkauspaiva, erikoistuvan_kuittausaika)
         SELECT $1, true,
                $2, true, CURRENT_DATE,
                $2, true, CURRENT_DATE,
                CURRENT_DATE, CURRENT_DATE
         WHERE NOT EXISTS (SELECT 1 FROM koejakson_valiarviointi WHERE opintooikeus_id = $1)`,
        [oid, kouluttajaId]
      )
      await client.query(
        `UPDATE koejakson_valiarviointi
         SET lahikouluttaja_id                 = $2,
             lahikouluttaja_hyvaksynyt         = true,
             lahikouluttajan_kuittausaika      = CURRENT_DATE,
             lahiesimies_id                   = $2,
             lahiesimies_hyvaksynyt            = true,
             lahiesimiehen_kuittausaika        = CURRENT_DATE,
             erikoistuvan_kuittausaika        = CURRENT_DATE,
             edistyminen_tavoitteiden_mukaista = true
         WHERE opintooikeus_id = $1`,
        [oid, kouluttajaId]
      )

      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Poistaa vain loppukeskustelu-rivin erikoistuvalta (käytetään ennen loppukeskustelu-lomakkeen täyttämistä).
   */
  async 'db:cleanupLoppukeskustelu'({ erikoistuvaEmail }: { erikoistuvaEmail: string }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      await client.query(`DELETE FROM koejakson_loppukeskustelu WHERE opintooikeus_id = $1`, [oid])
      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Poistaa loppukeskustelu- ja valiarviointi-rivit erikoistuvalta.
   */
  async 'db:cleanupValiarviointi'({ erikoistuvaEmail }: { erikoistuvaEmail: string }): Promise<null> {
    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      await client.query(`DELETE FROM koejakson_loppukeskustelu WHERE opintooikeus_id = $1`, [oid])
      await client.query(`DELETE FROM koejakson_valiarviointi    WHERE opintooikeus_id = $1`, [oid])
      return null
    } finally {
      await client.end()
    }
  },

  /**
   * Varmistaa, että loppukeskustelu on hyväksytty molemmilta osapuolilta.
   * Kutsuu ensin db:ensureValiarviointiHyvaksytty.
   */
  async 'db:ensureLoppukeskusteluHyvaksytty'({
    erikoistuvaEmail,
    kouluttajaEmail,
  }: {
    erikoistuvaEmail: string
    kouluttajaEmail: string
  }): Promise<null> {
    await koejaksoTasks['db:ensureValiarviointiHyvaksytty']({ erikoistuvaEmail, kouluttajaEmail })

    const client = dbClient()
    await client.connect()
    try {
      const oid: number = await getOpintooikeusId(client, erikoistuvaEmail)
      if (!oid) return null
      const kouluttajaId: number = await getKayttajaId(client, kouluttajaEmail)
      if (!kouluttajaId) return null

      await client.query(
        `INSERT INTO koejakson_loppukeskustelu
           (opintooikeus_id, esitetaan_koejakson_hyvaksymista,
            lahikouluttaja_id, lahikouluttaja_hyvaksynyt, lahikouluttajan_kuittausaika,
            lahiesimies_id, lahiesimies_hyvaksynyt, lahiesimiehen_kuittausaika,
            muokkauspaiva, erikoistuvan_kuittausaika)
         SELECT $1, true,
                $2, true, CURRENT_DATE,
                $2, true, CURRENT_DATE,
                CURRENT_DATE, CURRENT_DATE
         WHERE NOT EXISTS (SELECT 1 FROM koejakson_loppukeskustelu WHERE opintooikeus_id = $1)`,
        [oid, kouluttajaId]
      )
      await client.query(
        `UPDATE koejakson_loppukeskustelu
         SET lahikouluttaja_id            = $2,
             lahikouluttaja_hyvaksynyt    = true,
             lahikouluttajan_kuittausaika = CURRENT_DATE,
             lahiesimies_id              = $2,
             lahiesimies_hyvaksynyt       = true,
             lahiesimiehen_kuittausaika   = CURRENT_DATE,
             erikoistuvan_kuittausaika   = CURRENT_DATE
         WHERE opintooikeus_id = $1`,
        [oid, kouluttajaId]
      )

      return null
    } finally {
      await client.end()
    }
  },
}

// ── Apufunktiot ───────────────────────────────────────────────────────────────

async function getOpintooikeusId(client: import('pg').Client, email: string): Promise<number> {
  const result = await client.query(
    `SELECT o.id
     FROM opintooikeus o
     JOIN erikoistuva_laakari el ON el.id = o.erikoistuva_laakari_id
     JOIN kayttaja k ON k.id = el.kayttaja_id
     JOIN jhi_user u ON u.id = k.user_id
     WHERE u.email = $1 AND o.kaytossa = true
     LIMIT 1`,
    [email]
  )
  return result.rows[0]?.id ?? null
}

async function getKayttajaId(client: import('pg').Client, email: string): Promise<number> {
  const result = await client.query(
    `SELECT k.id FROM kayttaja k JOIN jhi_user u ON u.id = k.user_id WHERE u.email = $1`,
    [email]
  )
  return result.rows[0]?.id ?? null
}
