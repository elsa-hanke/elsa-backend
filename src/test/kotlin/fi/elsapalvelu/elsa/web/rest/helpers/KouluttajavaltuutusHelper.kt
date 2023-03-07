package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import jakarta.persistence.EntityManager

class KouluttajavaltuutusHelper {

    companion object {

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VALTUUTUKSEN_LUONTIAIKA: Instant = Instant.ofEpochMilli(0L)
        private val UPDATED_VALTUUTUKSEN_LUONTIAIKA: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        private val DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA: Instant = Instant.ofEpochMilli(0L)
        private val UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            alkamispaiva: LocalDate? = DEFAULT_ALKAMISPAIVA,
            paattymispaiva: LocalDate? = DEFAULT_PAATTYMISPAIVA,
            opintooikeus: Opintooikeus? = null,
            valtuutettu: Kayttaja? = null
        ): Kouluttajavaltuutus {
            val kouluttajavaltuutus = Kouluttajavaltuutus(
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                valtuutuksenLuontiaika = DEFAULT_VALTUUTUKSEN_LUONTIAIKA,
                valtuutuksenMuokkausaika = DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA
            )

            if (opintooikeus == null) {
                val erikoistuvaLaakari: ErikoistuvaLaakari
                if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                    erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
                    em.persist(erikoistuvaLaakari)
                    em.flush()
                } else {
                    erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
                }
                kouluttajavaltuutus.valtuuttajaOpintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            } else {
                kouluttajavaltuutus.valtuuttajaOpintooikeus = opintooikeus
            }

            if (valtuutettu == null) {
                val kayttaja: Kayttaja
                if (em.findAll(Kayttaja::class).isEmpty()) {
                    kayttaja = KayttajaHelper.createEntity(em)
                    em.persist(kayttaja)
                    em.flush()
                } else {
                    kayttaja = em.findAll(Kayttaja::class).get(0)
                }
                kouluttajavaltuutus.valtuutettu = kayttaja
            } else {
                kouluttajavaltuutus.valtuutettu = valtuutettu
            }

            return kouluttajavaltuutus
        }

        @JvmStatic
        fun createUpdatedEntity(
            em: EntityManager,
            alkamispaiva: LocalDate? = UPDATED_ALKAMISPAIVA,
            paattymispaiva: LocalDate? = UPDATED_PAATTYMISPAIVA,
            opintooikeus: Opintooikeus? = null,
            valtuutettu: Kayttaja? = null
        ): Kouluttajavaltuutus {
            val kouluttajavaltuutus = Kouluttajavaltuutus(
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                valtuutuksenLuontiaika = UPDATED_VALTUUTUKSEN_LUONTIAIKA,
                valtuutuksenMuokkausaika = UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA
            )

            if (opintooikeus == null) {
                val erikoistuvaLaakari: ErikoistuvaLaakari
                if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                    erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
                    em.persist(erikoistuvaLaakari)
                    em.flush()
                } else {
                    erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
                }
                kouluttajavaltuutus.valtuuttajaOpintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            } else {
                kouluttajavaltuutus.valtuuttajaOpintooikeus = opintooikeus
            }

            if (valtuutettu == null) {
                val kayttaja: Kayttaja
                if (em.findAll(Kayttaja::class).isEmpty()) {
                    kayttaja = KayttajaHelper.createEntity(em)
                    em.persist(kayttaja)
                    em.flush()
                } else {
                    kayttaja = em.findAll(Kayttaja::class).get(0)
                }
                kouluttajavaltuutus.valtuutettu = kayttaja
            } else {
                kouluttajavaltuutus.valtuutettu = valtuutettu
            }

            return kouluttajavaltuutus
        }
    }
}
