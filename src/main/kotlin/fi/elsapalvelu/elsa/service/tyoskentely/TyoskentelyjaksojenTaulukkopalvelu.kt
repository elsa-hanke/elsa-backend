package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotTableDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.perustiedot.ErikoisalaService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import org.springframework.stereotype.Service

@Service
class TyoskentelyjaksojenTaulukkopalvelu(
    private val tyoskentelyjaksot: TyoskentelyjaksoService,
    private val keskeytysajat: KeskeytysaikaService,
    private val erikoistujat: ErikoistuvaLaakariService,
    private val erikoisalat: ErikoisalaService,
    private val opintosuoritukset: OpintosuoritusService,
    private val hyvaksynnat: TerveyskeskuskoulutusjaksonHyvaksyntaService
) {
    fun haeErikoistujanTaulukko(kayttajaId: String, opintooikeusId: Long, erikoisalaId: Long?): TyoskentelyjaksotTableDTO {
        val taulukko = haePerustiedot(opintooikeusId)
        val erikoistujaId = erikoistujat.findOneByKayttajaUserId(kayttajaId)?.id.required()
        val erikoisala = erikoisalaId?.let { erikoisalat.findOne(it).orElse(null) }

        // Hammaslääketieteessä terveyskeskuskoulutusjaksoa ei tarvitse suorittaa.
        if (erikoisala?.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE) {
            taulukko.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY
            taulukko.terveyskeskuskoulutusjaksonKorjausehdotus = ErikoisalaTyyppi.HAMMASLAAKETIEDE.toString()
            return taulukko
        }

        return taydennaTerveyskeskuskoulutuksenTila(taulukko, opintooikeusId, erikoistujaId) {
            hyvaksynnat.getTerveyskoulutusjaksoSuoritettu(opintooikeusId)
        }
    }

    fun haeYekTaulukko(kayttajaId: String, opintooikeusId: Long): TyoskentelyjaksotTableDTO {
        val taulukko = haePerustiedot(opintooikeusId)
        val erikoistujaId = erikoistujat.findOneByKayttajaUserId(kayttajaId)?.id.required()
        return taydennaTerveyskeskuskoulutuksenTila(taulukko, opintooikeusId, erikoistujaId) {
            hyvaksynnat.getTerveyskoulutusjaksoSuoritettuYek(opintooikeusId)
        }
    }

    private fun haePerustiedot(opintooikeusId: Long): TyoskentelyjaksotTableDTO {
        val taulukko = TyoskentelyjaksotTableDTO()
        taulukko.tyoskentelyjaksot = tyoskentelyjaksot.findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        taulukko.keskeytykset = keskeytysajat.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).toMutableSet()
        taulukko.tilastot = tyoskentelyjaksot.getTilastot(opintooikeusId)
        return taulukko
    }

    private fun taydennaTerveyskeskuskoulutuksenTila(
        taulukko: TyoskentelyjaksotTableDTO,
        opintooikeusId: Long,
        erikoistujaId: Long,
        onkoKoulutusjaksoSuoritettu: () -> Boolean
    ): TyoskentelyjaksotTableDTO {
        val suorituspaiva = opintosuoritukset.getTerveyskoulutusjaksoSuoritusPvm(opintooikeusId, erikoistujaId)
        if (suorituspaiva != null) {
            taulukko.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY
            taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm = suorituspaiva
            return taulukko
        }

        val hyvaksynta = hyvaksynnat.findByOpintooikeusId(opintooikeusId)
        if (hyvaksynta != null) {
            taulukko.terveyskeskuskoulutusjaksonTila = hyvaksynta.tila
            taulukko.terveyskeskuskoulutusjaksonKorjausehdotus = hyvaksynta.virkailijanKorjausehdotus ?: hyvaksynta.vastuuhenkilonKorjausehdotus
            taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm = hyvaksynta.vastuuhenkilonKuittausaika
        } else if (onkoKoulutusjaksoSuoritettu()) {
            taulukko.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.UUSI
        }
        return taulukko
    }
}
