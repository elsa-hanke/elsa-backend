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
        private val DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(20L)

        const val DEFAULT_ASETUS = "55/2020"

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            opintooikeudenAlkamispaiva: LocalDate? = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
            opintooikeudenPaattymispaiva: LocalDate? = DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA,
            erikoisala: Erikoisala? = null
        ): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari()

            var kayttaja = em.findAll(Kayttaja::class).firstOrNull { it.user == user }
            if (kayttaja == null) {
                kayttaja = KayttajaHelper.createEntity(em, user)
                em.persist(kayttaja)
                em.flush()
            }

            erikoistuvaLaakari.kayttaja = kayttaja
            em.persist(erikoistuvaLaakari)

            val yliopisto: Yliopisto
            if (em.findAll(Yliopisto::class).isEmpty()) {
                yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
                em.persist(yliopisto)
                em.flush()
            } else {
                yliopisto = em.findAll(Yliopisto::class).get(0)
            }

            var erikoistuvanErikoisala = erikoisala
            if (erikoistuvanErikoisala == null) {
                if (em.findAll(Erikoisala::class).isEmpty()) {
                    erikoistuvanErikoisala = ErikoisalaHelper.createEntity()
                    em.persist(erikoisala)
                    em.flush()
                } else {
                    erikoistuvanErikoisala = em.findAll(Erikoisala::class).get(0)
                }
            }

            val opintoopas: Opintoopas
            if (em.findAll(Opintoopas::class).isEmpty()) {
                opintoopas = OpintoopasHelper.createEntity(em)
                em.persist(opintoopas)
                em.flush()
            } else {
                opintoopas = em.findAll(Opintoopas::class).get(0)
            }
            opintoopas.erikoisala = erikoistuvanErikoisala

            val asetus: Asetus
            if (em.findAll(Asetus::class).isEmpty()) {
                asetus = Asetus(nimi = DEFAULT_ASETUS)
                em.persist(asetus)
                em.flush()
            } else {
                asetus = em.findAll(Asetus::class).get(0)
            }

            val opintooikeus = Opintooikeus(
                opintooikeudenMyontamispaiva = opintooikeudenAlkamispaiva,
                opintooikeudenPaattymispaiva = opintooikeudenPaattymispaiva,
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                osaamisenArvioinninOppaanPvm = DEFAULT_ERIKOISTUMISEN_ALOITUSPAIVA,
                erikoistuvaLaakari = erikoistuvaLaakari,
                yliopisto = yliopisto,
                erikoisala = erikoistuvanErikoisala,
                opintoopas = opintoopas,
                asetus = asetus,
                kaytossa = true
            )
            em.persist(opintooikeus)
            em.flush()

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
