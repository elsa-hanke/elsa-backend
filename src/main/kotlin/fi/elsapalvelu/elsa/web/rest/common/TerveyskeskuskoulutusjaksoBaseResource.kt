package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.KayttajaService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.web.rest.TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.toFileDownloadResponse
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.http.ResponseEntity

abstract class TerveyskeskuskoulutusjaksoBaseResource(
    protected val userService: UserService,
    protected val kayttajaService: KayttajaService,
    protected val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    protected val asiakirjaService: AsiakirjaService
) {

    /**
     * Wraps a block in the standard Terveyskeskuskoulutusjakso exception-handling:
     *  - EntityNotFoundException  → "Vastuuhenkilöä ei löytynyt"
     *  - ValidationException      → "vähimmäispituus ei täyty"
     */
    protected fun <T> withTerveyskeskusExceptionHandling(block: () -> ResponseEntity<T>): ResponseEntity<T> =
        try {
            block()
        } catch (e: EntityNotFoundException) {
            throw BadRequestAlertException(
                "Vastuuhenkilöä ei löytynyt",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.vastuuhenkiloa-ei-loytynyt"
            )
        } catch (e: ValidationException) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson vähimmäispituus ei täyty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-vahimmaispituus-ei-tayty"
            )
        }

    /**
     * Converts an [AsiakirjaDTO] into a file-download [ResponseEntity], or 404 when null.
     */
    protected fun buildAsiakirjaDownloadResponse(asiakirja: AsiakirjaDTO?): ResponseEntity<ByteArray> =
        asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi ?: "", asiakirja.tyyppi ?: "")
            ?: ResponseEntity.notFound().build()
}

