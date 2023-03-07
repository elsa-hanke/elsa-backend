package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Koulutusjakso
import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

class KoulutusjaksoHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_MUUT_OSAAMISTAVOITTEET = "AAAAAAAAAA"
        private const val UPDATED_MUUT_OSAAMISTAVOITTEET = "BBBBBBBBBB"

        private val DEFAULT_LUOTU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_LUOTU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_TALLENNETTU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_TALLENNETTU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LUKITTU: Boolean = false
        private const val UPDATED_LUKITTU: Boolean = true

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Koulutusjakso {
            val koulutusjakso = Koulutusjakso(
                nimi = DEFAULT_NIMI,
                muutOsaamistavoitteet = DEFAULT_MUUT_OSAAMISTAVOITTEET,
                luotu = DEFAULT_LUOTU,
                tallennettu = DEFAULT_TALLENNETTU,
                lukittu = DEFAULT_LUKITTU
            )

            // Lisätään pakollinen tieto
            val koulutussuunnitelma: Koulutussuunnitelma
            if (em.findAll(Koulutussuunnitelma::class).isEmpty()) {
                koulutussuunnitelma = KoulutussuunnitelmaHelper.createEntity(em, user)
                em.persist(koulutussuunnitelma)
                em.flush()
            } else {
                koulutussuunnitelma = em.findAll(Koulutussuunnitelma::class).get(0)
            }
            koulutusjakso.koulutussuunnitelma = koulutussuunnitelma

            return koulutusjakso
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Koulutusjakso {
            val koulutusjakso = Koulutusjakso(
                nimi = UPDATED_NIMI,
                muutOsaamistavoitteet = UPDATED_MUUT_OSAAMISTAVOITTEET,
                luotu = UPDATED_LUOTU,
                tallennettu = UPDATED_TALLENNETTU,
                lukittu = UPDATED_LUKITTU
            )

            val koulutussuunnitelma: Koulutussuunnitelma
            if (em.findAll(Koulutussuunnitelma::class).isEmpty()) {
                koulutussuunnitelma = KoulutussuunnitelmaHelper.createUpdatedEntity(em)
                em.persist(koulutussuunnitelma)
                em.flush()
            } else {
                koulutussuunnitelma = em.findAll(Koulutussuunnitelma::class).get(0)
            }
            koulutusjakso.koulutussuunnitelma = koulutussuunnitelma

            return koulutusjakso
        }
    }
}
