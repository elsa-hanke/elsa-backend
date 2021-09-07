package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import javax.persistence.EntityManager

class ErikoistuvaLaakariHelper {

    companion object {

        private const val DEFAULT_PUHELINNUMERO = "AAAAAAAAAA"
        private const val UPDATED_PUHELINNUMERO = "BBBBBBBBBB"

        private const val DEFAULT_OPISKELIJATUNNUS = "AAAAAAAAAA"
        private const val UPDATED_OPISKELIJATUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_YLIOPISTO = "TAYS"

        private val DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA: LocalDate = LocalDate.ofEpochDay(10L)

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari(
                puhelinnumero = DEFAULT_PUHELINNUMERO,
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                opintosuunnitelmaKaytossaPvm = DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA
            )

            // Lisätään pakollinen tieto
            var kayttaja = em.findAll(Kayttaja::class).firstOrNull { it.user == user }
            if (kayttaja == null) {
                kayttaja = KayttajaHelper.createEntity(em, user)
                em.persist(kayttaja)
                em.flush()
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
                opiskelijatunnus = UPDATED_OPISKELIJATUNNUS
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
