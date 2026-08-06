package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.kayttaja.FileValidationService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.tyoskentely.OverlappingTyoskentelyjaksoValidationService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.web.rest.TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.ValidationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.security.Principal
import java.time.LocalDate

private const val TYOSKENTELYJAKSO_ENTITY_NAME = "tyoskentelyjakso"
private const val KESKEYTYSAIKA_ENTITY_NAME = "keskeytysaika"
private const val ASIAKIRJA_ENTITY_NAME = "asiakirja"
private const val TYOSKENTELYPAIKKA_ENTITY_NAME = "tyoskentelypaikka"

@Suppress("TooManyFunctions")
@Component
class TyoskentelyjaksoResourceSupport(
    private val fileValidationService: FileValidationService,
    private val overlappingTyoskentelyjaksoValidationService: OverlappingTyoskentelyjaksoValidationService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val opintooikeusService: OpintooikeusService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {

    fun getMappedFiles(
        files: List<MultipartFile>?,
        opintooikeusId: Long
    ): MutableSet<AsiakirjaDTO>? {
        files?.let {
            if (!fileValidationService.validate(it, opintooikeusId)) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ASIAKIRJA_ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return it.map { file -> file.mapAsiakirja() }.toMutableSet()
        }

        return null
    }

    fun validateLaillistamispaivaAndTodistus(
        user: UserDTO,
        laillistamispaiva: LocalDate?,
        laillistamistodistus: MultipartFile?
    ) {
        if ((laillistamispaiva == null || laillistamistodistus == null) &&
            !erikoistuvaLaakariService.laillistamispaivaAndTodistusExists(user.id!!)
        ) {
            throw BadRequestAlertException(
                "Laillistamispaiva ja todistus vaaditaan",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.laillistamispaiva-ja-todistus-vaaditaan"
            )
        }
    }

    fun validateNewTyoskentelyjaksoDTO(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idexists"
            )
        }
        if (tyoskentelyjaksoDTO.tyoskentelypaikka == null || tyoskentelyjaksoDTO.tyoskentelypaikka!!.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä",
                TYOSKENTELYPAIKKA_ENTITY_NAME,
                "idexists"
            )
        }
    }

    fun validateTyoskentelyaika(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (!overlappingTyoskentelyjaksoValidationService.validateTyoskentelyjakso(opintooikeusId, tyoskentelyjaksoDTO)) {
            throwOverlappingTyoskentelyjaksotException()
        }
    }

    fun validateAlkamisJaPaattymispaiva(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        tyoskentelyjaksoDTO.paattymispaiva?.isBefore(tyoskentelyjaksoDTO.alkamispaiva)?.let {
            if (it) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    TYOSKENTELYPAIKKA_ENTITY_NAME,
                    "dataillegal.tyoskentelyjakson-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
                )
            }
        }

        if (!tyoskentelyjaksoService.validateAlkamisJaPaattymispaiva(tyoskentelyjaksoDTO, opintooikeusId)) {
            throw BadRequestAlertException(
                "Työskentelyjakson alkamis- tai päättymispäivä ei ole kelvollinen.",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.tyoskentelyjakson-paattymispaiva-ei-ole-kelvollinen"
            )
        }
    }

    fun validateKeskeytysaikaDTO(keskeytysaikaDTO: KeskeytysaikaDTO) {
        if (keskeytysaikaDTO.alkamispaiva == null || keskeytysaikaDTO.paattymispaiva == null) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamis- ja päättymispäivä ovat pakollisia tietoja",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysaika-alkamis-ja-paattymispaiva-ovat-pakollisia-tietoja"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva!!.isAfter(keskeytysaikaDTO.paattymispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei saa olla ennen alkamisaikaa",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva!!.isBefore(keskeytysaikaDTO.tyoskentelyjakso!!.alkamispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamispäivä ei voi olla ennen työskentelyjakson alkamispäivää",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-alkamispaiva-ei-voi-olla-ennen-tyoskentelyjakson-alkamispaivaa"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso!!.paattymispaiva != null && keskeytysaikaDTO.paattymispaiva!!.isAfter(
                keskeytysaikaDTO.tyoskentelyjakso!!.paattymispaiva
            )
        ) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei voi olla työskentelyjakson päättymispäivän jälkeen",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-voi-olla-tyoskentelyjakson-paattymispaivan-jalkeen"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso == null) {
            throw BadRequestAlertException(
                "Keskeytysajan täytyy kohdistua työskentelyjaksoon",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-taytyy-kohdistua-tyoskentelyjaksoon"
            )
        }
    }

    fun validateMuokkausoikeudet(principal: Principal?, userId: String, entity: String, userDescription: String) {
        if ((principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
                .contains(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
        ) {
            val opintooikeus = opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)
            if (!opintooikeus.muokkausoikeudetVirkailijoilla) {
                throw BadRequestAlertException(
                    "Ei oikeuksia muokata $userDescription tietoja",
                    entity,
                    "dataillegal.ei-oikeuksia-muokata-erikoistujan-tietoja"
                )
            }
        }
    }

    fun throwOverlappingTyoskentelyjaksotException() {
        throw BadRequestAlertException(
            "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.paallekkaisten-tyoskentelyjaksojen-yhteenlaskettu-aika-ylittyy"
        )
    }

    fun liitettyTerveyskoulutusjaksoonException(e: ValidationException): BadRequestAlertException {
        return BadRequestAlertException(
            e.message ?: "Validaatiovirhe",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.terveyskeskuskoulutusjaksoon-liitettya-tyoskentelyjaksoa-ei-voi-paivittaa"
        )
    }
}

