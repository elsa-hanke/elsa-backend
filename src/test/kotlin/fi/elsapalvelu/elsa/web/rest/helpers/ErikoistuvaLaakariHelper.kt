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

        const val DEFAULT_YLIOPISTO = "TAYS"

        private val DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA: LocalDate = LocalDate.ofEpochDay(10L)

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari()

            var kayttaja = em.findAll(Kayttaja::class).firstOrNull { it.user == user }
            if (kayttaja == null) {
                kayttaja = KayttajaHelper.createEntity(em, user)
                em.persist(kayttaja)
                em.flush()
            }

            val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
            em.persist(yliopisto)
            erikoistuvaLaakari.kayttaja = kayttaja

            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaHelper.createEntity()
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class).get(0)
            }

            em.persist(erikoistuvaLaakari)

            val opintooikeus: Opintooikeus
            if (em.findAll(Opintooikeus::class).isEmpty()) {
                opintooikeus = Opintooikeus(
                    opintooikeudenMyontamispaiva = LocalDate.ofEpochDay(0L),
                    opintooikeudenPaattymispaiva = LocalDate.ofEpochDay(10L),
                    opiskelijatunnus = "123456",
                    opintosuunnitelmaKaytossaPvm = DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA,
                    erikoistuvaLaakari = erikoistuvaLaakari,
                    yliopisto = yliopisto,
                    erikoisala = erikoisala
                )
                em.persist(opintooikeus)
                em.flush()
            } else {
                opintooikeus = em.findAll(Opintooikeus::class).get(0)
            }
            erikoistuvaLaakari.opintooikeudet.add(opintooikeus)

            return erikoistuvaLaakari
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari()

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
