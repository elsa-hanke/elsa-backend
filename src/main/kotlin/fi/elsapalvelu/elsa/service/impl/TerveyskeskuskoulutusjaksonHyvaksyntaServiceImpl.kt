package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.constants.VASTUUHENKILO_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.TerveyskeskuskoulutusjaksonHyvaksyntaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoWithKeskeytysajatMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@Service
@Transactional
class TerveyskeskuskoulutusjaksonHyvaksyntaServiceImpl(
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val terveyskeskuskoulutusjaksonHyvaksyntaMapper: TerveyskeskuskoulutusjaksonHyvaksyntaMapper,
    private val kayttajaMapper: KayttajaMapper,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoWithKeskeytysajatMapper,
    private val tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService

) : TerveyskeskuskoulutusjaksonHyvaksyntaService {

    override fun findById(id: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.findById(id).orElse(null)?.let {
            return mapTerveyskeskuskoulutusjakso(it)
        }
        return null
    }

    override fun findByOpintooikeusId(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByOpintooikeusId(opintooikeusId)?.let {
            return mapTerveyskeskuskoulutusjakso(it)
        }
        return null
    }

    override fun findByOpintooikeusIdOrCreateNew(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        findByOpintooikeusId(opintooikeusId)?.let {
            return it
        }

        opintooikeusRepository.findById(opintooikeusId).orElse(null)?.let {
            val tyoskentelyjaksot =
                tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppi(
                    opintooikeusId,
                    TyoskentelyjaksoTyyppi.TERVEYSKESKUS
                )
            val suoritettuPituus = getKokonaispituus(tyoskentelyjaksot)

            if (suoritettuPituus < it.opintoopas!!.terveyskeskuskoulutusjaksonVahimmaispituus!!) {
                throw ValidationException()
            }

            val vastuuhenkilo =
                kayttajaRepository.findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
                    listOf(VASTUUHENKILO),
                    it.yliopisto?.id,
                    VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN
                )
                    ?.let { v ->
                        kayttajaMapper.toDto(v)
                    } ?: throw EntityNotFoundException(VASTUUHENKILO_NOT_FOUND_ERROR)

            return TerveyskeskuskoulutusjaksonHyvaksyntaDTO(
                erikoistuvanAvatar = it.erikoistuvaLaakari?.kayttaja?.getAvatar(),
                erikoistuvanErikoisala = it.erikoistuvaLaakari?.getErikoisalaNimi(),
                erikoistuvanNimi = it.erikoistuvaLaakari?.kayttaja?.getNimi(),
                erikoistuvanOpiskelijatunnus = it.opiskelijatunnus,
                erikoistuvanSyntymaaika = it.erikoistuvaLaakari?.syntymaaika,
                erikoistuvanYliopisto = it.yliopisto?.nimi,
                laillistamispaiva = it.erikoistuvaLaakari?.laillistamispaiva,
                laillistamispaivanLiite = it.erikoistuvaLaakari?.laillistamispaivanLiitetiedosto,
                laillistamispaivanLiitteenNimi = it.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi,
                laillistamispaivanLiitteenTyyppi = it.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi,
                asetus = it.asetus?.nimi,
                terveyskeskuskoulutusjaksonKesto = suoritettuPituus,
                tyoskentelyjaksot = tyoskentelyjaksot.map(tyoskentelyjaksoMapper::toDto),
                vastuuhenkilonNimi = vastuuhenkilo.nimi,
                vastuuhenkilonNimike = vastuuhenkilo.nimike
            )
        }
        return null
    }

    override fun getTerveyskoulutusjaksoSuoritettu(opintooikeus: Opintooikeus): Boolean {
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppi(
                opintooikeus.id!!,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS
            )
        val suoritettuPituus = getKokonaispituus(tyoskentelyjaksot)

        return suoritettuPituus >= opintooikeus.opintoopas!!.terveyskeskuskoulutusjaksonVahimmaispituus!!
    }

    override fun create(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            val hyvaksynta = TerveyskeskuskoulutusjaksonHyvaksynta(opintooikeus = it)
            terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(hyvaksynta)
            return terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(hyvaksynta)
        }

        return null
    }

    override fun update(
        userId: String,
        isVirkailija: Boolean,
        terveyskeskuskoulutusjaksoDTO: TerveyskeskuskoulutusjaksonHyvaksyntaDTO
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        val kayttaja = kayttajaRepository.findOneByUserId(userId)
        return terveyskeskuskoulutusjaksonHyvaksyntaRepository.findById(
            terveyskeskuskoulutusjaksoDTO.id!!
        )
            .orElse(null)?.let {
                if (isVirkailija) {
                    if (terveyskeskuskoulutusjaksoDTO.korjausehdotus != null) {
                        it.korjausehdotus = terveyskeskuskoulutusjaksoDTO.korjausehdotus
                    } else {
                        it.virkailijaHyvaksynyt = true
                    }
                } else if (kayttajaRepository.findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
                        listOf(VASTUUHENKILO),
                        it.opintooikeus?.yliopisto?.id,
                        VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN
                    )?.id == kayttaja.orElse(null)?.id
                ) {
                    if (terveyskeskuskoulutusjaksoDTO.korjausehdotus != null) {
                        it.korjausehdotus = terveyskeskuskoulutusjaksoDTO.korjausehdotus
                    } else {
                        it.vastuuhenkiloHyvaksynyt = true
                    }
                }
                val result = terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(it)
                return terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(result)
            }
    }

    override fun existsByOpintooikeusId(opintooikeusId: Long): Boolean {
        return terveyskeskuskoulutusjaksonHyvaksyntaRepository.existsByOpintooikeusId(opintooikeusId)
    }

    private fun mapTerveyskeskuskoulutusjakso(hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta): TerveyskeskuskoulutusjaksonHyvaksyntaDTO {
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppi(
                hyvaksynta.opintooikeus?.id!!,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS
            )
        val suoritettuPituus = getKokonaispituus(tyoskentelyjaksot)

        if (suoritettuPituus < hyvaksynta.opintooikeus?.opintoopas!!.terveyskeskuskoulutusjaksonVahimmaispituus!!) {
            throw ValidationException()
        }

        val vastuuhenkilo =
            kayttajaRepository.findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                hyvaksynta.opintooikeus?.yliopisto?.id,
                VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN
            )
                ?.let { v ->
                    kayttajaMapper.toDto(v)
                } ?: throw EntityNotFoundException(VASTUUHENKILO_NOT_FOUND_ERROR)

        val result = terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(hyvaksynta)
        result.terveyskeskuskoulutusjaksonKesto = suoritettuPituus
        result.vastuuhenkilonNimi = vastuuhenkilo.nimi
        result.vastuuhenkilonNimike = vastuuhenkilo.nimike
        result.tyoskentelyjaksot = tyoskentelyjaksot.map(tyoskentelyjaksoMapper::toDto)

        return result
    }

    private fun getKokonaispituus(tyoskentelyjaksot: List<Tyoskentelyjakso>): Double {
        val hyvaksiluettavatCounter = HyvaksiluettavatCounterData().apply {
            hyvaksiluettavatPerYearMap =
                tyoskentelyjaksonPituusCounterService.getHyvaksiluettavatPerYearMap(
                    tyoskentelyjaksot
                )
        }
        var suoritettuPituus = 0.0

        tyoskentelyjaksot.forEach {
            val tyoskentelyjaksonPituus =
                tyoskentelyjaksonPituusCounterService.calculateInDays(it, hyvaksiluettavatCounter)
            if (tyoskentelyjaksonPituus > 0) {
                suoritettuPituus += tyoskentelyjaksonPituus
            }
        }

        return suoritettuPituus
    }
}
