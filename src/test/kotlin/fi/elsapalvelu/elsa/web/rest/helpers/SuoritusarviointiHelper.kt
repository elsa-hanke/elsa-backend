package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class SuoritusarviointiHelper {

    companion object {

        private val DEFAULT_TAPAHTUMAN_AJANKOHTA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_TAPAHTUMAN_AJANKOHTA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_ARVIOITAVA_TAPAHTUMA = "AAAAAAAAAA"
        private const val UPDATED_ARVIOITAVA_TAPAHTUMA = "BBBBBBBBBB"

        private val DEFAULT_PYYNNON_AIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PYYNNON_AIKA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LISATIEDOT = "AAAAAAAAAA"
        private const val UPDATED_LISATIEDOT = "BBBBBBBBBB"

        private const val DEFAULT_ITSEARVIOINTI_VAATIVUUSTASO: Int = 1
        private const val UPDATED_ITSEARVIOINTI_VAATIVUUSTASO: Int = 2

        private const val DEFAULT_ITSEARVIOINTI_LUOTTAMUKSEN_TASO: Int = 1
        private const val UPDATED_ITSEARVIOINTI_LUOTTAMUKSEN_TASO: Int = 2

        private const val DEFAULT_SANALLINEN_ITSEARVIOINTI = "AAAAAAAAAA"
        private const val UPDATED_SANALLINEN_ITSEARVIOINTI = "BBBBBBBBBB"

        private const val DEFAULT_VAATIVUUSTASO: Int = 1
        private const val UPDATED_VAATIVUUSTASO: Int = 2

        private const val DEFAULT_LUOTTAMUKSEN_TASO: Int = 1
        private const val UPDATED_LUOTTAMUKSEN_TASO: Int = 2

        private const val DEFAULT_SANALLINEN_ARVIOINTI = "AAAAAAAAAA"
        private const val UPDATED_SANALLINEN_ARVIOINTI = "BBBBBBBBBB"

        private val DEFAULT_ARVIOINTI_AIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ARVIOINTI_AIKA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LUKITTU: Boolean = false
        private const val UPDATED_LUKITTU: Boolean = true

        @JvmStatic
        fun createEntity(em: EntityManager): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = DEFAULT_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = DEFAULT_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = DEFAULT_PYYNNON_AIKA,
                lisatiedot = DEFAULT_LISATIEDOT,
                itsearviointiVaativuustaso = DEFAULT_ITSEARVIOINTI_VAATIVUUSTASO,
                itsearviointiLuottamuksenTaso = DEFAULT_ITSEARVIOINTI_LUOTTAMUKSEN_TASO,
                sanallinenItsearviointi = DEFAULT_SANALLINEN_ITSEARVIOINTI,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                luottamuksenTaso = DEFAULT_LUOTTAMUKSEN_TASO,
                sanallinenArviointi = DEFAULT_SANALLINEN_ARVIOINTI,
                arviointiAika = DEFAULT_ARVIOINTI_AIKA,
                lukittu = DEFAULT_LUKITTU
            )

            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Add required entity
            val epaOsaamisalue: EpaOsaamisalue
            if (em.findAll(EpaOsaamisalue::class).isEmpty()) {
                epaOsaamisalue = EpaOsaamisalueHelper.createEntity()
                em.persist(epaOsaamisalue)
                em.flush()
            } else {
                epaOsaamisalue = em.findAll(EpaOsaamisalue::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = epaOsaamisalue

            // Add required entity
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso

            return suoritusarviointi
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = UPDATED_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = UPDATED_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = UPDATED_PYYNNON_AIKA,
                lisatiedot = UPDATED_LISATIEDOT,
                itsearviointiVaativuustaso = UPDATED_ITSEARVIOINTI_VAATIVUUSTASO,
                itsearviointiLuottamuksenTaso = UPDATED_ITSEARVIOINTI_LUOTTAMUKSEN_TASO,
                sanallinenItsearviointi = UPDATED_SANALLINEN_ITSEARVIOINTI,
                vaativuustaso = UPDATED_VAATIVUUSTASO,
                luottamuksenTaso = UPDATED_LUOTTAMUKSEN_TASO,
                sanallinenArviointi = UPDATED_SANALLINEN_ARVIOINTI,
                arviointiAika = UPDATED_ARVIOINTI_AIKA,
                lukittu = UPDATED_LUKITTU
            )

            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Add required entity
            val epaOsaamisalue: EpaOsaamisalue
            if (em.findAll(EpaOsaamisalue::class).isEmpty()) {
                epaOsaamisalue = EpaOsaamisalueHelper.createUpdatedEntity()
                em.persist(epaOsaamisalue)
                em.flush()
            } else {
                epaOsaamisalue = em.findAll(EpaOsaamisalue::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = epaOsaamisalue

            // Add required entity
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createUpdatedEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso

            return suoritusarviointi
        }
    }
}
