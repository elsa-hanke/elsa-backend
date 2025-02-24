package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.extensions.pattern
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.mapper.ArviointiasteikkoMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.MessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional
class EtusivuServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val arvioitavanKokonaisuudenKategoriaRepository: ArvioitavanKokonaisuudenKategoriaRepository,
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository,
    private val erikoistujienSeurantaQueryService: ErikoistujienSeurantaQueryService,
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val arviointiasteikkoMapper: ArviointiasteikkoMapper,
    private val opintooikeusService: OpintooikeusService,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val opintosuoritusService: OpintosuoritusService,
    private val clock: Clock,
    private val messageSource: MessageSource,
    private val erikoisalaRepository: ErikoisalaRepository
) : EtusivuService {

    override fun getErikoistujienSeurantaVastuuhenkiloRajaimet(
        userId: String
    ): ErikoistujienSeurantaDTO {
        val kayttaja: Kayttaja? = kayttajaRepository.findOneByUserId(userId).orElse(null)
        val seurantaDTO = ErikoistujienSeurantaDTO()
        kayttaja?.let {
            seurantaDTO.kayttajaYliopistoErikoisalat =
                kayttaja.yliopistotAndErikoisalat.groupBy { it.yliopisto }.map {
                    KayttajaErikoisalatPerYliopistoDTO(
                        yliopistoNimi = it.key?.nimi.toString(),
                        erikoisalat = it.value.map { kayttajaYliopistoErikoisala -> kayttajaYliopistoErikoisala.erikoisala?.nimi!! }
                            .sorted()
                    )
                }
            seurantaDTO.kayttajaYliopistoErikoisalat?.forEach {
                seurantaDTO.erikoisalat?.addAll(it.erikoisalat!!)
            }
            seurantaDTO.erikoisalat = seurantaDTO.erikoisalat?.sorted()?.toMutableSet()
        }
        return seurantaDTO
    }

    override fun getErikoistujienSeurantaForVastuuhenkilo(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
    ): Page<ErikoistujanEteneminenDTO>? {
        val kayttaja: Kayttaja? = kayttajaRepository.findOneByUserId(userId).orElse(null)
        kayttaja?.let { k ->
            return erikoistujienSeurantaQueryService.findByErikoisalaAndYliopisto(
                criteria,
                pageable,
                k.user?.langKey,
                k.yliopistotAndErikoisalat
            ).map { opintooikeus -> getErikoistujanEteneminenForKouluttajaOrVastuuhenkilo(opintooikeus) }
        }
        return null
    }

    override fun getKoulutettavienSeurantaForVastuuhenkilo(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<KoulutettavanEteneminenDTO>? {
        val kayttaja: Kayttaja? = kayttajaRepository.findOneByUserId(userId).orElse(null)
        kayttaja?.let { k ->
            return erikoistujienSeurantaQueryService.findByErikoisalaAndYliopisto(
                criteria,
                pageable,
                k.user?.langKey,
                k.yliopistotAndErikoisalat
            ).map { opintooikeus -> mapKoulutettavanEdistyminen(opintooikeus) }
        }
        return null
    }

    override fun getErikoistujienSeurantaKouluttajaRajaimet(
        userId: String
    ): ErikoistujienSeurantaDTO {
        val kayttaja: Kayttaja? = kayttajaRepository.findOneByUserId(userId).orElse(null)
        val seurantaDTO = ErikoistujienSeurantaDTO()
        kayttaja?.let {
            seurantaDTO.kayttajaYliopistoErikoisalat =
                kayttaja.yliopistotAndErikoisalat.groupBy { it.yliopisto }.map {
                    KayttajaErikoisalatPerYliopistoDTO(
                        yliopistoNimi = it.key?.nimi.toString(),
                        erikoisalat = it.value.map { kayttajaYliopistoErikoisala -> kayttajaYliopistoErikoisala.erikoisala?.nimi!! }
                            .sorted()
                    )
                }
            seurantaDTO.kayttajaYliopistoErikoisalat?.forEach {
                seurantaDTO.erikoisalat?.addAll(it.erikoisalat!!)
            }
            seurantaDTO.erikoisalat = seurantaDTO.erikoisalat?.sorted()?.toMutableSet()
        }
        return seurantaDTO
    }

    override fun getErikoistujienSeurantaForKouluttaja(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
    ): Page<ErikoistujanEteneminenDTO>? {
        val kayttaja: Kayttaja? = kayttajaRepository.findOneByUserId(userId).orElse(null)
        kayttaja?.let { k ->
            return erikoistujienSeurantaQueryService.findByKouluttajaValtuutus(
                criteria,
                pageable,
                k.id!!
            ).map { opintooikeus -> getErikoistujanEteneminenForKouluttajaOrVastuuhenkilo(opintooikeus) }
        }
        return null
    }

    private fun getErikoistujanEteneminenForKouluttajaOrVastuuhenkilo(
        opintooikeus: Opintooikeus
    ): ErikoistujanEteneminenDTO {
        val eteneminen = ErikoistujanEteneminenDTO()

        // Erikoistujan tiedot
        eteneminen.opintooikeusId = opintooikeus.id
        eteneminen.erikoistuvaLaakariEtuNimi =
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName
        eteneminen.erikoistuvaLaakariSukuNimi =
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName
        eteneminen.erikoistuvaLaakariSyntymaaika = opintooikeus.erikoistuvaLaakari?.syntymaaika
        eteneminen.opintooikeudenMyontamispaiva = opintooikeus.opintooikeudenMyontamispaiva
        eteneminen.opintooikeudenPaattymispaiva = opintooikeus.opintooikeudenPaattymispaiva
        eteneminen.asetus = opintooikeus.asetus?.nimi
        eteneminen.erikoisala = opintooikeus.erikoisala?.nimi
        eteneminen.laakarikoulutusSuoritettuSuomiTaiBelgia =
            opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuSuomiTaiBelgia
        eteneminen.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia =
            opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia

        // Työskentelyjaksot
        eteneminen.tyoskentelyjaksoTilastot = tyoskentelyjaksoService.getTilastot(opintooikeus)

        // Suoritusarvioinnit
        val suoritusarvioinnitMap = getSuoritusarvioinnitMap(opintooikeus.id!!)
        eteneminen.arviointienKeskiarvo = getArviointienKeskiarvo(suoritusarvioinnitMap)
        eteneminen.arviointienLkm =
            getArvioitavatKokonaisuudetVahintaanYksiArvioLkm(suoritusarvioinnitMap)
        eteneminen.arvioitavienKokonaisuuksienLkm =
            getArvioitavienKokonaisuuksienLkm(opintooikeus, suoritusarvioinnitMap.keys)

        // Seurantajaksot
        val seurantajaksot = seurantajaksoRepository.findByOpintooikeusId(opintooikeus.id!!)
        eteneminen.seurantajaksotLkm = seurantajaksot.size
        eteneminen.seurantajaksonHuoletLkm =
            seurantajaksot.filter { jakso -> jakso.huolenaiheet != null }.size

        // Suoritemerkinnät
        val suoritemerkinnatMap = getSuoritemerkinnatMap(opintooikeus.id!!)
        eteneminen.suoritemerkinnatLkm = getSuoritemerkinnatLkm(suoritemerkinnatMap)
        eteneminen.vaaditutSuoritemerkinnatLkm =
            getVaaditutSuoritemerkinnatLkm(opintooikeus, suoritemerkinnatMap.values.flatten())

        val opintosuoritukset =
            opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id!!).asSequence()
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
            opintooikeus.erikoistuvaLaakari?.id!!,
            YEK_ERIKOISALA_ID
        )
        eteneminen.koejaksoTila = getKoejaksoTila(opintooikeus, opintosuoritukset)
        eteneminen.terveyskeskuskoulutusjaksoSuoritettu =
            getTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset + yekSuoritukset)

        return eteneminen
    }

    override fun getErikoistujienSeurantaForVirkailija(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<ErikoistujanEteneminenVirkailijaDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            // Opintohallinnon virkailija toimii vain yhden yliopiston alla.
            k.yliopistot.firstOrNull()?.let {
                return erikoistujienSeurantaQueryService.findByCriteriaAndYliopistoId(
                    criteria,
                    pageable,
                    it.id!!,
                    k.user?.langKey,
                    YEK_ERIKOISALA_ID
                ).map { opintooikeus ->
                    val opintosuoritukset =
                        opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id!!)
                            .asSequence()
                    val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
                        opintooikeus.erikoistuvaLaakari?.id!!,
                        YEK_ERIKOISALA_ID
                    )
                    ErikoistujanEteneminenVirkailijaDTO(
                        opintooikeus.id,
                        opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName,
                        opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName,
                        opintooikeus.erikoistuvaLaakari?.syntymaaika,
                        opintooikeus.erikoisala?.nimi,
                        opintooikeus.asetus?.nimi,
                        getKoejaksoTila(opintooikeus, opintosuoritukset),
                        opintooikeus.opintooikeudenMyontamispaiva,
                        opintooikeus.opintooikeudenPaattymispaiva,
                        tyoskentelyjaksoService.getTilastot(opintooikeus),
                        getTeoriakoulutuksetTuntimaara(
                            opintooikeus.id!!,
                            opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID,
                            opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
                        ),
                        opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
                        getJohtamisopinnotSuoritettu(opintosuoritukset),
                        opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
                        getSateilysuojakoulutuksetSuoritettu(opintosuoritukset),
                        opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
                        getValtakunnallisetKuulustelutSuoritettuLkm(opintosuoritukset),
                        getTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset + yekSuoritukset)
                    )
                }
            }
        }
        return null
    }

    override fun getKoulutettavienSeurantaForVirkailija(
        userId: String,
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable
    ): Page<KoulutettavanEteneminenDTO>? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let { k ->
            // Opintohallinnon virkailija toimii vain yhden yliopiston alla.
            k.yliopistot.firstOrNull()?.let {
                return erikoistujienSeurantaQueryService.findByCriteriaAndYliopistoIdAndErikoisalaId(
                    criteria,
                    pageable,
                    it.id!!,
                    YEK_ERIKOISALA_ID,
                    k.user?.langKey
                ).map { opintooikeus -> mapKoulutettavanEdistyminen(opintooikeus) }
            }
        }
        return null
    }

    override fun getErikoistumisenSeurantaForErikoistuja(userId: String): ErikoistumisenEdistyminenDTO? {
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId).orElse(null)
        return opintooikeus
            .let {
                val suoritusarvioinnitMap = getSuoritusarvioinnitMap(it.id!!)
                val arvioitavatKokonaisuudetWithArviointi = suoritusarvioinnitMap.keys
                val suoritemerkinnatMap = getSuoritemerkinnatMap(it.id!!)
                val suoritemerkinnat = suoritemerkinnatMap.values.flatten()
                val opintosuoritukset =
                    opintosuoritusRepository.findAllByOpintooikeusId(it.id!!).asSequence()
                val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
                    opintooikeus.erikoistuvaLaakari?.id!!,
                    YEK_ERIKOISALA_ID
                )
                val arviointiasteikko =
                    it.opintoopas?.arviointiasteikko?.let { arviointiasteikkoMapper.toDto(it) }

                ErikoistumisenEdistyminenDTO(
                    getArviointienKeskiarvo(suoritusarvioinnitMap),
                    getArvioitavatKokonaisuudetVahintaanYksiArvioLkm(suoritusarvioinnitMap),
                    getArvioitavienKokonaisuuksienLkm(it, arvioitavatKokonaisuudetWithArviointi),
                    arviointiasteikko,
                    getSuoritemerkinnatLkm(suoritemerkinnatMap),
                    getVaaditutSuoritemerkinnatLkm(it, suoritemerkinnat),
                    getSuoriteOsaAlueetSuoritettuLkm(
                        suoritemerkinnatMap,
                        it.osaamisenArvioinninOppaanPvm!!
                    ),
                    getSuoriteOsaAlueetVaadittuLkm(it, suoritemerkinnatMap),
                    tyoskentelyjaksoService.getTilastot(it),
                    getTeoriakoulutuksetTuntimaara(
                        it.id!!,
                        opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID,
                        opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
                    ),
                    it.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
                    getJohtamisopinnotSuoritettu(opintosuoritukset),
                    it.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
                    getSateilysuojakoulutuksetSuoritettu(opintosuoritukset),
                    it.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
                    getKoejaksoTila(it, opintosuoritukset),
                    koejaksonSuoritusmerkintaExists(opintosuoritukset),
                    getValtakunnallisetKuulustelutSuoritettuLkm(opintosuoritukset),
                    it.opintooikeudenMyontamispaiva,
                    it.opintooikeudenPaattymispaiva,
                    getTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset + yekSuoritukset),
                    opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuSuomiTaiBelgia,
                    yekSuoritukset.firstOrNull { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_PATEVYYS }?.suorituspaiva
                )
            }
    }

    override fun getAvoimetAsiatForErikoistuja(userId: String): List<AvoinAsiaDTO>? {
        val avoimetAsiatList = mutableListOf<AvoinAsiaDTO>()
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        var locale = Locale.forLanguageTag("fi")
        if (user.langKey != null) locale = Locale.forLanguageTag(user.langKey)

        mapVanhenevatKatseluoikeudetToAvoimetAsiatIfExists(avoimetAsiatList, opintooikeusId, locale)
        mapKoulutussopimusPalautettuIfExists(avoimetAsiatList, opintooikeusId, locale)
        mapAloituskeskusteluPalautettuIfExists(avoimetAsiatList, opintooikeusId, locale)
        mapVastuuhenkilonarvioPalautettuIfExists(avoimetAsiatList, opintooikeusId, locale)
        mapSeurantajaksotPalautettuIfExists(avoimetAsiatList, opintooikeusId, locale)
        mapTerveyskeskuskoulutusjaksoPalautettuOrHaettavissaIfExists(
            avoimetAsiatList,
            opintooikeusId,
            locale
        )
        mapValmistumispyyntoPalautettuIfExists(avoimetAsiatList, opintooikeusId, locale)

        return avoimetAsiatList.sortedBy { it.pvm }
    }

    override fun getAvoimetAsiatForYekKoulutettava(userId: String): List<AvoinAsiaDTO>? {
        val avoimetAsiatList = mutableListOf<AvoinAsiaDTO>()
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        var locale = Locale.forLanguageTag("fi")
        if (user.langKey != null) locale = Locale.forLanguageTag(user.langKey)

        mapVanhenevatKatseluoikeudetToAvoimetAsiatIfExists(avoimetAsiatList, opintooikeusId, locale)
        mapTerveyskeskuskoulutusjaksoPalautettuOrHaettavissaIfExists(
            avoimetAsiatList,
            opintooikeusId,
            locale,
            true
        )
        mapValmistumispyyntoPalautettuIfExists(avoimetAsiatList, opintooikeusId, locale)

        return avoimetAsiatList.sortedBy { it.pvm }
    }

    override fun getVanhenevatKatseluoikeudetForKouluttaja(userId: String): List<KatseluoikeusDTO>? {
        return kouluttajavaltuutusRepository.findAllByValtuutettuUserIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
            userId,
            getVanhenevatKatseluoikeudetPaattymispaivaBeforeDate(),
            getVanhenevatKatseluoikeudetPaattymispaivaAfterDate()
        ).map {
            KatseluoikeusDTO(
                it.valtuuttajaOpintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
                it.paattymispaiva
            )
        }
    }

    private fun mapVanhenevatKatseluoikeudetToAvoimetAsiatIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale
    ) {
        kouluttajavaltuutusRepository.findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
            opintooikeusId,
            getVanhenevatKatseluoikeudetPaattymispaivaBeforeDate(),
            getVanhenevatKatseluoikeudetPaattymispaivaAfterDate()
        ).map {
            val avoinAsia = AvoinAsiaDTO(
                it.id,
                AvoinAsiaTyyppiEnum.KOULUTTAJAVALTUUTUS,
                messageSource.getMessage(
                    "avoimetasiat.kouluttajavaltuutus",
                    arrayOf("${it.valtuutettu?.user?.firstName} ${it.valtuutettu?.user?.lastName}"),
                    locale
                ),
                it.paattymispaiva
            )
            avoimetAsiatList.add(avoinAsia)
        }
    }

    private fun getVanhenevatKatseluoikeudetPaattymispaivaBeforeDate() =
        LocalDate.now(clock).plusMonths(1).plusDays(1)

    private fun getVanhenevatKatseluoikeudetPaattymispaivaAfterDate() =
        LocalDate.now(clock).minusDays(1)

    private fun getSuoritusarvioinnitMap(opintooikeusId: Long): Map<ArvioitavaKokonaisuus, Int> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .asSequence()
            .flatMap { arviointi -> arviointi.arvioitavatKokonaisuudet }
            .filter { kokonaisuus -> kokonaisuus.arviointiasteikonTaso != null }
            .groupBy { kokonaisuus -> kokonaisuus.arvioitavaKokonaisuus!! }
            .mapValues { it.value.maxOf { arviointi -> arviointi.arviointiasteikonTaso!! } }
    }

    private fun getArviointienKeskiarvo(suoritusarvioinnitMap: Map<ArvioitavaKokonaisuus, Int>): Double? =
        if (suoritusarvioinnitMap.isNotEmpty())
            suoritusarvioinnitMap.values.sumOf { it } / suoritusarvioinnitMap.keys.size.toDouble()
        else null

    private fun getArvioitavatKokonaisuudetVahintaanYksiArvioLkm(suoritusarvioinnitMap: Map<ArvioitavaKokonaisuus, Int>): Int =
        suoritusarvioinnitMap.keys.size

    private fun getArvioitavienKokonaisuuksienLkm(
        opintooikeus: Opintooikeus,
        arvioitavatKokonaisuudetWithArviointi: Set<ArvioitavaKokonaisuus>
    ): Int {
        var arvioitavatKokonaisuudetLkm =
            arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(
                opintooikeus.erikoisala?.id,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            ).map { it.arvioitavatKokonaisuudet }.flatten().filter {
                isValidByVoimassaDate(
                    it.voimassaoloAlkaa!!,
                    it.voimassaoloLoppuu,
                    opintooikeus.osaamisenArvioinninOppaanPvm!!
                )
            }.size

        // Lisätään arvioitava kokonaisuus mukaan kokonaismäärään voimassaolosta huolimatta, jos siihen kohdistuu arviointi.
        arvioitavatKokonaisuudetWithArviointi.forEach {
            val arvioitavaKokonaisuusVoimassa = isValidByVoimassaDate(
                it.voimassaoloAlkaa!!,
                it.voimassaoloLoppuu,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            )
            if (!arvioitavaKokonaisuusVoimassa) {
                arvioitavatKokonaisuudetLkm++
            }
        }

        return arvioitavatKokonaisuudetLkm
    }

    private fun getSuoritemerkinnatMap(opintooikeusId: Long): Map<Suorite, List<Suoritemerkinta>> =
        suoritemerkintaRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .groupBy { merkinta -> merkinta.suorite!! }

    private fun getSuoritemerkinnatLkm(suoritemerkinnatMap: Map<Suorite, List<Suoritemerkinta>>): Int {
        var lkm = 0
        suoritemerkinnatMap.forEach {
            // Mikäli vaadittu määrä asetettu, ei huomioida sen yli meneviä suoritemerkintöjä.
            val merkinnat =
                if (it.key.vaadittulkm != null && it.value.size > it.key.vaadittulkm!!)
                    it.key.vaadittulkm!!
                else it.value.size
            lkm = lkm.plus(merkinnat)
        }
        return lkm
    }

    private fun getVaaditutSuoritemerkinnatLkm(
        opintooikeus: Opintooikeus,
        suoritemerkinnat: List<Suoritemerkinta>
    ): Int {
        val suoritteenKategoriatByVoimassa =
            suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(
                opintooikeus.erikoisala?.id,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            )

        var vaaditutSuoritteetLkm = suoritteenKategoriatByVoimassa.sumOf { kategoria ->
            kategoria.suoritteet.filter { suorite ->
                isValidByVoimassaDate(
                    suorite.voimassaolonAlkamispaiva!!,
                    suorite.voimassaolonPaattymispaiva,
                    opintooikeus.osaamisenArvioinninOppaanPvm!!
                ) && suorite.vaadittulkm != null
            }
                .sumOf { suorite -> suorite.vaadittulkm!! }
        }
        suoritemerkinnat.forEach {
            // Otetaan suorite mukaan vaadittujen kokonaismäärän laskentaan voimassaolosta huolimatta,
            // jos siihen kohdistuu suoritemerkintä ja vaadittulkm on asetettu.
            // Ei kuitenkaan lisätä lukumäärään koko vaadittua määrää, koska voimassaolo kyseisen suoritteen tai sen
            // kategorian osalta on jo umpeutunut, eikä vaatimus näin ollen myöskään ole enää voimassa.
            if (suoriteOrKategoriaNotVoimassa(
                    it.suorite!!,
                    opintooikeus.osaamisenArvioinninOppaanPvm!!
                ) && it.suorite?.vaadittulkm != null
            ) {
                vaaditutSuoritteetLkm++
            }
        }

        return vaaditutSuoritteetLkm
    }

    private fun getSuoriteOsaAlueetSuoritettuLkm(
        suoritemerkinnatMap: Map<Suorite, List<Suoritemerkinta>>,
        osaamisenArvioinninOppaanPvm: LocalDate
    ): Int {
        var lkm = 0
        // Lisätään suoritettujen osa-alueiden määrää myös siinä tapauksessa, että suorite tai sen kategoria ei enää
        // ole voimassa, koska tällöin vaatimus suoritteiden määrästä ei enää päde ja kyseistä osa-aluetta voidaan
        // pitää suoritettuna.
        suoritemerkinnatMap.forEach {
            if (it.key.vaadittulkm != null && (it.value.size >= it.key.vaadittulkm!! || suoriteOrKategoriaNotVoimassa(
                    it.key,
                    osaamisenArvioinninOppaanPvm
                ))
            ) {
                lkm++
            }
        }

        return lkm
    }

    private fun getSuoriteOsaAlueetVaadittuLkm(
        opintooikeus: Opintooikeus,
        suoritemerkinnatMap: Map<Suorite, List<Suoritemerkinta>>
    ): Int {
        var suoriteOsaalueetVaadittuLkm =
            suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(
                opintooikeus.erikoisala?.id,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            ).map { kategoria ->
                kategoria.suoritteet.filter {
                    isValidByVoimassaDate(
                        it.voimassaolonAlkamispaiva!!,
                        it.voimassaolonPaattymispaiva,
                        opintooikeus.osaamisenArvioinninOppaanPvm!!
                    ) && it.vaadittulkm != null
                }
            }.flatten().size

        suoritemerkinnatMap.keys.forEach {
            // Otetaan osa-alue (suorite, jolle asetettu vaadittu lkm)
            // mukaan laskentaan voimassaolosta huolimatta, jos siihen kohdistuu suoritemerkintä.
            if (suoriteOrKategoriaNotVoimassa(
                    it,
                    opintooikeus.osaamisenArvioinninOppaanPvm!!
                ) && it.vaadittulkm != null
            ) {
                suoriteOsaalueetVaadittuLkm++
            }
        }

        return suoriteOsaalueetVaadittuLkm
    }

    private fun suoriteOrKategoriaNotVoimassa(
        suorite: Suorite,
        osaamisenArvioinninOppaanPvm: LocalDate
    ): Boolean {
        val suoriteVoimassa = isValidByVoimassaDate(
            suorite.voimassaolonAlkamispaiva!!,
            suorite.voimassaolonPaattymispaiva,
            osaamisenArvioinninOppaanPvm
        )
        return !suoriteVoimassa
    }

    private fun isValidByVoimassaDate(
        voimassaoloAlkaaDate: LocalDate,
        voimassaoloPaattyy: LocalDate?,
        osaamisenArvioinninOppaanPvm: LocalDate
    ): Boolean =
        voimassaoloAlkaaDate <= osaamisenArvioinninOppaanPvm &&
            (voimassaoloPaattyy == null || voimassaoloPaattyy >= osaamisenArvioinninOppaanPvm)

    private fun getTeoriakoulutuksetTuntimaara(opintooikeusId: Long, yek: Boolean, vaadittu: Double?): Double =
        if (yek) {
            if (opintosuoritusRepository.findAllByOpintooikeusIdAndTyyppi(
                    opintooikeusId,
                    OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS
                ).isNotEmpty()
            ) vaadittu ?: 0.0
            else 0.0
        } else teoriakoulutusRepository.findAllByOpintooikeusId(opintooikeusId).sumOf { t ->
            t.erikoistumiseenHyvaksyttavaTuntimaara ?: 0.0
        }

    private fun getJohtamisopinnotSuoritettu(
        opintosuoritukset: Sequence<Opintosuoritus>
    ): Double {
        return opintosuoritukset.filter { opintosuoritus ->
            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
        }.sumOf { johtamisopinto ->
            johtamisopinto.opintopisteet ?: 0.0
        }
    }

    private fun getSateilysuojakoulutuksetSuoritettu(
        opintosuoritukset: Sequence<Opintosuoritus>
    ): Double {
        return opintosuoritukset.filter { opintosuoritus ->
            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS
        }.sumOf { sateilysuojakoulutus ->
            sateilysuojakoulutus.opintopisteet ?: 0.0
        }
    }

    private fun getValtakunnallisetKuulustelutSuoritettuLkm(opintosuoritukset: Sequence<Opintosuoritus>): Int =
        opintosuoritukset.count { opintosuoritus ->
            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU && opintosuoritus.hyvaksytty
        }

    private fun getKoejaksoTila(
        opintooikeus: Opintooikeus,
        opintosuoritukset: Sequence<Opintosuoritus>
    ): KoejaksoTila {
        if (koejaksonSuoritusmerkintaExists(opintosuoritukset)) {
            return KoejaksoTila.HYVAKSYTTY
        }

        val koulutussopimus =
            koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeus.id!!)
        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeus.id!!)
        val aloituskeskustelu =
            koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintooikeus.id!!)

        return if (vastuuhenkilonArvio.isPresent && vastuuhenkilonArvio.get().koejaksoHyvaksytty == true) {
            KoejaksoTila.HYVAKSYTTY
        } else if ((koulutussopimus.isPresent && koulutussopimus.get().koejaksonAlkamispaiva?.isAfter(
                LocalDate.now()
            ) != true) || (aloituskeskustelu.isPresent && aloituskeskustelu.get().koejaksonAlkamispaiva?.isAfter(
                LocalDate.now()
            ) != true)
        ) {
            KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        } else {
            KoejaksoTila.EI_AKTIIVINEN
        }
    }

    private fun koejaksonSuoritusmerkintaExists(opintosuoritukset: Sequence<Opintosuoritus>) =
        opintosuoritukset.any { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.KOEJAKSO && it.hyvaksytty }

    private fun getTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset: Sequence<Opintosuoritus>): Boolean {
        return opintosuoritukset.any {
            (it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO
                || it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO) && it.hyvaksytty
        }
    }

    private fun mapKoulutussopimusPalautettuIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale
    ) {
        koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeusId).ifPresent {
            if (!it.korjausehdotus.isNullOrBlank()) {
                avoimetAsiatList.add(
                    AvoinAsiaDTO(
                        tyyppi = AvoinAsiaTyyppiEnum.KOULUTUSSOPIMUS,
                        asia = messageSource.getMessage(
                            "avoimetasiat.koulutussopimus",
                            null,
                            locale
                        ),
                        pvm = it.muokkauspaiva
                    )
                )
            }
        }
    }

    private fun mapAloituskeskusteluPalautettuIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale
    ) {
        koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintooikeusId).ifPresent {
            if (!it.korjausehdotus.isNullOrBlank()) {
                avoimetAsiatList.add(
                    AvoinAsiaDTO(
                        tyyppi = AvoinAsiaTyyppiEnum.ALOITUSKESKUSTELU,
                        asia = messageSource.getMessage(
                            "avoimetasiat.aloituskeskustelu",
                            null,
                            locale
                        ),
                        pvm = it.muokkauspaiva
                    )
                )
            }
        }
    }

    private fun mapVastuuhenkilonarvioPalautettuIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale
    ) {
        koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeusId).ifPresent {
            if (!it.virkailijanKorjausehdotus.isNullOrBlank() || !it.vastuuhenkilonKorjausehdotus.isNullOrBlank()) {
                avoimetAsiatList.add(
                    AvoinAsiaDTO(
                        tyyppi = AvoinAsiaTyyppiEnum.VASTUUHENKILON_ARVIO,
                        asia = messageSource.getMessage(
                            "avoimetasiat.vastuuhenkilonarvio",
                            null,
                            locale
                        ),
                        pvm = it.muokkauspaiva
                    )
                )
            }
        }
    }

    private fun mapSeurantajaksotPalautettuIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale
    ) {
        seurantajaksoRepository.findByOpintooikeusId(opintooikeusId).forEach {
            if (!it.korjausehdotus.isNullOrBlank()) {
                avoimetAsiatList.add(
                    AvoinAsiaDTO(
                        id = it.id,
                        tyyppi = AvoinAsiaTyyppiEnum.SEURANTAJAKSO,
                        asia = messageSource.getMessage(
                            "avoimetasiat.seurantajakso",
                            arrayOf(
                                "${it.alkamispaiva?.format(DateTimeFormatter.ofPattern(locale.pattern()))}",
                                "${it.paattymispaiva?.format(DateTimeFormatter.ofPattern(locale.pattern()))}"
                            ),
                            locale
                        ),
                        pvm = it.tallennettu
                    )
                )
            }
        }
    }

    private fun mapTerveyskeskuskoulutusjaksoPalautettuOrHaettavissaIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale,
        yek: Boolean = false
    ) {
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId).getOrNull()
        // Check if erikoisala tyyppi is HAMMASLAAKETIEDE since on that occasion, the terveyskeskuskoulutusjakso is not applicable
        val erikoisala: Erikoisala? = erikoisalaRepository.findById(opintooikeus?.erikoisala?.id!!).getOrNull()
        val hammaslaaketiede: ErikoisalaTyyppi = enumValueOf("HAMMASLAAKETIEDE")
        if (erikoisala != null && erikoisala.tyyppi == hammaslaaketiede) {
            return
        }
        if (opintosuoritusService.getTerveyskoulutusjaksoSuoritettu(opintooikeusId, opintooikeus.erikoistuvaLaakari?.id!!)) {
            return
        }
        val hyvaksynta =
            terveyskeskuskoulutusjaksonHyvaksyntaRepository.findByOpintooikeusId(opintooikeusId)
        if (hyvaksynta != null) {
            if (!hyvaksynta.virkailijanKorjausehdotus.isNullOrBlank() || !hyvaksynta.vastuuhenkilonKorjausehdotus.isNullOrBlank()) {
                avoimetAsiatList.add(
                    AvoinAsiaDTO(
                        hyvaksynta.id,
                        tyyppi = AvoinAsiaTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO,
                        asia = messageSource.getMessage(
                            "avoimetasiat.terveyskeskuskoulutusjaksonhyvaksynta",
                            arrayOf(),
                            locale
                        ),
                        pvm = hyvaksynta.muokkauspaiva
                    )
                )
            }
        } else if (!yek && terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(opintooikeusId)
            || (yek && terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettuYek(opintooikeusId))
        ) {
            avoimetAsiatList.add(
                AvoinAsiaDTO(
                    tyyppi = AvoinAsiaTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO,
                    asia = messageSource.getMessage(
                        "avoimetasiat.terveyskeskuskoulutusjaksonhyvaksynta",
                        arrayOf(),
                        locale
                    )
                )
            )
        }
    }

    private fun mapValmistumispyyntoPalautettuIfExists(
        avoimetAsiatList: MutableList<AvoinAsiaDTO>,
        opintooikeusId: Long,
        locale: Locale
    ) {
        valmistumispyyntoRepository.findByOpintooikeusId(opintooikeusId)?.let {
            if (it.vastuuhenkiloOsaamisenArvioijaPalautusaika != null ||
                it.virkailijanPalautusaika != null ||
                it.vastuuhenkiloHyvaksyjaPalautusaika != null
            ) {
                avoimetAsiatList.add(
                    AvoinAsiaDTO(
                        it.id,
                        tyyppi = AvoinAsiaTyyppiEnum.VALMISTUMISPYYNTO,
                        asia = messageSource.getMessage(
                            "avoimetasiat.valmistumispyynto",
                            arrayOf(),
                            locale
                        ),
                        pvm = getValmistumispyynnonPalautusaika(it)
                    )
                )
            }
        }
    }

    private fun getValmistumispyynnonPalautusaika(valmistumispyynto: Valmistumispyynto) =
        valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika
            ?: valmistumispyynto.virkailijanPalautusaika
            ?: valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika

    private fun mapKoulutettavanEdistyminen(opintooikeus: Opintooikeus): KoulutettavanEteneminenDTO {
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
            opintooikeus.erikoistuvaLaakari?.id!!,
            YEK_ERIKOISALA_ID
        )
        return KoulutettavanEteneminenDTO(
            opintooikeusId = opintooikeus.id,
            etunimi = opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName,
            sukunimi = opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName,
            syntymaaika = opintooikeus.erikoistuvaLaakari?.syntymaaika,
            erikoisala = opintooikeus.erikoisala?.nimi,
            asetus = opintooikeus.asetus?.nimi,
            opintooikeudenMyontamispaiva = opintooikeus.opintooikeudenMyontamispaiva,
            opintooikeudenPaattymispaiva = opintooikeus.opintooikeudenPaattymispaiva,
            tyoskentelyjaksoTilastot = tyoskentelyjaksoService.getTilastot(opintooikeus),
            teoriakoulutuksetSuoritettu = yekSuoritukset.any { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS },
            yekSuoritettu = yekSuoritukset.any { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_PATEVYYS }
        )
    }
}
