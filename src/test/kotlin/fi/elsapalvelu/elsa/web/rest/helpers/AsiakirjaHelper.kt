package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDateTime
import jakarta.persistence.EntityManager

object AsiakirjaHelper {

    const val ASIAKIRJA_PDF_NIMI = "Asiakirja.pdf"
    const val ASIAKIRJA_PNG_NIMI = "Asiakirja.png"

    const val ASIAKIRJA_PDF_TYYPPI = "application/pdf"
    const val ASIAKIRJA_PNG_TYYPPI = "image/png"

    val ASIAKIRJA_PDF_DATA = byteArrayOf(0x2E, 0x38)
    val ASIAKIRJA_PNG_DATA = byteArrayOf(0x2E, 0x14)

    fun createEntity(
        em: EntityManager,
        user: User? = null,
        tyoskentelyjakso: Tyoskentelyjakso? = null,
        teoriakoulutus: Teoriakoulutus? = null
    ): Asiakirja {
        val asiakirja = Asiakirja(
            nimi = ASIAKIRJA_PDF_NIMI,
            tyyppi = ASIAKIRJA_PDF_TYYPPI,
            lisattypvm = LocalDateTime.now(),
            tyoskentelyjakso = tyoskentelyjakso,
            teoriakoulutus = teoriakoulutus,
            asiakirjaData = AsiakirjaData(
                data = ASIAKIRJA_PDF_DATA
            )
        )

        val erikoistuvaLaakari =
            em.findAll(ErikoistuvaLaakari::class)
                .firstOrNull { it.kayttaja?.user == user }
                ?: ErikoistuvaLaakariHelper.createEntity(em, user).also {
                    em.persist(it)
                    em.flush()
                }

        asiakirja.opintooikeus =
            erikoistuvaLaakari.getOpintooikeusKaytossa()

        return asiakirja
    }
}
