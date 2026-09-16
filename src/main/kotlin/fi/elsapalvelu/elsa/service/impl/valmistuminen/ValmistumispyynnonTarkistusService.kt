package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusRepository
import fi.elsapalvelu.elsa.repository.koulutus.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.constants.ERIKOISALA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotKoulutustyypitDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoSuoritustenTilaDTO
import fi.elsapalvelu.elsa.service.mapper.koulutus.OpintosuoritusMapper
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

private val YEK_OPINTOSUORITUSTYYPIT = setOf(
    OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS,
    OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO,
    OpintosuoritusTyyppiEnum.YEK_PATEVYYS
)

@Component
class ValmistumispyynnonTarkistusService(
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository:
        TerveyskeskuskoulutusjaksonHyvaksyntaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val opintosuoritusMapper: OpintosuoritusMapper,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val vanhentumisService: ValmistumispyynnonVanhentumisService,
    private val pdfTextFieldValidator: PdfTextFieldValidator
) {

    fun validateValmistumispyyntoPdfText(
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ) {
        pdfTextFieldValidator.validate(
            fields = listOf(
                "selvitys-vanhentuneista-suorituksista-otsikko" to
                    uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista
            ),
            pdfSource = "valmistumispyynto"
        )
    }

    fun validateVirkailijanPdfText(
        id: Long,
        tarkistusDTO: ValmistumispyynnonTarkistusUpdateDTO
    ) {
        if (tarkistusDTO.korjausehdotus != null) return

        pdfTextFieldValidator.validate(
            fields = listOf(
                "virkailijan-valmistumisen-yhteenveto" to tarkistusDTO.virkailijanYhteenveto,
                "lisatiedot-vastuuhenkilolle" to tarkistusDTO.lisatiedotVastuuhenkilolle
            ),
            pdfSource = "valmistumispyynto",
            sourceId = id
        )
    }

    fun taydenna(dto: ValmistumispyynnonTarkistusDTO): ValmistumispyynnonTarkistusDTO {
        val opintooikeusId = dto.valmistumispyynto?.opintooikeusId ?: return dto
        val opintooikeus = haeOpintooikeus(opintooikeusId)
        val opintosuoritukset = haeOlennaisetOpintosuoritukset(opintooikeus)

        lisaaTyoskentelytiedot(dto, opintooikeus, opintosuoritukset)
        lisaaOpintosuoritustiedot(dto, opintooikeus, opintosuoritukset)
        lisaaKoejaksotiedot(dto, opintooikeusId, opintosuoritukset)
        lisaaVanhentumistiedot(dto, opintooikeus)
        return dto
    }

    private fun lisaaTyoskentelytiedot(
        dto: ValmistumispyynnonTarkistusDTO,
        opintooikeus: Opintooikeus,
        opintosuoritukset: OlennaisetOpintosuoritukset
    ) {
        val id = opintooikeus.id.required()
        dto.tyoskentelyjaksotTilastot = tyoskentelyjaksoService.getTilastot(id).koulutustyypit
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByOpintooikeusId(id)
            ?.let { approval ->
                if (approval.vastuuhenkiloHyvaksynyt) {
                    dto.terveyskeskustyoHyvaksyttyPvm = approval.vastuuhenkilonKuittausaika
                }
                dto.terveyskeskustyoHyvaksyntaId = approval.id
            }

        (opintosuoritukset.nykyiset + opintosuoritukset.yek)
            .firstOrNull {
                it.tyyppi?.nimi in setOf(
                    OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO,
                    OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO
                )
            }
            ?.let { dto.terveyskeskustyoOpintosuoritusId = it.id }

        dto.tutkimustyotaTehty = tyoskentelyjaksoService.existsByKaytannonKoulutus(
            id,
            KaytannonKoulutusTyyppi.TUTKIMUSTYO
        )
        if (opintooikeus.onYek()) {
            val tyoskentelyjaksot =
                tyoskentelyjaksoService.findAllByOpintooikeusIdWithKeskeytykset(id)
            dto.tyoskentelyjaksot = TyoskentelyjaksotKoulutustyypitDTO(
                terveyskeskus = tyoskentelyjaksot.filter {
                    it.tyoskentelypaikka?.tyyppi == TyoskentelyjaksoTyyppi.TERVEYSKESKUS
                },
                yliopistosairaala = tyoskentelyjaksot.filter {
                    it.tyoskentelypaikka?.tyyppi in setOf(
                        TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA,
                        TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
                    )
                },
                yliopistosairaaloidenUlkopuolinen = tyoskentelyjaksot.filter {
                    it.tyoskentelypaikka?.tyyppi in setOf(
                        TyoskentelyjaksoTyyppi.YKSITYINEN,
                        TyoskentelyjaksoTyyppi.MUU
                    )
                }
            )
        }
    }

    private fun lisaaOpintosuoritustiedot(
        dto: ValmistumispyynnonTarkistusDTO,
        opintooikeus: Opintooikeus,
        opintosuoritukset: OlennaisetOpintosuoritukset
    ) {
        opintosuoritukset.yek
            .firstOrNull { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_PATEVYYS }
            ?.let {
                dto.yekSuoritettu = true
                dto.yekSuorituspaiva = it.suorituspaiva
            }

        val opintoopas = opintooikeus.opintoopas
        dto.teoriakoulutusSuoritettu = if (opintooikeus.onYek()) {
            if (opintosuoritukset.nykyiset.any {
                    it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS
                }
            ) {
                opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
            } else {
                0.0
            }
        } else {
            teoriakoulutusRepository.findAllByOpintooikeusId(opintooikeus.id.required())
                .mapNotNull { it.erikoistumiseenHyvaksyttavaTuntimaara }
                .sum()
        }
        dto.teoriakoulutusVaadittu =
            opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
        dto.sateilusuojakoulutusSuoritettu = opintosuoritukset.nykyiset
            .filter { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS }
            .mapNotNull { it.opintopisteet }
            .sum()
        dto.sateilusuojakoulutusVaadittu =
            opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara
        dto.johtamiskoulutusSuoritettu = opintosuoritukset.nykyiset
            .filter { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO }
            .mapNotNull { it.opintopisteet }
            .sum()
        dto.johtamiskoulutusVaadittu =
            opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara
        dto.kuulustelut = opintosuoritukset.nykyiset
            .filter { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU }
            .map(opintosuoritusMapper::toDto)
    }

    private fun lisaaKoejaksotiedot(
        dto: ValmistumispyynnonTarkistusDTO,
        opintooikeusId: Long,
        opintosuoritukset: OlennaisetOpintosuoritukset
    ) {
        opintosuoritukset.nykyiset.firstOrNull {
            it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.KOEJAKSO && it.hyvaksytty
        }?.let { dto.koejaksoHyvaksyttyPvm = it.suorituspaiva }
        koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeusId)
            .orElse(null)
            ?.takeIf { it.vastuuhenkiloHyvaksynyt }
            ?.let { dto.koejaksoHyvaksyttyPvm = it.vastuuhenkilonKuittausaika }
    }

    private fun lisaaVanhentumistiedot(
        dto: ValmistumispyynnonTarkistusDTO,
        opintooikeus: Opintooikeus
    ) {
        val erikoisalaTyyppi = opintooikeus.erikoisala?.tyyppi
            ?: throw EntityNotFoundException(ERIKOISALA_NOT_FOUND_ERROR)
        val vanhentuneetSuoritukset = vanhentumisService.haeVanhentuneetSuoritukset(
            opintooikeus.id.required(),
            erikoisalaTyyppi
        )
        dto.suoritustenTila = ValmistumispyyntoSuoritustenTilaDTO(
            erikoisalaTyyppi = erikoisalaTyyppi,
            vanhojaTyoskentelyjaksojaOrSuorituksiaExists =
                vanhentuneetSuoritukset.vanhojaTyoskentelyjaksojaOrSuorituksiaExists,
            kuulusteluVanhentunut = vanhentuneetSuoritukset.kuulusteluVanhentunut
        )
    }

    private fun haeOlennaisetOpintosuoritukset(
        opintooikeus: Opintooikeus
    ): OlennaisetOpintosuoritukset {
        val nykyisetOpintosuoritukset = opintosuoritusRepository
            .findAllByOpintooikeusId(opintooikeus.id.required())
            .filter {
                if (opintooikeus.onYek()) {
                    it.tyyppi?.nimi in YEK_OPINTOSUORITUSTYYPIT
                } else {
                    it.tyyppi?.nimi !in YEK_OPINTOSUORITUSTYYPIT
                }
            }
        val yekOpintosuoritukset = if (opintooikeus.onYek()) {
            emptyList()
        } else {
            opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
                opintooikeus.erikoistuvaLaakari?.id.required(),
                YEK_ERIKOISALA_ID
            )
        }
        return OlennaisetOpintosuoritukset(nykyisetOpintosuoritukset, yekOpintosuoritukset)
    }

    private fun haeOpintooikeus(id: Long) = opintooikeusRepository.findByIdOrNull(id)
        ?: throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)

    private fun Opintooikeus.onYek() = erikoisala?.id == YEK_ERIKOISALA_ID

    private data class OlennaisetOpintosuoritukset(
        val nykyiset: List<Opintosuoritus>,
        val yek: List<Opintosuoritus>
    )
}
