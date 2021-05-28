package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.AsiakirjaData
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.web.rest.findAll
import org.hibernate.engine.jdbc.BlobProxy
import java.time.LocalDateTime
import javax.persistence.EntityManager

class AsiakirjaHelper {

    companion object {

        const val ASIAKIRJA_PDF_NIMI: String = "Asiakirja.pdf"
        const val ASIAKIRJA_PNG_NIMI: String = "Asiakirja.png"
        const val ASIAKIRJA_PDF_TYYPPI: String = "application/pdf"
        const val ASIAKIRJA_PNG_TYYPPI: String = "image/png"
        val ASIAKIRJA_PDF_DATA = byteArrayOf(0x2E, 0x38)
        val ASIAKIRJA_PNG_DATA = byteArrayOf(0x2E, 0x14)

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            userId: String? = null,
            tyoskentelyjakso: Tyoskentelyjakso? = null
        ): Asiakirja {
            val asiakirja = Asiakirja(
                nimi = ASIAKIRJA_PDF_NIMI,
                tyyppi = ASIAKIRJA_PDF_TYYPPI,
                lisattypvm = LocalDateTime.now(),
                tyoskentelyjakso = tyoskentelyjakso,
                asiakirjaData = AsiakirjaData(
                    data = BlobProxy.generateProxy(ASIAKIRJA_PDF_DATA)
                )
            )

            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, userId)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            asiakirja.erikoistuvaLaakari = erikoistuvaLaakari

            return asiakirja
        }

    }

}
