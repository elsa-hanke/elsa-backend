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
import java.time.LocalDateTime
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import jakarta.persistence.EntityManager

object AsiakirjaHelper {

    const val ASIAKIRJA_PDF_NIMI = "Asiakirja.pdf"
    const val ASIAKIRJA_PNG_NIMI = "Asiakirja.png"

    const val ASIAKIRJA_PDF_TYYPPI = "application/pdf"
    const val ASIAKIRJA_PNG_TYYPPI = "image/png"

    val ASIAKIRJA_PDF_DATA: ByteArray =
        requireNotNull(AsiakirjaHelper::class.java.getResourceAsStream("/fixtures/valid.pdf"))
            .use { it.readBytes() }
    val ASIAKIRJA_PNG_DATA = byteArrayOf(0x2E, 0x14)

    fun createEntity(
        em: EntityManager,
        user: User? = null,
        tyoskentelyjakso: Tyoskentelyjakso? = null,
        teoriakoulutus: Teoriakoulutus? = null,
        opintooikeus: Opintooikeus = tyoskentelyjakso?.opintooikeus ?: teoriakoulutus?.opintooikeus
            ?: Testisuhteet.opintooikeus(em, user)
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

        asiakirja.opintooikeus = opintooikeus

        return asiakirja
    }
}
