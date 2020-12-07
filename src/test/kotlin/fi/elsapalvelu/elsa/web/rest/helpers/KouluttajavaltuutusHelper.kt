package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.persistence.EntityManager

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
        fun createEntity(em: EntityManager): Kouluttajavaltuutus {
            val kouluttajavaltuutus = Kouluttajavaltuutus(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                valtuutuksenLuontiaika = DEFAULT_VALTUUTUKSEN_LUONTIAIKA,
                valtuutuksenMuokkausaika = DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA
            )

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            kouluttajavaltuutus.valtuuttaja = erikoistuvaLaakari

            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            kouluttajavaltuutus.valtuutettu = kayttaja

            return kouluttajavaltuutus
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Kouluttajavaltuutus {
            val kouluttajavaltuutus = Kouluttajavaltuutus(
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                valtuutuksenLuontiaika = UPDATED_VALTUUTUKSEN_LUONTIAIKA,
                valtuutuksenMuokkausaika = UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA
            )

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            kouluttajavaltuutus.valtuuttaja = erikoistuvaLaakari
            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            kouluttajavaltuutus.valtuutettu = kayttaja
            return kouluttajavaltuutus
        }
    }
}
