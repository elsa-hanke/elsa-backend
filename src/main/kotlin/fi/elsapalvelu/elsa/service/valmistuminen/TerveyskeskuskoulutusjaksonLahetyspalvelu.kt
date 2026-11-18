package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.web.rest.TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/** Kokoaa lähetyksen vaiheet säilyttäen taustapalveluiden nykyiset transaktiorajat. */
@Service
class TerveyskeskuskoulutusjaksonLahetyspalvelu(
    private val erikoistujat: ErikoistuvaLaakariService,
    private val opintosuoritukset: OpintosuoritusService,
    private val hyvaksyntapalvelu: TerveyskeskuskoulutusjaksonHyvaksyntaService
) {
    fun lahetaErikoistujanHyvaksynta(
        kayttajaId: String, opintooikeusId: Long, laillistamispaiva: LocalDate?, laillistamistodistus: MultipartFile?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        val erikoistujaId = erikoistujat.findOneByKayttajaUserId(kayttajaId)?.id.required()
        if (opintosuoritukset.getTerveyskoulutusjaksoSuoritettu(opintooikeusId, erikoistujaId)) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksynnästä on jo opintosuoritus",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME, "dataillegal.terveyskeskuskoulutusjaksolla-on-opintosuoritus"
            )
        }
        tarkistaKoulutusjaksonPituus(hyvaksyntapalvelu.getTerveyskoulutusjaksoSuoritettu(opintooikeusId))
        return luoHyvaksynta(kayttajaId, opintooikeusId, laillistamispaiva, laillistamistodistus)
    }

    fun lahetaYekHyvaksynta(
        kayttajaId: String, opintooikeusId: Long, laillistamispaiva: LocalDate?, laillistamistodistus: MultipartFile?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        tarkistaKoulutusjaksonPituus(hyvaksyntapalvelu.getTerveyskoulutusjaksoSuoritettuYek(opintooikeusId))
        return luoHyvaksynta(kayttajaId, opintooikeusId, laillistamispaiva, laillistamistodistus)
    }

    fun lahetaUudelleen(
        kayttajaId: String, opintooikeusId: Long, laillistamispaiva: LocalDate?, laillistamistodistus: MultipartFile?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        val hyvaksynta = hyvaksyntapalvelu.findByOpintooikeusId(opintooikeusId)
            ?: throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksyntää ei löydy",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME, "dataillegal.terveyskeskuskoulutusjakson-hyvaksyntaa-ei-loydy"
            )
        paivitaLaillistaminen(kayttajaId, laillistamispaiva, laillistamistodistus)
        return hyvaksyntapalvelu.update(kayttajaId, false, hyvaksynta.id.required(), null, null)
    }

    private fun tarkistaKoulutusjaksonPituus(riittavaPituus: Boolean) {
        if (!riittavaPituus) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksyntään vaadittava työskentelyaika ei täyty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME, "dataillegal.terveyskeskuskoulutusjakson-pituus-liian-pieni"
            )
        }
    }

    private fun luoHyvaksynta(
        kayttajaId: String, opintooikeusId: Long, laillistamispaiva: LocalDate?, laillistamistodistus: MultipartFile?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        tarkistaLaillistaminen(kayttajaId, laillistamispaiva, laillistamistodistus)
        if (hyvaksyntapalvelu.existsByOpintooikeusId(opintooikeusId)) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksyntä on jo lähetetty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME, "dataillegal.terveyskeskuskoulutusjakson-hyvaksynta-on-jo-lahetetty"
            )
        }
        paivitaLaillistaminen(kayttajaId, laillistamispaiva, laillistamistodistus)
        return hyvaksyntapalvelu.create(opintooikeusId)
    }

    private fun tarkistaLaillistaminen(kayttajaId: String, laillistamispaiva: LocalDate?, laillistamistodistus: MultipartFile?) {
        if ((laillistamispaiva == null || laillistamistodistus == null) && !erikoistujat.laillistamispaivaAndTodistusExists(kayttajaId)) {
            throw BadRequestAlertException(
                "Laillistamispaiva ja todistus vaaditaan",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME, "dataillegal.laillistamispaiva-ja-todistus-vaaditaan"
            )
        }
    }

    private fun paivitaLaillistaminen(kayttajaId: String, laillistamispaiva: LocalDate?, laillistamistodistus: MultipartFile?) {
        erikoistujat.updateLaillistamispaiva(
            kayttajaId, laillistamispaiva, laillistamistodistus?.bytes,
            laillistamistodistus?.originalFilename, laillistamistodistus?.contentType
        )
    }
}
