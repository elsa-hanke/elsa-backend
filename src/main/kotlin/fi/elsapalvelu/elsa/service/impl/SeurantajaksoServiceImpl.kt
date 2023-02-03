package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import fi.elsapalvelu.elsa.service.mapper.ArviointiasteikkoMapper
import fi.elsapalvelu.elsa.service.mapper.SeurantajaksoMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class SeurantajaksoServiceImpl(
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val seurantajaksoMapper: SeurantajaksoMapper,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val koulutusjaksoRepository: KoulutusjaksoRepository,
    private val mailService: MailService,
    private val suoritemerkintaService: SuoritemerkintaService,
    private val koulutusjaksoService: KoulutusjaksoService,
    private val teoriakoulutusService: TeoriakoulutusService,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService,
    private val arviointiasteikkoMapper: ArviointiasteikkoMapper
) : SeurantajaksoService {

    override fun create(
        seurantajaksoDTO: SeurantajaksoDTO,
        opintooikeusId: Long
    ): SeurantajaksoDTO? {
        opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let { opintooikeus ->
            var seurantajakso = seurantajaksoMapper.toEntity(seurantajaksoDTO)
            seurantajakso.opintooikeus = opintooikeus
            seurantajakso = seurantajaksoRepository.save(seurantajakso)

            // Lukitaan seurantajaksoon liittyvät tiedot
            val arvioinnit =
                suoritusarviointiRepository.findForSeurantajakso(
                    opintooikeusId,
                    seurantajakso.alkamispaiva!!,
                    seurantajakso.paattymispaiva!!
                )
            arvioinnit.forEach { it.lukittu = true }
            suoritusarviointiRepository.saveAll(arvioinnit)

            val suoritemerkinnat =
                suoritemerkintaRepository.findForSeurantajakso(
                    opintooikeusId,
                    seurantajakso.alkamispaiva!!,
                    seurantajakso.paattymispaiva!!
                )
            suoritemerkinnat.forEach { it.lukittu = true }
            suoritemerkintaRepository.saveAll(suoritemerkinnat)

            val koulutusjaksot = seurantajakso.koulutusjaksot
            if (koulutusjaksot != null && koulutusjaksot.size > 0) {
                koulutusjaksot.forEach { it.lukittu = true }
                koulutusjaksoRepository.saveAll(koulutusjaksot)
            }

            kouluttajavaltuutusService.lisaaValtuutus(
                opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                seurantajakso.kouluttaja?.id!!
            )

            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
                templateName = "uusiSeurantajakso.html",
                titleKey = "email.uusiseurantajakso.title",
                properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
            )

            return seurantajaksoMapper.toDto(seurantajakso)
        }

        return null
    }

    override fun update(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO {
        var seurantajakso =
            seurantajaksoRepository.findById(seurantajaksoDTO.id!!)
                .orElseThrow { EntityNotFoundException("Seurantajaksoa ei löydy") }

        val updatedSeurantajakso =
            seurantajaksoMapper.toEntity(seurantajaksoDTO)

        if (seurantajakso.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id == userId
            && (seurantajakso.seurantakeskustelunYhteisetMerkinnat == null
                || seurantajakso.korjausehdotus != null)
        ) {
            seurantajakso.omaArviointi = updatedSeurantajakso.omaArviointi
            seurantajakso.lisahuomioita = updatedSeurantajakso.lisahuomioita
            seurantajakso.seuraavanJaksonTavoitteet =
                updatedSeurantajakso.seuraavanJaksonTavoitteet
            seurantajakso.seurantakeskustelunYhteisetMerkinnat =
                updatedSeurantajakso.seurantakeskustelunYhteisetMerkinnat
            seurantajakso.seuraavanKeskustelunAjankohta =
                updatedSeurantajakso.seuraavanKeskustelunAjankohta
            seurantajakso.korjausehdotus = null
            seurantajakso = seurantajaksoRepository.save(seurantajakso)

            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
                templateName = "seurantajaksonYhteisetMerkinnat.html",
                titleKey = "email.seurantajaksonyhteisetmerkinnat.title",
                properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
            )
        }

        if (seurantajakso.kouluttaja?.user?.id == userId && seurantajakso.hyvaksytty != true) {
            if (updatedSeurantajakso.korjausehdotus != null) {
                seurantajakso.korjausehdotus = updatedSeurantajakso.korjausehdotus
            } else {
                seurantajakso.edistyminenTavoitteidenMukaista =
                    updatedSeurantajakso.edistyminenTavoitteidenMukaista
                seurantajakso.huolenaiheet = updatedSeurantajakso.huolenaiheet
                seurantajakso.kouluttajanArvio = updatedSeurantajakso.kouluttajanArvio
                seurantajakso.erikoisalanTyoskentelyvalmiudet =
                    updatedSeurantajakso.erikoisalanTyoskentelyvalmiudet
                seurantajakso.jatkotoimetJaRaportointi =
                    updatedSeurantajakso.jatkotoimetJaRaportointi

                if (seurantajakso.seurantakeskustelunYhteisetMerkinnat != null && seurantajakso.korjausehdotus == null) {
                    seurantajakso.hyvaksytty = true
                }
                if (seurantajakso.edistyminenTavoitteidenMukaista == true) {
                    seurantajakso.huolenaiheet = null
                }
            }

            seurantajakso = seurantajaksoRepository.save(seurantajakso)

            when {
                seurantajakso.korjausehdotus != null -> {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(seurantajakso.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                            .get().user!!,
                        templateName = "seurantajaksoPalautettu.html",
                        titleKey = "email.seurantajaksopalautettu.title",
                        properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                    )
                }
                seurantajakso.hyvaksytty == true -> {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(seurantajakso.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                            .get().user!!,
                        templateName = "seurantajaksoHyvaksytty.html",
                        titleKey = "email.seurantajaksohyvaksytty.title",
                        properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                    )
                }
                else -> {
                    mailService.sendEmailFromTemplate(
                        kayttajaRepository.findById(seurantajakso.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                            .get().user!!,
                        templateName = "seurantajaksoArvioitu.html",
                        titleKey = "email.seurantajaksoarvioitu.title",
                        properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                    )
                }
            }
        }

        return seurantajaksoMapper.toDto(seurantajakso)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): List<SeurantajaksoDTO> {
        val result = seurantajaksoRepository.findByOpintooikeusId(opintooikeusId)
            .map(seurantajaksoMapper::toDto)
        result.forEach { it.tila = SeurantajaksoTila.fromSeurantajakso(it) }
        return result
    }

    @Transactional(readOnly = true)
    override fun findByKouluttajaUserId(userId: String): List<SeurantajaksoDTO> {
        val result = seurantajaksoRepository.findByKouluttajaUserId(userId)
            .map(seurantajaksoMapper::toDto)
        result.forEach { it.tila = SeurantajaksoTila.fromSeurantajakso(it) }
        return result
    }

    override fun findAvoinByKouluttajaUserId(userId: String): List<EtusivuSeurantajaksoDTO> {
        val result = seurantajaksoRepository.findAvoinByKouluttajaUserId(userId)
            .map { sj ->
                EtusivuSeurantajaksoDTO(
                    id = sj.id,
                    erikoistujanNimi = sj.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi(),
                    tallennettu = sj.tallennettu,
                    alkamispaiva = sj.alkamispaiva,
                    paattymispaiva = sj.paattymispaiva,
                    tila = SeurantajaksoTila.fromSeurantajakso(sj)
                )
            }
        return result
    }

    @Transactional(readOnly = true)
    override fun findByIdAndKouluttajaUserId(id: Long, userId: String): SeurantajaksoDTO? {
        val result = seurantajaksoRepository
            .findByIdAndKouluttajaUserId(id, userId)?.let {
                seurantajaksoMapper.toDto(it)
            }
        if (result != null) {
            result.tila = SeurantajaksoTila.fromSeurantajakso(result)
        }
        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long,
        opintooikeusId: Long
    ): SeurantajaksoDTO? {
        val result = seurantajaksoRepository
            .findByIdAndOpintooikeusId(id, opintooikeusId)?.let {
                seurantajaksoMapper.toDto(it)
            }
        if (result != null) {
            result.tila = SeurantajaksoTila.fromSeurantajakso(result)
        }
        return result
    }

    override fun findSeurantajaksonTiedot(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate,
        koulutusjaksot: List<Long>
    ): SeurantajaksonTiedotDTO {
        val arvioinnit =
            suoritusarviointiRepository.findForSeurantajakso(
                opintooikeusId,
                alkamispaiva,
                paattymispaiva
            )
        val arvioitavatKokonaisuudetMap =
            arvioinnit.flatMap { it.arvioitavatKokonaisuudet }.groupBy { it.arvioitavaKokonaisuus }
        val arvioitavatKategoriatMap = arvioitavatKokonaisuudetMap.keys.groupBy { it?.kategoria }
        val kategoriat = arvioitavatKategoriatMap.map { (kategoria, kokonaisuudet) ->
            SeurantajaksonArviointiKategoriaDTO(
                kategoria?.nimi,
                kategoria?.jarjestysnumero,
                kokonaisuudet.map {
                    SeurantajaksonArviointiKokonaisuusDTO(
                        it?.nimi,
                        arvioitavatKokonaisuudetMap[it]?.map { kokonaisuus ->
                            SeurantajaksonArviointiDTO(
                                arvioitavaTapahtuma = kokonaisuus.suoritusarviointi?.arvioitavaTapahtuma,
                                arviointiasteikonTaso = kokonaisuus.arviointiasteikonTaso,
                                tapahtumanAjankohta = kokonaisuus.suoritusarviointi?.tapahtumanAjankohta,
                                arviointiasteikko = arviointiasteikkoMapper.toDto(kokonaisuus.suoritusarviointi?.arviointiasteikko!!),
                                suoritusarviointiId = kokonaisuus.suoritusarviointi?.id
                            )
                        }
                    )
                })
        }.sortedBy { it.jarjestysnumero }

        val suoritemerkinnat =
            suoritemerkintaService.findForSeurantajakso(
                opintooikeusId,
                alkamispaiva,
                paattymispaiva
            )
        val suoritteetMap = suoritemerkinnat.groupBy { it.suorite }
        val suoritteet = suoritteetMap.map { (tavoite, suoritemerkinnat) ->
            SeurantajaksonSuoritemerkintaDTO(tavoite?.nimi, suoritemerkinnat)
        }

        val koulutusjaksotDTO =
            koulutusjaksoService.findForSeurantajakso(koulutusjaksot, opintooikeusId)
        val osaamistavoitteet =
            koulutusjaksotDTO.map { jakso -> jakso.osaamistavoitteet.map { it.nimi } }.flatten()
                .distinct()
        val muutTavoitteet =
            koulutusjaksotDTO.mapNotNull { jakso -> jakso.muutOsaamistavoitteet }
                .distinct()
        val teoriakoulutukset =
            teoriakoulutusService.findForSeurantajakso(opintooikeusId, alkamispaiva, paattymispaiva)
        return SeurantajaksonTiedotDTO(
            osaamistavoitteet = osaamistavoitteet,
            muutOsaamistavoitteet = muutTavoitteet,
            arvioinnit = kategoriat,
            arviointienMaara = arvioinnit.size,
            suoritemerkinnat = suoritteet,
            suoritemerkinnatMaara = suoritemerkinnat.size,
            teoriakoulutukset = teoriakoulutukset
        )
    }

    override fun findSeurantajaksonTiedot(id: Long, userId: String): SeurantajaksonTiedotDTO {
        val seurantajakso = seurantajaksoRepository.findByIdAndKouluttajaUserId(id, userId)

        return findSeurantajaksonTiedot(
            seurantajakso?.opintooikeus?.id!!,
            seurantajakso.alkamispaiva!!,
            seurantajakso.paattymispaiva!!,
            seurantajakso.koulutusjaksot?.map { it.id!! }.orEmpty()
        )
    }

    override fun delete(id: Long, opintooikeusId: Long) {
        val seurantajakso =
            seurantajaksoRepository.findByIdAndOpintooikeusId(id, opintooikeusId)

        if (seurantajakso != null) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
                templateName = "seurantajaksoPoistettu.html",
                titleKey = "email.seurantajaksopoistettu.title",
                properties = mapOf(
                    Pair(
                        MailProperty.NAME,
                        seurantajakso.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
                            .toString()
                    )
                )
            )

            // Poistetaan lukot jos ei päällekkäisiä seurantajaksoja
            val seurantajaksot =
                seurantajaksoRepository.findByOpintooikeusId(opintooikeusId)
                    .filter { it.id != seurantajakso.id }
            val arvioinnit =
                suoritusarviointiRepository.findForSeurantajakso(
                    opintooikeusId,
                    seurantajakso.alkamispaiva!!,
                    seurantajakso.paattymispaiva!!
                )
            arvioinnit.forEach { arviointi ->
                if (!onkoSeurantajaksolla(seurantajaksot, arviointi.tapahtumanAjankohta!!)) {
                    arviointi.lukittu = false
                }
            }
            suoritusarviointiRepository.saveAll(arvioinnit)

            val suoritemerkinnat = suoritemerkintaRepository.findForSeurantajakso(
                opintooikeusId, seurantajakso.alkamispaiva!!, seurantajakso.paattymispaiva!!
            )
            suoritemerkinnat.forEach { suoritemerkinta ->
                if (!onkoSeurantajaksolla(seurantajaksot, suoritemerkinta.suorituspaiva!!)) {
                    suoritemerkinta.lukittu = false
                }
            }
            suoritemerkintaRepository.saveAll(suoritemerkinnat)

            val koulutusjaksot = seurantajakso.koulutusjaksot
            if (koulutusjaksot != null && koulutusjaksot.size > 0) {
                koulutusjaksot.forEach { koulutusjakso ->
                    if (seurantajaksot.none { it.koulutusjaksot?.contains(koulutusjakso) == true }) {
                        koulutusjakso.lukittu = false
                    }
                }
                koulutusjaksoRepository.saveAll(koulutusjaksot)
            }

            seurantajaksoRepository.delete(seurantajakso)
        }
    }

    private fun onkoSeurantajaksolla(seurantajaksot: List<Seurantajakso>, pvm: LocalDate): Boolean {
        seurantajaksot.forEach {
            if (pvm.isAfter(it.alkamispaiva) && pvm.isBefore(
                    it.paattymispaiva
                )
            ) {
                return true
            }
        }
        return false
    }
}
