package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.ErikoistujienSeurantaQueryService
import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.mapper.ArviointiasteikkoMapper
import org.springframework.context.MessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.*
import javax.persistence.EntityNotFoundException

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
    private val clock: Clock,
    private val messageSource: MessageSource
) : EtusivuService {

    override fun getErikoistujienSeurantaForVastuuhenkilo(userId: String): ErikoistujienSeurantaDTO {
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
            val seurattavatOpintooikeudet: MutableList<Opintooikeus> = mutableListOf()
            kayttaja.yliopistotAndErikoisalat.forEach {
                seurattavatOpintooikeudet.addAll(
                    opintooikeusRepository.findByErikoisalaAndYliopisto(
                        it.erikoisala?.id!!,
                        it.yliopisto?.id!!
                    )
                )
            }
            seurattavatOpintooikeudet.forEach {
                seurantaDTO.erikoistujienEteneminen?.add(
                    getErikoistujanEteneminenForKouluttajaOrVastuuhenkilo(it)
                )
            }
        }

        return seurantaDTO
    }

    override fun getErikoistujienSeurantaForKouluttaja(userId: String): ErikoistujienSeurantaDTO {
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
            opintooikeusRepository.findByKouluttajaValtuutus(kayttaja.id!!).forEach {
                seurantaDTO.erikoistujienEteneminen?.add(
                    getErikoistujanEteneminenForKouluttajaOrVastuuhenkilo(it)
                )
            }
        }

        return seurantaDTO
    }

    private fun getErikoistujanEteneminenForKouluttajaOrVastuuhenkilo(opintooikeus: Opintooikeus): ErikoistujanEteneminenDTO {
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

        eteneminen.koejaksoTila = getKoejaksoTila(opintooikeus)

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
                    k.user?.langKey
                ).map { opintooikeus ->
                    val opintosuoritukset =
                        opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id!!).asSequence()
                    ErikoistujanEteneminenVirkailijaDTO(
                        opintooikeus.id,
                        opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName,
                        opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName,
                        opintooikeus.erikoistuvaLaakari?.syntymaaika,
                        opintooikeus.erikoisala?.nimi,
                        opintooikeus.asetus?.nimi,
                        getKoejaksoTila(opintooikeus),
                        opintooikeus.opintooikeudenMyontamispaiva,
                        opintooikeus.opintooikeudenPaattymispaiva,
                        tyoskentelyjaksoService.getTilastot(opintooikeus),
                        getTeoriakoulutuksetTuntimaara(opintooikeus.id!!),
                        opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
                        getJohtamisopinnotSuoritettu(opintosuoritukset),
                        opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
                        getSateilysuojakoulutuksetSuoritettu(opintosuoritukset),
                        opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
                        getValtakunnallisetKuulustelutSuoritettuLkm(opintosuoritukset)
                    )
                }
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
                val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(it.id!!).asSequence()
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
                    getTeoriakoulutuksetTuntimaara(it.id!!),
                    it.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
                    getJohtamisopinnotSuoritettu(opintosuoritukset),
                    it.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
                    getSateilysuojakoulutuksetSuoritettu(opintosuoritukset),
                    it.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
                    getKoejaksoTila(it),
                    getValtakunnallisetKuulustelutSuoritettuLkm(opintosuoritukset),
                    it.opintooikeudenMyontamispaiva,
                    it.opintooikeudenPaattymispaiva
                )
            }
    }

    override fun getAvoimetAsiatForErikoistuja(userId: String): List<AvoinAsiaDTO>? {
        val avoimetAsiatList = mutableListOf<AvoinAsiaDTO>()
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        var locale = Locale.forLanguageTag("fi")
        if (user.langKey != null) locale = Locale.forLanguageTag(user.langKey)

        mapVanhenevatKatseluoikeudetToAvoimetAsiat(
            avoimetAsiatList,
            opintooikeusId,
            locale
        )

        return avoimetAsiatList
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

    private fun mapVanhenevatKatseluoikeudetToAvoimetAsiat(
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

    private fun getVanhenevatKatseluoikeudetPaattymispaivaAfterDate() = LocalDate.now(clock).minusDays(1)

    private fun getSuoritusarvioinnitMap(opintooikeusId: Long): Map<ArvioitavaKokonaisuus, Int> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .asSequence()
            .filter { arviointi -> arviointi.arviointiasteikonTaso != null }
            .groupBy { arviointi -> arviointi.arvioitavaKokonaisuus!! }
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
            ).map { it.arvioitavatKokonaisuudet }.flatten().size

        // Lisätään arvioitava kokonaisuus mukaan kokonaismäärään voimassaolosta huolimatta, jos siihen kohdistuu arviointi.
        arvioitavatKokonaisuudetWithArviointi.forEach {
            val arvioitavaKokonaisuusVoimassa = isValidByVoimassaDate(
                it.voimassaoloAlkaa!!,
                it.voimassaoloLoppuu,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            )
            val kategoriaVoimassa = isValidByVoimassaDate(
                it.kategoria?.voimassaoloAlkaa!!,
                it.kategoria?.voimassaoloLoppuu,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            )
            if (!arvioitavaKokonaisuusVoimassa || !kategoriaVoimassa) {
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
            kategoria.suoritteet.filter { suorite -> suorite.vaadittulkm != null }
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
                kategoria.suoritteet.filter { it.vaadittulkm != null }
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
        val kategoriaVoimassa = isValidByVoimassaDate(
            suorite.kategoria?.voimassaolonAlkamispaiva!!,
            suorite.kategoria?.voimassaolonPaattymispaiva,
            osaamisenArvioinninOppaanPvm
        )
        return !suoriteVoimassa || !kategoriaVoimassa
    }

    private fun isValidByVoimassaDate(
        voimassaoloAlkaaDate: LocalDate,
        voimassaoloPaattyy: LocalDate?,
        osaamisenArvioinninOppaanPvm: LocalDate
    ): Boolean =
        voimassaoloAlkaaDate <= osaamisenArvioinninOppaanPvm &&
            (voimassaoloPaattyy == null || voimassaoloPaattyy >= osaamisenArvioinninOppaanPvm)

    private fun getTeoriakoulutuksetTuntimaara(opintooikeusId: Long): Double =
        teoriakoulutusRepository.findAllByOpintooikeusId(opintooikeusId).sumOf { t ->
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
        opintooikeus: Opintooikeus
    ): KoejaksoTila {
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
}
