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

object SuoritemerkintaHelper {

        private val DEFAULT_SUORITUSPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_SUORITUSPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LUOTTAMUKSEN_TASO: Int = 1
        private const val UPDATED_LUOTTAMUKSEN_TASO: Int = 2

        private const val DEFAULT_VAATIVUUSTASO: Int = 1
        private const val UPDATED_VAATIVUUSTASO: Int = 2

        private const val DEFAULT_LISATIEDOT = "AAAAAAAAAA"
        private const val UPDATED_LISATIEDOT = "BBBBBBBBBB"

        private const val DEFAULT_LUKITTU: Boolean = false
        private const val UPDATED_LUKITTU: Boolean = true

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            erikoisala: Erikoisala? = null,
            user: User? = null,
            suorituspaiva: LocalDate = DEFAULT_SUORITUSPAIVA,
            suorite: Suorite? = null,
            tyoskentelyjakso: Tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, user).also(em::persist)
        ): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = suorituspaiva,
                arviointiasteikonTaso = DEFAULT_LUOTTAMUKSEN_TASO,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                lisatiedot = DEFAULT_LISATIEDOT,
                lukittu = DEFAULT_LUKITTU
            )

            suoritemerkinta.suorite = suorite ?: SuoriteHelper.createEntity(em, erikoisala ?: tyoskentelyjakso.opintooikeus?.erikoisala).also(em::persist)
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso
            suoritemerkinta.arviointiasteikko = tyoskentelyjakso.opintooikeus?.opintoopas?.arviointiasteikko

            return suoritemerkinta
        }

        @JvmStatic
        fun createUpdatedEntity(tyoskentelyjakso: Tyoskentelyjakso, suorite: Suorite): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = UPDATED_SUORITUSPAIVA,
                arviointiasteikonTaso = UPDATED_LUOTTAMUKSEN_TASO,
                vaativuustaso = UPDATED_VAATIVUUSTASO,
                lisatiedot = UPDATED_LISATIEDOT,
                lukittu = UPDATED_LUKITTU
            )

            suoritemerkinta.suorite = suorite
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso
            suoritemerkinta.arviointiasteikko = tyoskentelyjakso.opintooikeus?.opintoopas?.arviointiasteikko

            return suoritemerkinta
        }

}
