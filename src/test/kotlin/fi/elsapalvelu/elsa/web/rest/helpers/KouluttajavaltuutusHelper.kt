package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import jakarta.persistence.EntityManager

object KouluttajavaltuutusHelper {

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

            kouluttajavaltuutus.valtuuttajaOpintooikeus = opintooikeus ?: Testisuhteet.opintooikeus(em, null)
            kouluttajavaltuutus.valtuutettu = valtuutettu ?: KayttajaHelper.createEntity(em).also(em::persist)

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

            kouluttajavaltuutus.valtuuttajaOpintooikeus = opintooikeus ?: Testisuhteet.opintooikeus(em, null)
            kouluttajavaltuutus.valtuutettu = valtuutettu ?: KayttajaHelper.createEntity(em).also(em::persist)

            return kouluttajavaltuutus
        }

}
