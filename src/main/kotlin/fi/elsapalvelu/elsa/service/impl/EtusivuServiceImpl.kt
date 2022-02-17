package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.ErikoistujanEteneminenDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaErikoisalatPerYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
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
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository
) : EtusivuService {

    override fun getErikoistujienSeurantaForVastuuhenkilo(userId: String): ErikoistujienSeurantaDTO {
        val kayttaja: Kayttaja? = kayttajaRepository.findOneByUserId(userId).orElse(null)
        val seurantaDTO = ErikoistujienSeurantaDTO()

        kayttaja?.let {
            seurantaDTO.kayttajaYliopistoErikoisalat =
                kayttaja.yliopistotAndErikoisalat.groupBy { it.yliopisto }.map {
                    KayttajaErikoisalatPerYliopistoDTO(
                        yliopistoNimi = it.key?.nimi,
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

        // Koejakso
        val koulutussopimus =
            koejaksonKoulutussopimusRepository.findByOpintooikeusId(opintooikeus.id!!)
        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findByOpintooikeusId(opintooikeus.id!!)
        val aloituskeskustelu =
            koejaksonAloituskeskusteluRepository.findByOpintooikeusId(opintooikeus.id!!)

        if (vastuuhenkilonArvio.isPresent && vastuuhenkilonArvio.get().koejaksoHyvaksytty == true) {
            eteneminen.koejaksoTila = KoejaksoTila.HYVAKSYTTY
        } else if ((koulutussopimus.isPresent && koulutussopimus.get().koejaksonAlkamispaiva?.isAfter(
                LocalDate.now()
            ) != true) || (aloituskeskustelu.isPresent && aloituskeskustelu.get().koejaksonAlkamispaiva?.isAfter(
                LocalDate.now()
            ) != true)
        ) {
            eteneminen.koejaksoTila = KoejaksoTila.ODOTTAA_HYVAKSYNTAA
        } else {
            eteneminen.koejaksoTila = KoejaksoTila.EI_AKTIIVINEN
        }

        return eteneminen
    }
}
