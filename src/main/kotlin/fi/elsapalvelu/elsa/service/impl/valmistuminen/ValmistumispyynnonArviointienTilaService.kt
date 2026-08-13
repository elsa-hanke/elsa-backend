package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavaKokonaisuusRepository
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoArviointienTilaDTO
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValmistumispyynnonArviointienTilaService(
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository
) {

    fun haeArviointienTila(
        valmistumispyynto: Valmistumispyynto
    ): ValmistumispyyntoArviointienTilaDTO {
        val opintooikeus = valmistumispyynto.opintooikeus
        val arvioitavatKokonaisuudet =
            arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
            opintooikeus?.erikoisala?.id,
            opintooikeus?.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
        )
        val arvioinnit = suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(
            opintooikeus?.id.required()
        )
        val arvioitujenKokonaisuuksienIds = arvioinnit
            .flatMap { it.arvioitavatKokonaisuudet }
            .map { it.arvioitavaKokonaisuus?.id.required() }
            .distinct()

        return ValmistumispyyntoArviointienTilaDTO(
            hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour = arvioinnit
                .flatMap { it.arvioitavatKokonaisuudet }
                .filter { it.arviointiasteikonTaso != null }
                .any { it.arviointiasteikonTaso.required() < MINIMUM_ASSESSMENT },
            hasArvioitaviaKokonaisuuksiaWithoutArviointi = arvioitavatKokonaisuudet.any {
                it.id.required() !in arvioitujenKokonaisuuksienIds
            }
        )
    }

    private companion object {
        const val MINIMUM_ASSESSMENT = 4
    }
}
