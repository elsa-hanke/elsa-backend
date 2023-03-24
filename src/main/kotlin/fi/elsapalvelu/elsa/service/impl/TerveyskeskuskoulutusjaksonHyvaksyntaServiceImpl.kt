package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.VASTUUHENKILO_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
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
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.web.multipart.MultipartFile

private const val TERVEYSKESKUS_HYVAKSYNTA_MINIMIPITUUS = 273.75 // 9kk

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
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val applicationProperties: ApplicationProperties,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) : TerveyskeskuskoulutusjaksonHyvaksyntaService {

    override fun findByIdAndYliopistoIdVirkailija(
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

    override fun findByIdAndYliopistoIdVastuuhenkilo(
        id: Long,
        yliopistoIds: List<Long>
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByIdAndYliopistoIdForVastuuhenkilo(
            id,
            yliopistoIds
        )?.let {
            val result = mapTerveyskeskuskoulutusjakso(it)
            if (it.korjausehdotusVastuuhenkilolta != null) {
                result.tila = TerveyskeskuskoulutusjaksoTila.PALAUTETTU_KORJATTAVAKSI
                result.korjausehdotus = it.korjausehdotusVastuuhenkilolta
            }
            return result
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
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<TerveyskeskuskoulutusjaksoSimpleDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            return terveyskeskuskoulutusjaksonHyvaksyntaQueryService.findByCriteriaAndYliopistoId(
                criteria,
                pageable,
                k.yliopistot.map { it.id!! },
                true,
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
        return null
    }

    override fun findByVastuuhenkiloUserId(
        userId: String,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<TerveyskeskuskoulutusjaksoSimpleDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            return terveyskeskuskoulutusjaksonHyvaksyntaQueryService.findByCriteriaAndYliopistoId(
                criteria,
                pageable,
                k.yliopistotAndErikoisalat.filter {
                    it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                        .contains(VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN)
                }
                    .map { it.yliopisto?.id!! },
                false,
                k.user?.langKey
            ).map { hyvaksynta ->
                TerveyskeskuskoulutusjaksoSimpleDTO(
                    hyvaksynta.id,
                    TerveyskeskuskoulutusjaksoTila.fromHyvaksynta(hyvaksynta, true),
                    hyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
                    hyvaksynta.muokkauspaiva
                )
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
                tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppiAndKaytannonKoulutus(
                    opintooikeusId,
                    TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                    KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
                )
            val suoritettuPituus = getKokonaispituus(tyoskentelyjaksot)

            if (suoritettuPituus < TERVEYSKESKUS_HYVAKSYNTA_MINIMIPITUUS) {
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
                yleislaaketieteenVastuuhenkilonNimi = vastuuhenkilo.nimi,
                yleislaaketieteenVastuuhenkilonNimike = vastuuhenkilo.nimike
            )
        }
        return null
    }

    override fun getTerveyskoulutusjaksoSuoritettu(opintooikeusId: Long): Boolean {
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppiAndKaytannonKoulutus(
                opintooikeusId,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
            )
        val suoritettuPituus = getKokonaispituus(tyoskentelyjaksot)

        return suoritettuPituus >= TERVEYSKESKUS_HYVAKSYNTA_MINIMIPITUUS
    }

    override fun create(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO? {
        opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            val hyvaksynta = TerveyskeskuskoulutusjaksonHyvaksynta(opintooikeus = it)
            terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(hyvaksynta)

            mailService.sendEmailFromTemplate(
                to = it.yliopisto?.nimi?.getOpintohallintoEmailAddress(applicationProperties),
                templateName = "tkkjaksonHyvaksymishakemusTarkastettavissa.html",
                titleKey = "email.tkkjaksonhyvaksymishakemustarkastettavissa.title",
                properties = mapOf(
                    Pair(MailProperty.ID, hyvaksynta.id.toString()),
                    Pair(MailProperty.URL_PATH, "terveyskeskuskoulutusjakson-tarkistus")
                )
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
        lisatiedotVirkailijalta: String?,
        laillistamispaiva: LocalDate?,
        laillistamistodistus: MultipartFile?
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
                        vastuuhenkilo,
                        laillistamispaiva,
                        laillistamistodistus
                    )
                } else if (vastuuhenkilo?.id == kayttaja.id) {
                    return handleVastuuhenkilo(it, kayttaja, korjausehdotus)
                }
                return null
            }
    }

    private fun handleErikoistuja(hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta): TerveyskeskuskoulutusjaksonHyvaksyntaDTO {
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
        vastuuhenkilo: Kayttaja?,
        laillistamispaiva: LocalDate?,
        laillistamistodistus: MultipartFile?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO {
        hyvaksynta.virkailija = kayttaja
        if (korjausehdotus != null) {
            hyvaksynta.korjausehdotus = korjausehdotus
        } else {
            hyvaksynta.virkailijaHyvaksynyt = true
            hyvaksynta.virkailijanKuittausaika = LocalDate.now()
            hyvaksynta.lisatiedotVirkailijalta = lisatiedotVirkailijalta
            hyvaksynta.korjausehdotusVastuuhenkilolta = null
        }

        if (laillistamistodistus != null || laillistamispaiva != null) {
            erikoistuvaLaakariService.updateLaillistamispaiva(
                hyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                laillistamispaiva,
                laillistamistodistus?.bytes,
                laillistamistodistus?.originalFilename,
                laillistamistodistus?.contentType
            )
        }

        val result = terveyskeskuskoulutusjaksonHyvaksyntaRepository.save(hyvaksynta)

        if (result.virkailijaHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                to = vastuuhenkilo?.user?.email,
                templateName = "tkkjaksonHyvaksymishakemusTarkastettavissa.html",
                titleKey = "email.tkkjaksonhyvaksymishakemustarkastettavissa.title",
                properties = mapOf(
                    Pair(MailProperty.ID, result.id.toString()),
                    Pair(MailProperty.URL_PATH, "terveyskeskuskoulutusjakson-hyvaksynta")
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

    private fun handleVastuuhenkilo(
        hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta,
        kayttaja: Kayttaja,
        korjausehdotus: String?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO {
        hyvaksynta.vastuuhenkilo = kayttaja
        if (korjausehdotus != null) {
            hyvaksynta.korjausehdotus = korjausehdotus
            hyvaksynta.korjausehdotusVastuuhenkilolta = korjausehdotus
            hyvaksynta.lisatiedotVirkailijalta = null
            hyvaksynta.virkailija = null
            hyvaksynta.virkailijanKuittausaika = null
            hyvaksynta.virkailijaHyvaksynyt = false
        } else {
            hyvaksynta.vastuuhenkiloHyvaksynyt = true
            hyvaksynta.vastuuhenkilonKuittausaika = LocalDate.now()

            val tyoskentelyjaksot =
                tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppiAndKaytannonKoulutus(
                    hyvaksynta.opintooikeus?.id!!,
                    TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                    KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
                )
            tyoskentelyjaksot.forEach {
                it.liitettyTerveyskeskuskoulutusjaksoon = true
                tyoskentelyjaksoRepository.save(it)
            }
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
                        "tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta"
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
                        "terveyskeskuskoulutusjaksot/terveyskeskuskoulutusjakson-tarkistus/${hyvaksynta.id}"
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
            tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppiAndKaytannonKoulutus(
                hyvaksynta.opintooikeus?.id!!,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
            )
        val suoritettuPituus = getKokonaispituus(tyoskentelyjaksot)

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
        result.yleislaaketieteenVastuuhenkilonNimi = vastuuhenkilo.nimi
        result.yleislaaketieteenVastuuhenkilonNimike = vastuuhenkilo.nimike
        result.tyoskentelyjaksot = tyoskentelyjaksot.map(tyoskentelyjaksoMapper::toDto)
        result.tila = TerveyskeskuskoulutusjaksoTila.fromHyvaksynta(hyvaksynta)

        return result
    }

    private fun getKokonaispituus(tyoskentelyjaksot: List<Tyoskentelyjakso>): Double {
        var suoritettuPituus = 0.0
        val vahennettavatMap = tyoskentelyjaksoService.getVahennettavatPaivat(tyoskentelyjaksot)

        tyoskentelyjaksot.forEach {
            val tyoskentelyjaksonPituus =
                tyoskentelyjaksonPituusCounterService.calculateInDays(it, vahennettavatMap[it.id])
            if (tyoskentelyjaksonPituus > 0) {
                suoritettuPituus += tyoskentelyjaksonPituus
            }
        }

        return suoritettuPituus
    }
}
