package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Koulutusjakso
import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.web.rest.findAll
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.ZoneId

object KoulutusjaksoHelper {

    private const val DEFAULT_NIMI = "AAAAAAAAAA"
    private const val UPDATED_NIMI = "BBBBBBBBBB"

    private const val DEFAULT_MUUT_OSAAMISTAVOITTEET = "AAAAAAAAAA"
    private const val UPDATED_MUUT_OSAAMISTAVOITTEET = "BBBBBBBBBB"

    private val DEFAULT_LUOTU = LocalDate.ofEpochDay(0L)
    private val UPDATED_LUOTU = LocalDate.now(ZoneId.systemDefault())

    private val DEFAULT_TALLENNETTU = LocalDate.ofEpochDay(0L)
    private val UPDATED_TALLENNETTU = LocalDate.now(ZoneId.systemDefault())

    private const val DEFAULT_LUKITTU = false
    private const val UPDATED_LUKITTU = true

    fun createEntity(em: EntityManager, user: User? = null): Koulutusjakso {
        val koulutusjakso = Koulutusjakso(
            nimi = DEFAULT_NIMI,
            muutOsaamistavoitteet = DEFAULT_MUUT_OSAAMISTAVOITTEET,
            luotu = DEFAULT_LUOTU,
            tallennettu = DEFAULT_TALLENNETTU,
            lukittu = DEFAULT_LUKITTU
        )

        val koulutussuunnitelma = resolveKoulutussuunnitelma(em, user, updated = false)
        koulutusjakso.koulutussuunnitelma = koulutussuunnitelma

        return koulutusjakso
    }

    fun createUpdatedEntity(em: EntityManager): Koulutusjakso {
        val koulutusjakso = Koulutusjakso(
            nimi = UPDATED_NIMI,
            muutOsaamistavoitteet = UPDATED_MUUT_OSAAMISTAVOITTEET,
            luotu = UPDATED_LUOTU,
            tallennettu = UPDATED_TALLENNETTU,
            lukittu = UPDATED_LUKITTU
        )

        val koulutussuunnitelma = resolveKoulutussuunnitelma(em, updated = true)
        koulutusjakso.koulutussuunnitelma = koulutussuunnitelma

        return koulutusjakso
    }

    private fun resolveKoulutussuunnitelma(
        em: EntityManager,
        user: User? = null,
        updated: Boolean
    ): Koulutussuunnitelma {
        val existing = em.findAll(Koulutussuunnitelma::class).firstOrNull()

        if (existing != null) return existing

        val created =
            if (updated) {
                KoulutussuunnitelmaHelper.createUpdatedEntity(em)
            } else {
                KoulutussuunnitelmaHelper.createEntity(em, user)
            }

        em.persist(created)
        em.flush()

        return created
    }
}
