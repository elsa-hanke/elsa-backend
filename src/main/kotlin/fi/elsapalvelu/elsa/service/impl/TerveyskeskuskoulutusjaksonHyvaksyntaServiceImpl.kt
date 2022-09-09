package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Kayttaja
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
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.VASTUUHENKILO_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.TerveyskeskuskoulutusjaksoCriteria
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.TerveyskeskuskoulutusjaksonHyvaksyntaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoWithKeskeytysajatMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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
    private val tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaQueryService: TerveyskeskuskoulutusjaksonHyvaksyntaQueryService,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties

) : TerveyskeskuskoulutusjaksonHyvaksyntaService {

    override fun findByIdAndYliopistoId(
        id: Long,
        yliopistoIds: List<Long>
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByIdAndOpintooikeusYliopistoIdIn(
            id,
            yliopistoIds
        )?.let {
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

    override fun findByVirkailijaUserId(
        userId: String,
        criteria: TerveyskeskuskoulutusjaksoCriteria,
        pageable: Pageable
    ): Page<TerveyskeskuskoulutusjaksoSimpleDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            k.yliopistot.firstOrNull()?.let {
                return terveyskeskuskoulutusjaksonHyvaksyntaQueryService.findByCriteriaAndYliopistoId(
                    criteria,
                    pageable,
                    it.id!!,
                    k.user?.langKey
                ).map { hyvaksynta ->
                    TerveyskeskuskoulutusjaksoSimpleDTO(
                        hyvaksynta.id,
                        TerveyskeskuskoulutusjaksoTila.fromHyvaksynta(hyvaksynta),
                        hyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
                        hyvaksynta.muokkauspaiva
                    )
                }
            }
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

            mailService.sendEmailFromTemplate(
                to = it.yliopisto?.nimi?.getOpintohallintoEmailAddress(applicationProperties),
                templateName = "tkkjaksonHyvaksymishakemusTarkastettavissa.html",
                titleKey = "email.tkkjaksonhyvaksymishakemustarkastettavissa.title",
                properties = mapOf(Pair(MailProperty.ID, hyvaksynta.id.toString()))
            )

            return terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(hyvaksynta)
        }

        return null
    }

    override fun update(
        userId: String,
        isVirkailija: Boolean,
        id: Long,
        korjausehdotus: String?,
        lisatiedotVirkailijalta: String?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        val kayttaja = kayttajaRepository.findOneByUserId(userId)
            .orElseThrow { EntityNotFoundException("Käyttäjää ei löydy") }
        return terveyskeskuskoulutusjaksonHyvaksyntaRepository.findById(id)
            .orElse(null)?.let {
                val vastuuhenkilo =
                    kayttajaRepository.findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
                        listOf(VASTUUHENKILO),
                        it.opintooikeus?.yliopisto?.id,
                        VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN
                    )
                if (it.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id == userId) {
                    return handleErikoistuja(it)
                } else if (isVirkailija) {
                    return handleVirkailija(
                        it,
                        kayttaja,
                        korjausehdotus,
                        lisatiedotVirkailijalta,
                        vastuuhenkilo
                    )
                } else if (vastuuhenkilo?.id == kayttaja.id) {
                    return handleVastuuhenkilo(it, kayttaja, korjausehdotus)
                }
                return null
            }
    }

    private fun handleErikoistuja(hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        hyvaksynta.korjausehdotus = null
        hyvaksynta.virkailija = null
        hyvaksynta.vastuuhenkilo = null
        val result = terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(hyvaksynta)
        return terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(result)
    }

    private fun handleVirkailija(
        hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta,
        kayttaja: Kayttaja,
        korjausehdotus: String?,
        lisatiedotVirkailijalta: String?,
        vastuuhenkilo: Kayttaja?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO {
        hyvaksynta.virkailija = kayttaja
        if (korjausehdotus != null) {
            hyvaksynta.korjausehdotus = korjausehdotus
        } else {
            hyvaksynta.virkailijaHyvaksynyt = true
            hyvaksynta.virkailijanKuittausaika = LocalDate.now()
            hyvaksynta.lisatiedotVirkailijalta = lisatiedotVirkailijalta
        }

        val result = terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(hyvaksynta)

        if (result.virkailijaHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                to = vastuuhenkilo?.user?.email,
                templateName = "tkkjaksonHyvaksymishakemusTarkastettavissa.html",
                titleKey = "email.tkkjaksonhyvaksymishakemustarkastettavissa.title",
                properties = mapOf(Pair(MailProperty.ID, result.id.toString()))
            )
        } else {
            mailService.sendEmailFromTemplate(
                to = hyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email,
                templateName = "tkkjaksonHyvaksymishakemusPalautettu.html",
                titleKey = "email.tkkjaksonhyvaksymishakemuspalautettu.title",
                properties = mapOf()
            )
        }

        return terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(result)
    }

    private fun handleVastuuhenkilo(
        hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta,
        kayttaja: Kayttaja,
        korjausehdotus: String?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO {
        hyvaksynta.vastuuhenkilo = kayttaja
        if (korjausehdotus != null) {
            hyvaksynta.korjausehdotus = korjausehdotus
            hyvaksynta.korjausehdotusVastuuhenkilolta = korjausehdotus
            hyvaksynta.virkailija = null
            hyvaksynta.virkailijanKuittausaika = null
            hyvaksynta.virkailijaHyvaksynyt = false
        } else {
            hyvaksynta.vastuuhenkiloHyvaksynyt = true
            hyvaksynta.vastuuhenkilonKuittausaika = LocalDate.now()
        }

        val result = terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(hyvaksynta)

        if (result.vastuuhenkiloHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                to = hyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email,
                templateName = "tkkjaksonHyvaksymishakemusHyvaksytty.html",
                titleKey = "email.tkkjaksonhyvaksymishakemushyvaksytty.title",
                properties = mapOf(
                    Pair(
                        MailProperty.URL_PATH,
                        "tyoskentelyjaksot/terveyskeskuskoulutusjakso"
                    )
                )
            )
            mailService.sendEmailFromTemplate(
                to = hyvaksynta.virkailija?.user?.email,
                templateName = "tkkjaksonHyvaksymishakemusHyvaksytty.html",
                titleKey = "email.tkkjaksonhyvaksymishakemushyvaksytty.title",
                properties = mapOf(
                    Pair(
                        MailProperty.URL_PATH,
                        "terveyskeskuskoulutusjakso/${hyvaksynta.id}"
                    )
                )
            )
        } else {
            mailService.sendEmailFromTemplate(
                to = hyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email,
                templateName = "tkkjaksonHyvaksymishakemusPalautettu.html",
                titleKey = "email.tkkjaksonhyvaksymishakemuspalautettu.title",
                properties = mapOf()
            )
        }

        return terveyskeskuskoulutusjaksonHyvaksyntaMapper.toDto(result)
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
        result.tila = TerveyskeskuskoulutusjaksoTila.fromHyvaksynta(hyvaksynta)

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
