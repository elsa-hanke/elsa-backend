package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.service.dto.suoritteet.VanhentuneetSuorituksetDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoSuoritustenTilaDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.kayttaja.FileValidationService
import fi.elsapalvelu.elsa.web.rest.VALMISTUMISPYYNTO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class ValmistumispyyntoApplicationService(
    private val valmistumispyyntoService: ValmistumispyyntoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val fileValidationService: FileValidationService
) {

    @Transactional(readOnly = true)
    fun findOne(opintooikeusId: Long): ValmistumispyyntoDTO? =
        valmistumispyyntoService.findOneByOpintooikeusId(opintooikeusId)

    @Transactional(readOnly = true)
    fun findSuoritustenTila(opintooikeusId: Long): ValmistumispyyntoSuoritustenTilaDTO {
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhentuneetSuoritukset = valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)

        return ValmistumispyyntoSuoritustenTilaDTO(
            erikoisalaTyyppi = erikoisalaTyyppi,
            vanhojaTyoskentelyjaksojaOrSuorituksiaExists =
                vanhentuneetSuoritukset.vanhojaTyoskentelyjaksojaOrSuorituksiaExists,
            kuulusteluVanhentunut = vanhentuneetSuoritukset.kuulusteluVanhentunut
        )
    }

    @Transactional
    fun create(
        userId: String,
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?,
        entityName: String = VALMISTUMISPYYNTO_ENTITY_NAME
    ): ValmistumispyyntoDTO {
        validateRequest(userId, opintooikeusId, uusiValmistumispyyntoDTO, laillistamistodistus, entityName)
        validateValmistumispyyntoNotExists(opintooikeusId, entityName)
        validateLaillistamistodistusIfExists(laillistamistodistus, entityName)
        updateLaillistamistiedot(userId, uusiValmistumispyyntoDTO, laillistamistodistus)

        return valmistumispyyntoService.create(opintooikeusId, uusiValmistumispyyntoDTO)
    }

    @Transactional
    fun update(
        userId: String,
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?,
        entityName: String = VALMISTUMISPYYNTO_ENTITY_NAME
    ): ValmistumispyyntoDTO = update(
        userId,
        opintooikeusId,
        uusiValmistumispyyntoDTO,
        laillistamistodistus,
        requireNotSent = false,
        entityName = entityName
    )

    @Transactional
    fun updateWhenNotSent(
        userId: String,
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?,
        entityName: String = VALMISTUMISPYYNTO_ENTITY_NAME
    ): ValmistumispyyntoDTO = update(
        userId,
        opintooikeusId,
        uusiValmistumispyyntoDTO,
        laillistamistodistus,
        requireNotSent = true,
        entityName = entityName
    )

    private fun update(
        userId: String,
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?,
        requireNotSent: Boolean,
        entityName: String
    ): ValmistumispyyntoDTO {
        validateRequest(userId, opintooikeusId, uusiValmistumispyyntoDTO, laillistamistodistus, entityName)
        validateLaillistamistodistusIfExists(laillistamistodistus, entityName)
        if (requireNotSent) {
            validateValmistumispyyntoNotSent(opintooikeusId, entityName)
        }
        updateLaillistamistiedot(userId, uusiValmistumispyyntoDTO, laillistamistodistus)

        return valmistumispyyntoService.update(opintooikeusId, uusiValmistumispyyntoDTO)
    }

    private fun validateRequest(
        userId: String,
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?,
        entityName: String
    ) {
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhentuneetSuoritukset = valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)

        validateLaillistamispaivaAndTodistus(
            userId,
            uusiValmistumispyyntoDTO,
            laillistamistodistus,
            entityName
        )
        validateVanhentuneetSuoritukset(uusiValmistumispyyntoDTO, vanhentuneetSuoritukset, entityName)
    }

    private fun validateValmistumispyyntoNotExists(opintooikeusId: Long, entityName: String) {
        if (valmistumispyyntoService.existsByOpintooikeusId(opintooikeusId)) {
            throw BadRequestAlertException(
                "Valmistumispyyntö on jo lähetetty",
                entityName,
                "dataillegal.valmistumispyynto-on-jo-lahetetty"
            )
        }
    }

    private fun validateValmistumispyyntoNotSent(opintooikeusId: Long, entityName: String) {
        if (valmistumispyyntoService.onkoLahetetty(opintooikeusId)) {
            throw BadRequestAlertException(
                "Lähetettyä valmistumispyyntöä ei saa muokata.",
                entityName,
                "dataillegal.lahetettya-valmistumispyyntoa-ei-saa-muokata"
            )
        }
    }

    private fun validateLaillistamispaivaAndTodistus(
        userId: String,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?,
        entityName: String
    ) {
        if ((uusiValmistumispyyntoDTO.laillistamispaiva == null || laillistamistodistus == null) &&
            !erikoistuvaLaakariService.laillistamispaivaAndTodistusExists(userId)
        ) {
            throw BadRequestAlertException(
                "Laillistamispaiva ja todistus vaaditaan",
                entityName,
                "dataillegal.laillistamispaiva-ja-todistus-vaaditaan"
            )
        }
    }

    private fun validateVanhentuneetSuoritukset(
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        vanhentuneetSuoritukset: VanhentuneetSuorituksetDTO,
        entityName: String
    ) {
        if (uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista == null &&
            (vanhentuneetSuoritukset.vanhojaTyoskentelyjaksojaOrSuorituksiaExists == true ||
                vanhentuneetSuoritukset.kuulusteluVanhentunut == true)
        ) {
            throw BadRequestAlertException(
                "Selvitys vanhentuneista suorituksista vaaditaan",
                entityName,
                "dataillegal.selvitys-vanhentuneista-suorituksista-vaaditaan"
            )
        }
    }

    private fun validateLaillistamistodistusIfExists(
        laillistamistodistus: MultipartFile?,
        entityName: String
    ) {
        if (laillistamistodistus != null && !fileValidationService.validate(listOf(laillistamistodistus))) {
            throw BadRequestAlertException(
                "Tiedosto ei ole kelvollinen.",
                entityName,
                "dataillegal.tiedosto-ei-ole-kelvollinen"
            )
        }
    }

    private fun updateLaillistamistiedot(
        userId: String,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?
    ) {
        erikoistuvaLaakariService.updateLaillistamispaiva(
            userId,
            uusiValmistumispyyntoDTO.laillistamispaiva,
            laillistamistodistus?.bytes,
            laillistamistodistus?.originalFilename,
            laillistamistodistus?.contentType
        )
    }
}
