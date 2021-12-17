package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class SuoritemerkintaHelper {

    companion object {

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
            suorituspaiva: LocalDate = DEFAULT_SUORITUSPAIVA
        ): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = suorituspaiva,
                arviointiasteikonTaso = DEFAULT_LUOTTAMUKSEN_TASO,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                lisatiedot = DEFAULT_LISATIEDOT,
                lukittu = DEFAULT_LUKITTU
            )

            // Lisätään pakollinen tieto
            val suorite: Suorite
            if (em.findAll(Suorite::class).isEmpty()) {
                suorite = SuoriteHelper.createEntity(em, erikoisala)
                em.persist(suorite)
                em.flush()
            } else {
                suorite = em.findAll(Suorite::class)[0]
            }
            suoritemerkinta.suorite = suorite

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, user)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class)[0]
            }
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso

            return suoritemerkinta
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = UPDATED_SUORITUSPAIVA,
                arviointiasteikonTaso = UPDATED_LUOTTAMUKSEN_TASO,
                vaativuustaso = UPDATED_VAATIVUUSTASO,
                lisatiedot = UPDATED_LISATIEDOT,
                lukittu = UPDATED_LUKITTU
            )

            // Lisätään pakollinen tieto
            val suorite: Suorite
            if (em.findAll(Suorite::class).isEmpty()) {
                suorite = SuoriteHelper.createUpdatedEntity(em)
                em.persist(suorite)
                em.flush()
            } else {
                suorite = em.findAll(Suorite::class)[0]
            }
            suoritemerkinta.suorite = suorite

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createUpdatedEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class)[0]
            }
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso
            return suoritemerkinta
        }
    }
}
