package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.required

import java.security.Principal
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.kayttaja.FileValidationService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.ValidationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

private const val TYOSKENTELYJAKSO_ENTITY_NAME = "tyoskentelyjakso"
private const val KESKEYTYSAIKA_ENTITY_NAME = "keskeytysaika"
private const val TYOSKENTELYPAIKKA_ENTITY_NAME = "tyoskentelypaikka"

@Component
class TyoskentelyjaksonTarkistuspalvelu(
    private val tiedostojenTarkistus: FileValidationService,
    private val tyoskentelyajanTarkistus: OverlappingTyoskentelyjaksoValidationService,
    private val tyoskentelyjaksot: TyoskentelyjaksoService,
    private val opintooikeudet: OpintooikeusService
) {

    fun muodostaAsiakirjat(
        liitteet: List<MultipartFile>?,
        opintooikeusId: Long
    ): MutableSet<AsiakirjaDTO>? {
        liitteet?.let {
            tiedostojenTarkistus.validate(it, opintooikeusId)
            return it.map { liite -> liite.mapAsiakirja() }.toMutableSet()
        }

        return null
    }

    fun tarkistaUusiTyoskentelyjakso(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idexists"
            )
        }
        if (tyoskentelyjaksoDTO.tyoskentelypaikka == null || tyoskentelyjaksoDTO.tyoskentelypaikka.required().id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä",
                TYOSKENTELYPAIKKA_ENTITY_NAME,
                "idexists"
            )
        }
    }

    fun tarkistaTyoskentelyaika(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (!tyoskentelyajanTarkistus.validateTyoskentelyjakso(opintooikeusId, tyoskentelyjaksoDTO)) {
            hylkaaPaallekkaisetTyoskentelyjaksot()
        }
    }

    fun tarkistaAlkamisJaPaattymispaiva(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        tyoskentelyjaksoDTO.paattymispaiva?.isBefore(tyoskentelyjaksoDTO.alkamispaiva)?.let {
            if (it) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    TYOSKENTELYPAIKKA_ENTITY_NAME,
                    "dataillegal.tyoskentelyjakson-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
                )
            }
        }

        if (!tyoskentelyjaksot.validateAlkamisJaPaattymispaiva(tyoskentelyjaksoDTO, opintooikeusId)) {
            throw BadRequestAlertException(
                "Työskentelyjakson alkamis- tai päättymispäivä ei ole kelvollinen.",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.tyoskentelyjakson-paattymispaiva-ei-ole-kelvollinen"
            )
        }
    }

    fun tarkistaKeskeytysaika(keskeytysaikaDTO: KeskeytysaikaDTO) {
        if (keskeytysaikaDTO.alkamispaiva == null || keskeytysaikaDTO.paattymispaiva == null) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamis- ja päättymispäivä ovat pakollisia tietoja",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysaika-alkamis-ja-paattymispaiva-ovat-pakollisia-tietoja"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva.required().isAfter(keskeytysaikaDTO.paattymispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei saa olla ennen alkamisaikaa",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva.required().isBefore(keskeytysaikaDTO.tyoskentelyjakso.required().alkamispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamispäivä ei voi olla ennen työskentelyjakson alkamispäivää",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-alkamispaiva-ei-voi-olla-ennen-tyoskentelyjakson-alkamispaivaa"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso.required().paattymispaiva != null && keskeytysaikaDTO.paattymispaiva.required().isAfter(
                keskeytysaikaDTO.tyoskentelyjakso.required().paattymispaiva
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

    fun tarkistaMuokkausoikeudet(principal: Principal?, kayttajaId: String, kohde: String, kayttajanKuvaus: String) {
        if ((principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
                .contains(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
        ) {
            val opintooikeus = opintooikeudet.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(kayttajaId)
            if (!opintooikeus.muokkausoikeudetVirkailijoilla) {
                throw BadRequestAlertException(
                    "Ei oikeuksia muokata $kayttajanKuvaus tietoja",
                    kohde,
                    "dataillegal.ei-oikeuksia-muokata-erikoistujan-tietoja"
                )
            }
        }
    }

    fun hylkaaPaallekkaisetTyoskentelyjaksot() {
        throw BadRequestAlertException(
            "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.paallekkaisten-tyoskentelyjaksojen-yhteenlaskettu-aika-ylittyy"
        )
    }

    fun terveyskeskuskoulutusjaksonMuokkausvirhe(virhe: ValidationException): BadRequestAlertException {
        return BadRequestAlertException(
            virhe.message ?: "Validaatiovirhe",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.terveyskeskuskoulutusjaksoon-liitettya-tyoskentelyjaksoa-ei-voi-paivittaa"
        )
    }
}

