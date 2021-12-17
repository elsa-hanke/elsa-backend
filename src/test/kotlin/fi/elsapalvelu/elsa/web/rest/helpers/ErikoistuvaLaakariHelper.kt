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
        const val DEFAULT_ASETUS = "55/2020"

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

            val opintoopas: Opintoopas
            if (em.findAll(Opintoopas::class).isEmpty()) {
                opintoopas = OpintoopasHelper.createEntity()
                em.persist(opintoopas)
                em.flush()
            } else {
                opintoopas = em.findAll(Opintoopas::class).get(0)
            }

            // Lisätään pakollinen tieto
            val arviointiasteikko: Arviointiasteikko
            if (em.findAll(Arviointiasteikko::class).isEmpty()) {
                arviointiasteikko = ArviointiasteikkoHelper.createEntity()
                em.persist(arviointiasteikko)
                em.flush()
            } else {
                arviointiasteikko = em.findAll(Arviointiasteikko::class).get(0)
            }
            opintoopas.arviointiasteikko = arviointiasteikko

            // Lisätään pakollinen tieto
            val asetus: Asetus
            if (em.findAll(Asetus::class).isEmpty()) {
                asetus = Asetus(nimi = DEFAULT_ASETUS)
                em.persist(asetus)
                em.flush()
            } else {
                asetus = em.findAll(Asetus::class).get(0)
            }

            val opintooikeus: Opintooikeus
            if (em.findAll(Opintooikeus::class).isEmpty()) {
                opintooikeus = Opintooikeus(
                    opintooikeudenMyontamispaiva = LocalDate.ofEpochDay(0L),
                    opintooikeudenPaattymispaiva = LocalDate.ofEpochDay(10L),
                    opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                    osaamisenArvioinninOppaanPvm = DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA,
                    erikoistuvaLaakari = erikoistuvaLaakari,
                    yliopisto = yliopisto,
                    erikoisala = erikoisala,
                    opintoopas = opintoopas,
                    asetus = asetus
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
