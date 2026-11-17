package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class KeskeytysajanMuokkauspalvelu(
    private val keskeytysajat: KeskeytysaikaService,
    private val poissaolojenTarkistus: OverlappingKeskeytysaikaValidationService,
    private val tyoskentelyajanTarkistus: OverlappingTyoskentelyjaksoValidationService,
    private val tarkistukset: TyoskentelyjaksonTarkistuspalvelu
) {
    fun tarkistaUudenKeskeytysajanTunniste(keskeytysaika: KeskeytysaikaDTO) {
        if (keskeytysaika.id != null) {
            throw BadRequestAlertException("Uusi keskeytysaika ei saa sisältää ID:tä", "keskeytysaika", "idexists")
        }
    }

    fun tarkistaPaivitettavanKeskeytysajanTunniste(keskeytysaika: KeskeytysaikaDTO) {
        if (keskeytysaika.id == null) {
            throw BadRequestAlertException("Virheellinen id", "tyoskentelyjakso", "idnull")
        }
    }

    fun luo(keskeytysaika: KeskeytysaikaDTO, opintooikeusId: Long): KeskeytysaikaDTO {
        tarkistaPaallekkaisetPoissaolot(keskeytysaika, opintooikeusId)
        return tallenna(keskeytysaika, opintooikeusId)
    }

    fun paivita(keskeytysaika: KeskeytysaikaDTO, opintooikeusId: Long): KeskeytysaikaDTO {
        tarkistaPaallekkaisetPoissaolot(keskeytysaika, opintooikeusId)
        if (!tyoskentelyajanTarkistus.validateKeskeytysaika(opintooikeusId, keskeytysaika)) {
            tarkistukset.hylkaaPaallekkaisetTyoskentelyjaksot()
        }
        return tallenna(keskeytysaika, opintooikeusId)
    }

    fun poista(keskeytysaikaId: Long, opintooikeusId: Long) {
        if (!tyoskentelyajanTarkistus.validateKeskeytysaikaDelete(opintooikeusId, keskeytysaikaId)) {
            tarkistukset.hylkaaPaallekkaisetTyoskentelyjaksot()
        }
        try {
            keskeytysajat.delete(keskeytysaikaId, opintooikeusId)
        } catch (virhe: ValidationException) {
            throw tarkistukset.terveyskeskuskoulutusjaksonMuokkausvirhe(virhe)
        }
    }

    private fun tarkistaPaallekkaisetPoissaolot(keskeytysaika: KeskeytysaikaDTO, opintooikeusId: Long) {
        if (!poissaolojenTarkistus.validateKeskeytysaika(opintooikeusId, keskeytysaika)) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a", "keskeytysaika",
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }
    }

    private fun tallenna(keskeytysaika: KeskeytysaikaDTO, opintooikeusId: Long): KeskeytysaikaDTO = try {
        keskeytysajat.save(keskeytysaika, opintooikeusId) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    } catch (virhe: ValidationException) {
        throw tarkistukset.terveyskeskuskoulutusjaksonMuokkausvirhe(virhe)
    }
}
