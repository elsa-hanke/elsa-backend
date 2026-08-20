package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavaKokonaisuusWithArvioinnitDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiByKokonaisuusDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapper
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonArviointitietoService(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val kayttajaMapper: KayttajaMapper,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper,
    private val asiakirjaMapper: AsiakirjaMapper
) {

    fun haeKategoriat(
        opintooikeusId: Long,
        vainKorkeinArviointi: Boolean
    ): List<ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO> {
        var arvioinnit = suoritusarviointiRepository
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .flatMap { it.arvioitavatKokonaisuudet }
        if (vainKorkeinArviointi) {
            arvioinnit = arvioinnit.filter { it.arviointiasteikonTaso != null }
        }

        val arvioinnitKokonaisuuksittain = arvioinnit.groupBy { it.arvioitavaKokonaisuus }
        val kokonaisuudetKategorioittain = arvioinnitKokonaisuuksittain.keys
            .sortedBy { it?.kategoria?.nimi }
            .groupBy { it?.kategoria }

        return kokonaisuudetKategorioittain.map { (kategoria, kokonaisuudet) ->
            mapKategoria(
                kategoria,
                kokonaisuudet,
                arvioinnitKokonaisuuksittain,
                vainKorkeinArviointi
            )
        }
    }

    private fun mapKategoria(
        kategoria: ArvioitavanKokonaisuudenKategoria?,
        kokonaisuudet: List<ArvioitavaKokonaisuus?>,
        arvioinnitKokonaisuuksittain:
            Map<ArvioitavaKokonaisuus?, List<SuoritusarvioinninArvioitavaKokonaisuus>>,
        vainKorkeinArviointi: Boolean
    ) = ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO(
        id = kategoria?.id,
        nimi = kategoria?.nimi,
        nimiSv = kategoria?.nimiSv,
        jarjestysnumero = kategoria?.jarjestysnumero,
        arviointejaYhteensa = if (vainKorkeinArviointi) {
            kokonaisuudet.size
        } else {
            kokonaisuudet.sumOf { arvioinnitKokonaisuuksittain[it]?.size ?: 0 }
        },
        arvioitavatKokonaisuudet = kokonaisuudet.sortedBy { it?.nimi }.map { kokonaisuus ->
            mapKokonaisuus(
                kokonaisuus,
                arvioinnitKokonaisuuksittain[kokonaisuus],
                vainKorkeinArviointi
            )
        }
    )

    private fun mapKokonaisuus(
        kokonaisuus: ArvioitavaKokonaisuus?,
        arvioinnit: List<SuoritusarvioinninArvioitavaKokonaisuus>?,
        vainKorkeinArviointi: Boolean
    ) = ArvioitavaKokonaisuusWithArvioinnitDTO(
        id = kokonaisuus?.id,
        nimi = kokonaisuus?.nimi,
        nimiSv = kokonaisuus?.nimiSv,
        kuvaus = kokonaisuus?.kuvaus,
        kuvausSv = kokonaisuus?.kuvausSv,
        voimassaoloAlkaa = kokonaisuus?.voimassaoloAlkaa,
        voimassaoloLoppuu = kokonaisuus?.voimassaoloLoppuu,
        suoritusarvioinnit = if (vainKorkeinArviointi) {
            listOf(
                arvioinnit?.sortedByDescending {
                    it.suoritusarviointi?.tapahtumanAjankohta
                }?.maxByOrNull { it.arviointiasteikonTaso.required() }
            ).map { mapKorkeinArviointi(it) }
        } else {
            arvioinnit?.sortedByDescending {
                it.suoritusarviointi?.tapahtumanAjankohta
            }?.map { it.toDto() }
        }
    )

    private fun mapKorkeinArviointi(
        arviointi: SuoritusarvioinninArvioitavaKokonaisuus?
    ) = SuoritusarviointiByKokonaisuusDTO(
        id = arviointi?.suoritusarviointi?.id,
        tapahtumanAjankohta = arviointi?.suoritusarviointi?.tapahtumanAjankohta,
        arvioitavaTapahtuma = arviointi?.suoritusarviointi?.arvioitavaTapahtuma,
        arviointiasteikonTaso = arviointi?.arviointiasteikonTaso,
        arvioinninAntaja = kayttajaMapper.toDto(
            arviointi?.suoritusarviointi?.arvioinninAntaja.required()
        ),
        tyoskentelyjakso = tyoskentelyjaksoMapper.toDto(
            requireNotNull(arviointi).suoritusarviointi?.tyoskentelyjakso.required()
        )
    )

    private fun SuoritusarvioinninArvioitavaKokonaisuus.toDto() =
        SuoritusarviointiByKokonaisuusDTO(
            id = suoritusarviointi?.id,
            tapahtumanAjankohta = suoritusarviointi?.tapahtumanAjankohta,
            arvioitavaTapahtuma = suoritusarviointi?.arvioitavaTapahtuma,
            arviointiasteikonTaso = arviointiasteikonTaso,
            itsearviointiArviointiasteikonTaso = itsearviointiArviointiasteikonTaso,
            arvioinninAntaja = kayttajaMapper.toDto(
                suoritusarviointi?.arvioinninAntaja.required()
            ),
            arvioinninSaaja = kayttajaMapper.toDto(
                suoritusarviointi?.tyoskentelyjakso?.opintooikeus
                    ?.erikoistuvaLaakari?.kayttaja.required()
            ),
            tyoskentelyjakso = tyoskentelyjaksoMapper.toDto(
                suoritusarviointi?.tyoskentelyjakso.required()
            ),
            arviointiAsiakirjat = suoritusarviointi?.arviointiAsiakirjat?.map(asiakirjaMapper::toDto),
            itsearviointiAsiakirjat =
            suoritusarviointi?.itsearviointiAsiakirjat?.map(asiakirjaMapper::toDto)
        )
}
