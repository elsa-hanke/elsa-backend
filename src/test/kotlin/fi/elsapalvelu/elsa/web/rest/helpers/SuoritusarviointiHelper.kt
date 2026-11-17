package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

object SuoritusarviointiHelper {

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
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            tapahtumanAjankohta: LocalDate = DEFAULT_TAPAHTUMAN_AJANKOHTA,
            arviointiasteikonTaso: Int? = DEFAULT_LUOTTAMUKSEN_TASO,
            arvioitavaKokonaisuus: ArvioitavaKokonaisuus? = null,
            tyoskentelyjakso: Tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, user).also(em::persist),
            arvioinninAntaja: Kayttaja = Testisuhteet.kayttaja(em, user)
        ): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = tapahtumanAjankohta,
                arvioitavaTapahtuma = DEFAULT_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = DEFAULT_PYYNNON_AIKA,
                lisatiedot = DEFAULT_LISATIEDOT,
                itsearviointiVaativuustaso = DEFAULT_ITSEARVIOINTI_VAATIVUUSTASO,
                sanallinenItsearviointi = DEFAULT_SANALLINEN_ITSEARVIOINTI,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                sanallinenArviointi = DEFAULT_SANALLINEN_ARVIOINTI,
                arviointiAika = DEFAULT_ARVIOINTI_AIKA,
                lukittu = DEFAULT_LUKITTU
            )

            suoritusarviointi.arvioinninAntaja = arvioinninAntaja

            val suoritusarvioinninArvioitavaKokonaisuus = SuoritusarvioinninArvioitavaKokonaisuus(
                suoritusarviointi = suoritusarviointi,
                itsearviointiArviointiasteikonTaso = DEFAULT_ITSEARVIOINTI_LUOTTAMUKSEN_TASO,
                arviointiasteikonTaso = arviointiasteikonTaso
            )

            suoritusarvioinninArvioitavaKokonaisuus.arvioitavaKokonaisuus = arvioitavaKokonaisuus ?: ArvioitavaKokonaisuusHelper.createEntity(em, kategoria =
                ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala = tyoskentelyjakso.opintooikeus?.erikoisala).also(em::persist)
            ).also(em::persist)
            suoritusarviointi.arvioitavatKokonaisuudet = mutableSetOf(suoritusarvioinninArvioitavaKokonaisuus)
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso
            suoritusarviointi.arviointiasteikko = tyoskentelyjakso.opintooikeus?.opintoopas?.arviointiasteikko

            return suoritusarviointi
        }

        @JvmStatic
        fun createUpdatedEntity(
            tyoskentelyjakso: Tyoskentelyjakso,
            arvioinninAntaja: Kayttaja,
            arvioitavaKokonaisuus: ArvioitavaKokonaisuus
        ): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = UPDATED_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = UPDATED_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = UPDATED_PYYNNON_AIKA,
                lisatiedot = UPDATED_LISATIEDOT,
                itsearviointiVaativuustaso = UPDATED_ITSEARVIOINTI_VAATIVUUSTASO,
                sanallinenItsearviointi = UPDATED_SANALLINEN_ITSEARVIOINTI,
                vaativuustaso = UPDATED_VAATIVUUSTASO,
                sanallinenArviointi = UPDATED_SANALLINEN_ARVIOINTI,
                arviointiAika = UPDATED_ARVIOINTI_AIKA,
                lukittu = UPDATED_LUKITTU
            )

            suoritusarviointi.arvioinninAntaja = arvioinninAntaja

            val suoritusarvioinninArvioitavaKokonaisuus = SuoritusarvioinninArvioitavaKokonaisuus(
                suoritusarviointi = suoritusarviointi,
                itsearviointiArviointiasteikonTaso = UPDATED_ITSEARVIOINTI_LUOTTAMUKSEN_TASO,
                arviointiasteikonTaso = UPDATED_LUOTTAMUKSEN_TASO
            )

            suoritusarvioinninArvioitavaKokonaisuus.arvioitavaKokonaisuus = arvioitavaKokonaisuus
            suoritusarviointi.arvioitavatKokonaisuudet = mutableSetOf(suoritusarvioinninArvioitavaKokonaisuus)
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso
            suoritusarviointi.arviointiasteikko = tyoskentelyjakso.opintooikeus?.opintoopas?.arviointiasteikko

            return suoritusarviointi
        }

}
