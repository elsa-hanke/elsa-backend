package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
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
                    data = BlobProxy.generateProxy(ASIAKIRJA_PDF_DATA)
                )
            )

            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            asiakirja.erikoistuvaLaakari = erikoistuvaLaakari

            return asiakirja
        }

    }

}
