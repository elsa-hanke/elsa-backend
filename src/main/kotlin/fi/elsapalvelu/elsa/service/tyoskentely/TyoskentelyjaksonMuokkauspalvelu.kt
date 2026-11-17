package fi.elsapalvelu.elsa.service.tyoskentely

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

private const val TYOSKENTELYJAKSO = "tyoskentelyjakso"

@Service
class TyoskentelyjaksonMuokkauspalvelu(
    private val tyoskentelyjaksot: TyoskentelyjaksoService,
    private val tarkistukset: TyoskentelyjaksonTarkistuspalvelu,
    private val objectMapper: ObjectMapper
) {
    fun luo(jakso: TyoskentelyjaksoDTO, opintooikeusId: Long, liitteet: List<MultipartFile>?): TyoskentelyjaksoDTO {
        tarkistukset.tarkistaUusiTyoskentelyjakso(jakso)
        tarkistaPaivatJaTyoskentelyaika(jakso, opintooikeusId)
        val asiakirjat = tarkistukset.muodostaAsiakirjat(liitteet, opintooikeusId) ?: mutableSetOf()
        return tyoskentelyjaksot.create(jakso, opintooikeusId, asiakirjat)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    fun paivita(
        jakso: TyoskentelyjaksoDTO,
        opintooikeusId: Long,
        liitteet: List<MultipartFile>?,
        poistettavatAsiakirjatJson: String?
    ): TyoskentelyjaksoDTO {
        if (jakso.id == null) {
            throw BadRequestAlertException("Työskentelyjakson ID puuttuu.", TYOSKENTELYJAKSO, "idnull")
        }
        tarkistaPaivatJaTyoskentelyaika(jakso, opintooikeusId)
        val asiakirjat = tarkistukset.muodostaAsiakirjat(liitteet, opintooikeusId) ?: mutableSetOf()
        val poistettavatAsiakirjat = poistettavatAsiakirjatJson?.let { objectMapper.readValue(it, mutableSetOf<Int>()::class.java) }
        return muunnaMuokkausvirhe {
            tyoskentelyjaksot.update(jakso, opintooikeusId, asiakirjat, poistettavatAsiakirjat)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

    fun paivitaAsiakirjat(
        jaksoId: Long,
        opintooikeusId: Long,
        liitteet: List<MultipartFile>?,
        poistettavatAsiakirjat: List<Int>?
    ): TyoskentelyjaksoDTO = muunnaMuokkausvirhe {
        tyoskentelyjaksot.updateAsiakirjat(
            jaksoId, opintooikeusId,
            tarkistukset.muodostaAsiakirjat(liitteet, opintooikeusId), poistettavatAsiakirjat?.toSet()
        ) ?: throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }

    fun poista(jaksoId: Long, opintooikeusId: Long) {
        if (!tyoskentelyjaksot.delete(jaksoId, opintooikeusId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

    fun paivitaKoejaksokytkenta(jakso: TyoskentelyjaksoDTO, opintooikeusId: Long): TyoskentelyjaksoDTO {
        if (jakso.id == null) {
            throw BadRequestAlertException("Virheellinen id", TYOSKENTELYJAKSO, "idnull")
        }
        if (jakso.liitettyKoejaksoon == null) {
            throw BadRequestAlertException(
                "Liitetty koejaksoon on pakollinen tieto", TYOSKENTELYJAKSO,
                "dataillegal.liitetty-koejaksoon-on-pakollinen-tieto"
            )
        }
        return tyoskentelyjaksot.updateLiitettyKoejaksoon(jakso.id.required(), opintooikeusId, jakso.liitettyKoejaksoon.required())
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    private fun tarkistaPaivatJaTyoskentelyaika(jakso: TyoskentelyjaksoDTO, opintooikeusId: Long) {
        tarkistukset.tarkistaAlkamisJaPaattymispaiva(opintooikeusId, jakso)
        tarkistukset.tarkistaTyoskentelyaika(opintooikeusId, jakso)
    }

    private fun <T> muunnaMuokkausvirhe(toiminto: () -> T): T = try {
        toiminto()
    } catch (virhe: ValidationException) {
        throw tarkistukset.terveyskeskuskoulutusjaksonMuokkausvirhe(virhe)
    }
}
