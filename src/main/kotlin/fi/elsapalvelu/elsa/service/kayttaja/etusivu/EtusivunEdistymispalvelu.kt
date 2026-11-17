package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusRepository
import fi.elsapalvelu.elsa.repository.koulutus.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistujanEteneminenDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistujanEteneminenVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistumisenEdistyminenDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KoulutettavanEteneminenDTO
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArviointiasteikkoMapper
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivunEdistymispalvelu(
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val arviointiasteikkoMapper: ArviointiasteikkoMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintooikeusService: OpintooikeusService,
    private val osaamisenYhteenvetopalvelu: OsaamisenYhteenvetopalvelu,
    private val koejaksopalvelu: EtusivunKoejaksopalvelu
) {

    fun muodostaErikoistujanEdistyminen(opintooikeus: Opintooikeus): ErikoistujanEteneminenDTO {
        val eteneminen = ErikoistujanEteneminenDTO()

        // Erikoistujan tiedot
        eteneminen.opintooikeusId = opintooikeus.id
        eteneminen.erikoistuvaLaakariEtuNimi = opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName
        eteneminen.erikoistuvaLaakariSukuNimi = opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName
        eteneminen.erikoistuvaLaakariSyntymaaika = opintooikeus.erikoistuvaLaakari?.syntymaaika
        eteneminen.opintooikeudenMyontamispaiva = opintooikeus.opintooikeudenMyontamispaiva
        eteneminen.opintooikeudenPaattymispaiva = opintooikeus.opintooikeudenPaattymispaiva
        eteneminen.asetus = opintooikeus.asetus?.nimi
        eteneminen.erikoisala = opintooikeus.erikoisala?.nimi
        eteneminen.laakarikoulutusSuoritettuSuomiTaiBelgia = opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuSuomiTaiBelgia
        eteneminen.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia = opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia

        // Työskentelyjaksot
        eteneminen.tyoskentelyjaksoTilastot = tyoskentelyjaksoService.getTilastot(opintooikeus)

        // Suoritusarvioinnit
        val kokonaisuuksienArviot = osaamisenYhteenvetopalvelu.haeKokonaisuuksienParhaatArviot(opintooikeus.id.required())
        eteneminen.arviointienKeskiarvo = osaamisenYhteenvetopalvelu.laskeArviointienKeskiarvo(kokonaisuuksienArviot)
        eteneminen.arviointienLkm = kokonaisuuksienArviot.keys.size
        eteneminen.arvioitavienKokonaisuuksienLkm = osaamisenYhteenvetopalvelu.laskeArvioitavatKokonaisuudet(opintooikeus, kokonaisuuksienArviot.keys)

        // Seurantajaksot
        val seurantajaksot = seurantajaksoRepository.findByOpintooikeusId(opintooikeus.id.required())
        eteneminen.seurantajaksotLkm = seurantajaksot.size
        eteneminen.seurantajaksonHuoletLkm = seurantajaksot.count { jakso -> jakso.huolenaiheet != null }

        // Suoritemerkinnät
        val suoritemerkinnatSuoritteittain = osaamisenYhteenvetopalvelu.haeSuoritemerkinnat(opintooikeus.id.required())
        eteneminen.suoritemerkinnatLkm = osaamisenYhteenvetopalvelu.laskeSuoritemerkinnat(suoritemerkinnatSuoritteittain)
        eteneminen.vaaditutSuoritemerkinnatLkm = osaamisenYhteenvetopalvelu.laskeVaaditutSuoritemerkinnat(opintooikeus, suoritemerkinnatSuoritteittain.values.flatten())

        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id.required()).asSequence()
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
            opintooikeus.erikoistuvaLaakari?.id.required(),
            YEK_ERIKOISALA_ID
        )
        eteneminen.koejaksoTila = koejaksopalvelu.haeKoejaksonTila(opintooikeus, opintosuoritukset)
        eteneminen.terveyskeskuskoulutusjaksoSuoritettu = onkoTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset + yekSuoritukset)

        return eteneminen
    }

    fun haeErikoistujanEdistyminen(kayttajatunnus: String): ErikoistumisenEdistyminenDTO? {
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(kayttajatunnus)
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId).orElse(null)
        val kokonaisuuksienArviot = osaamisenYhteenvetopalvelu.haeKokonaisuuksienParhaatArviot(opintooikeus.id.required())
        val arvioidutKokonaisuudet = kokonaisuuksienArviot.keys
        val suoritemerkinnatSuoritteittain = osaamisenYhteenvetopalvelu.haeSuoritemerkinnat(opintooikeus.id.required())
        val suoritemerkinnat = suoritemerkinnatSuoritteittain.values.flatten()
        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id.required()).asSequence()
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(opintooikeus.erikoistuvaLaakari?.id.required(), YEK_ERIKOISALA_ID)
        val arviointiasteikko = opintooikeus.opintoopas?.arviointiasteikko?.let { asteikko ->
            arviointiasteikkoMapper.toDto(asteikko)
        }

        return ErikoistumisenEdistyminenDTO(
            arviointienKeskiarvo = osaamisenYhteenvetopalvelu.laskeArviointienKeskiarvo(kokonaisuuksienArviot),
            arvioitavatKokonaisuudetVahintaanYksiArvioLkm = kokonaisuuksienArviot.keys.size,
            arvioitavienKokonaisuuksienLkm = osaamisenYhteenvetopalvelu.laskeArvioitavatKokonaisuudet(opintooikeus, arvioidutKokonaisuudet),
            arviointiasteikko = arviointiasteikko,
            suoritemerkinnatLkm = osaamisenYhteenvetopalvelu.laskeSuoritemerkinnat(suoritemerkinnatSuoritteittain),
            vaaditutSuoritemerkinnatLkm = osaamisenYhteenvetopalvelu.laskeVaaditutSuoritemerkinnat(opintooikeus, suoritemerkinnat),
            osaalueetSuoritettuLkm = osaamisenYhteenvetopalvelu.laskeSuoritetutOsaalueet(suoritemerkinnatSuoritteittain, opintooikeus.osaamisenArvioinninOppaanPvm.required()),
            osaalueetVaadittuLkm = osaamisenYhteenvetopalvelu.laskeVaaditutOsaalueet(opintooikeus, suoritemerkinnatSuoritteittain),
            tyoskentelyjaksoTilastot = tyoskentelyjaksoService.getTilastot(opintooikeus),
            teoriakoulutuksetSuoritettu = laskeTeoriakoulutustenTunnit(opintooikeus.id.required(), opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID,
                opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara),
            teoriakoulutuksetVaadittu = opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
            johtamisopinnotSuoritettu = laskeJohtamisopintojenOpintopisteet(opintosuoritukset),
            johtamisopinnotVaadittu = opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
            sateilysuojakoulutuksetSuoritettu = laskeSateilysuojakoulutustenOpintopisteet(opintosuoritukset),
            sateilysuojakoulutuksetVaadittu = opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
            koejaksoTila = koejaksopalvelu.haeKoejaksonTila(opintooikeus, opintosuoritukset),
            koejaksonSuoritusmerkintaExists = koejaksopalvelu.onkoKoejaksonSuoritusmerkinta(opintosuoritukset),
            valtakunnallisetKuulustelutSuoritettuLkm = laskeHyvaksytytKuulustelut(opintosuoritukset),
            opintooikeudenMyontamispaiva = opintooikeus.opintooikeudenMyontamispaiva,
            opintooikeudenPaattymispaiva = opintooikeus.opintooikeudenPaattymispaiva,
            terveyskeskuskoulutusjaksoSuoritettu = onkoTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset + yekSuoritukset),
            laakarikoulutusSuoritettuSuomiTaiBelgia = opintooikeus.erikoistuvaLaakari?.laakarikoulutusSuoritettuSuomiTaiBelgia,
            yekSuoritusPvm = yekSuoritukset.firstOrNull { suoritus -> suoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_PATEVYYS }?.suorituspaiva
        )
    }

    fun muodostaKoulutettavanEdistyminen(opintooikeus: Opintooikeus): KoulutettavanEteneminenDTO {
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(opintooikeus.erikoistuvaLaakari?.id.required(), YEK_ERIKOISALA_ID)
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

    fun muodostaVirkailijanEdistyminen(opintooikeus: Opintooikeus): ErikoistujanEteneminenVirkailijaDTO {
        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeus.id.required()).asSequence()
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(opintooikeus.erikoistuvaLaakari?.id.required(), YEK_ERIKOISALA_ID)
        return ErikoistujanEteneminenVirkailijaDTO(
            opintooikeusId = opintooikeus.id,
            etunimi = opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName,
            sukunimi = opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName,
            syntymaaika = opintooikeus.erikoistuvaLaakari?.syntymaaika,
            erikoisala = opintooikeus.erikoisala?.nimi,
            asetus = opintooikeus.asetus?.nimi,
            koejaksoTila = koejaksopalvelu.haeKoejaksonTila(opintooikeus, opintosuoritukset),
            opintooikeudenMyontamispaiva = opintooikeus.opintooikeudenMyontamispaiva,
            opintooikeudenPaattymispaiva = opintooikeus.opintooikeudenPaattymispaiva,
            tyoskentelyjaksoTilastot = tyoskentelyjaksoService.getTilastot(opintooikeus),
            teoriakoulutuksetSuoritettu = laskeTeoriakoulutustenTunnit(opintooikeus.id.required(), opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID,
                opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara),
            teoriakoulutuksetVaadittu = opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara,
            johtamisopinnotSuoritettu = laskeJohtamisopintojenOpintopisteet(opintosuoritukset),
            johtamisopinnotVaadittu = opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
            sateilysuojakoulutuksetSuoritettu = laskeSateilysuojakoulutustenOpintopisteet(opintosuoritukset),
            sateilysuojakoulutuksetVaadittu = opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara,
            valtakunnallisetKuulustelutSuoritettuLkm = laskeHyvaksytytKuulustelut(opintosuoritukset),
            terveyskeskuskoulutusjaksoSuoritettu = onkoTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset + yekSuoritukset)
        )
    }

    private fun laskeTeoriakoulutustenTunnit(opintooikeusId: Long, yek: Boolean, vaadittu: Double?): Double =
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

    private fun laskeJohtamisopintojenOpintopisteet(opintosuoritukset: Sequence<Opintosuoritus>): Double {
        return opintosuoritukset.filter { opintosuoritus ->
            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
        }.sumOf { johtamisopinto ->
            johtamisopinto.opintopisteet ?: 0.0
        }
    }

    private fun laskeSateilysuojakoulutustenOpintopisteet(opintosuoritukset: Sequence<Opintosuoritus>): Double {
        return opintosuoritukset.filter { opintosuoritus ->
            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS
        }.sumOf { sateilysuojakoulutus ->
            sateilysuojakoulutus.opintopisteet ?: 0.0
        }
    }

    private fun laskeHyvaksytytKuulustelut(opintosuoritukset: Sequence<Opintosuoritus>): Int =
        opintosuoritukset.count { opintosuoritus ->
            opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU && opintosuoritus.hyvaksytty
        }

    private fun onkoTerveyskeskuskoulutusjaksoSuoritettu(opintosuoritukset: Sequence<Opintosuoritus>): Boolean {
        return opintosuoritukset.any {
            (it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO || it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO) && it.hyvaksytty
        }
    }
}
