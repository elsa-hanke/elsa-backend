package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.web.rest.findAll
import javax.persistence.EntityManager

class ErikoistuvaLaakariHelper {

    companion object {

        private const val DEFAULT_PUHELINNUMERO = "AAAAAAAAAA"
        private const val UPDATED_PUHELINNUMERO = "BBBBBBBBBB"

        private const val DEFAULT_SAHKOPOSTI = "AAAAAAAAAA"
        private const val UPDATED_SAHKOPOSTI = "BBBBBBBBBB"

        private const val DEFAULT_OPISKELIJATUNNUS = "AAAAAAAAAA"
        private const val UPDATED_OPISKELIJATUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_OPINTOJEN_ALOITUSVUOSI: Int = 1900
        private const val UPDATED_OPINTOJEN_ALOITUSVUOSI: Int = 1901

        private const val DEFAULT_YLIOPISTO = "TAYS"

        @JvmStatic
        fun createEntity(em: EntityManager, userId: String? = null): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari(
                puhelinnumero = DEFAULT_PUHELINNUMERO,
                sahkoposti = DEFAULT_SAHKOPOSTI,
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                opintojenAloitusvuosi = DEFAULT_OPINTOJEN_ALOITUSVUOSI
            )

            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createEntity(em, userId)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
            em.persist(yliopisto)
            kayttaja.yliopisto = yliopisto
            erikoistuvaLaakari.kayttaja = kayttaja

            // Lisätään pakollinen tieto
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaHelper.createEntity()
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class).get(0)
            }
            erikoistuvaLaakari.erikoisala = erikoisala

            return erikoistuvaLaakari
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari(
                puhelinnumero = UPDATED_PUHELINNUMERO,
                sahkoposti = UPDATED_SAHKOPOSTI,
                opiskelijatunnus = UPDATED_OPISKELIJATUNNUS,
                opintojenAloitusvuosi = UPDATED_OPINTOJEN_ALOITUSVUOSI
            )

            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            erikoistuvaLaakari.kayttaja = kayttaja

            return erikoistuvaLaakari
        }
    }
}
