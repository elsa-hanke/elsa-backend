package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.ErikoistujienSeurantaQueryService
import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistujanEteneminenDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistujanEteneminenVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaErikoisalatPerYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class EtusivuServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository,
    private val erikoistujienSeurantaQueryService: ErikoistujienSeurantaQueryService,
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository
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
                seurantaDTO.erikoistujienEteneminen?.add(getErikoistujanEteneminen(it))
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
                seurantaDTO.erikoistujienEteneminen?.add(getErikoistujanEteneminen(it))
            }
        }

        return seurantaDTO
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
                    val teoriakoulutukset = teoriakoulutusRepository.findAllByOpintooikeusId(opintooikeus.id!!)
                    val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id!!)
                    ErikoistujanEteneminenVirkailijaDTO(
                        opintooikeus.erikoistuvaLaakari?.id,
                        opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName,
                        opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName,
                        opintooikeus.erikoistuvaLaakari?.syntymaaika,
                        opintooikeus.erikoisala?.nimi,
                        opintooikeus.asetus?.nimi,
                        getKoejaksoTila(opintooikeus),
                        opintooikeus.opintooikeudenMyontamispaiva,
                        opintooikeus.opintooikeudenPaattymispaiva,
                        tyoskentelyjaksoService.getTilastot(opintooikeus),
                        teoriakoulutukset.sumOf { t ->
                            t.erikoistumiseenHyvaksyttavaTuntimaara ?: 0.0
                        },
                        opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
                        opintosuoritukset.asSequence()
                            .filter { opintosuoritus -> opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO }
                            .sumOf { johtamisopinto ->
                                johtamisopinto.opintopisteet ?: 0.0
                            },
                        opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
                        opintosuoritukset.asSequence()
                            .filter { opintosuoritus -> opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS }
                            .sumOf { sateilysuojakoulutus ->
                                sateilysuojakoulutus.opintopisteet ?: 0.0
                            },
                        opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
                        opintosuoritukset.count { opintosuoritus ->
                            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU && opintosuoritus.hyvaksytty
                        }
                    )
                }
            }
        }
        return null
    }

    private fun getErikoistujanEteneminen(opintooikeus: Opintooikeus): ErikoistujanEteneminenDTO {
        val eteneminen = ErikoistujanEteneminenDTO()

        // Erikoistujan tiedot
        eteneminen.erikoistuvaLaakariId = opintooikeus.erikoistuvaLaakari?.id
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
        val suoritusarvioinnit =
            suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeus.id!!)
                .filter { arviointi -> arviointi.arviointiasteikonTaso != null }
                .groupBy { arviointi -> arviointi.arvioitavaKokonaisuus }
                .mapValues { it.value.maxOf { arviointi -> arviointi.arviointiasteikonTaso!! } }
        if (suoritusarvioinnit.isNotEmpty()) {
            eteneminen.arviointienKa =
                suoritusarvioinnit.values.sumOf { it } / suoritusarvioinnit.keys.size.toDouble()
        }
        eteneminen.arviointienLkm = suoritusarvioinnit.keys.size
        eteneminen.arvioitavienKokonaisuuksienLkm =
            arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
                opintooikeus.erikoisala?.id,
                opintooikeus.osaamisenArvioinninOppaanPvm!!
            ).size

        // Seurantajaksot
        val seurantajaksot = seurantajaksoRepository.findByOpintooikeusId(opintooikeus.id!!)
        eteneminen.seurantajaksotLkm = seurantajaksot.size
        eteneminen.seurantajaksonHuoletLkm =
            seurantajaksot.filter { jakso -> jakso.huolenaiheet != null }.size

        // Suoritemerkinnät
        val suoritemerkinnat =
            suoritemerkintaRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeus.id!!)
                .groupBy { merkinta -> merkinta.suorite }
        eteneminen.suoritemerkinnatLkm = 0

        // Jos yhden suoritteen kaikki vaaditut on merkitty, ei huomioida yli meneviä.
        // Jos kokonaismäärää ei ole tiedossa, näytetään merkintöjen määrä.
        suoritemerkinnat.forEach { suorite ->
            val merkinnat =
                if (suorite.key?.vaadittulkm != null && suorite.value.size > suorite.key?.vaadittulkm!!) suorite.key?.vaadittulkm!! else suorite.value.size
            eteneminen.suoritemerkinnatLkm = eteneminen.suoritemerkinnatLkm?.plus(merkinnat)
        }
        val suoritteenKategoriat = suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(
            opintooikeus.erikoisala?.id,
            opintooikeus.osaamisenArvioinninOppaanPvm!!
        )
        eteneminen.vaaditutSuoritemerkinnatLkm =
            suoritteenKategoriat.sumOf { kategoria ->
                kategoria.suoritteet.filter { suorite -> suorite.vaadittulkm != null }
                    .sumOf { suorite -> suorite.vaadittulkm!! }
            }

        eteneminen.koejaksoTila = getKoejaksoTila(opintooikeus)

        return eteneminen
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
