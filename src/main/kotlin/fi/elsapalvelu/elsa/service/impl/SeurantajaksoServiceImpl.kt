package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import fi.elsapalvelu.elsa.service.mapper.SeurantajaksoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class SeurantajaksoServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val seurantajaksoMapper: SeurantajaksoMapper,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val koulutusjaksoRepository: KoulutusjaksoRepository,
    private val mailService: MailService,
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritemerkintaService: SuoritemerkintaService,
    private val koulutusjaksoService: KoulutusjaksoService,
    private val teoriakoulutusService: TeoriakoulutusService
) : SeurantajaksoService {

    override fun create(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                ?: throw EntityNotFoundException("Vain erikoistuva lääkäri saa lisätä seurantajakson")
        var seurantajakso = seurantajaksoMapper.toEntity(seurantajaksoDTO)
        seurantajakso.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        seurantajakso = seurantajaksoRepository.save(seurantajakso)

        // Lukitaan seurantajaksoon liittyvät tiedot
        val arvioinnit =
            suoritusarviointiRepository.findForSeurantajakso(
                userId,
                seurantajakso.alkamispaiva!!,
                seurantajakso.paattymispaiva!!
            )
        arvioinnit.forEach { it.lukittu = true }
        suoritusarviointiRepository.saveAll(arvioinnit)

        val suoritemerkinnat =
            suoritemerkintaRepository.findForSeurantajakso(
                userId,
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

        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
            "uusiSeurantajakso.html",
            "email.uusiseurantajakso.title",
            properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
        )

        return seurantajaksoMapper.toDto(seurantajakso)
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

        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId).let {
            if (it == seurantajakso.erikoistuvaLaakari
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
                    "seurantajaksonYhteisetMerkinnat.html",
                    "email.seurantajaksonyhteisetmerkinnat.title",
                    properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                )
            }
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

            if (seurantajakso.korjausehdotus != null) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(seurantajakso.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "seurantajaksoPalautettu.html",
                    "email.seurantajaksopalautettu.title",
                    properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                )
            } else if (seurantajakso.hyvaksytty == true) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(seurantajakso.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "seurantajaksoHyvaksytty.html",
                    "email.seurantajaksohyvaksytty.title",
                    properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                )
            } else {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(seurantajakso.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "seurantajaksoArvioitu.html",
                    "email.seurantajaksoarvioitu.title",
                    properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
                )
            }
        }

        return seurantajaksoMapper.toDto(seurantajakso)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): List<SeurantajaksoDTO> {
        val result = seurantajaksoRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
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
        userId: String
    ): SeurantajaksoDTO? {
        val result = seurantajaksoRepository
            .findByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)?.let {
                seurantajaksoMapper.toDto(it)
            }
        if (result != null) {
            result.tila = SeurantajaksoTila.fromSeurantajakso(result)
        }
        return result
    }

    override fun findSeurantajaksonTiedot(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate,
        koulutusjaksot: List<Long>
    ): SeurantajaksonTiedotDTO {
        val arvioinnit =
            suoritusarviointiService.findForSeurantajakso(userId, alkamispaiva, paattymispaiva)
        val arvioitavatKokonaisuudetMap = arvioinnit.groupBy { it.arvioitavaOsaalue }
        val arvioitavatKategoriatMap = arvioitavatKokonaisuudetMap.keys.groupBy { it?.kategoria }
        val kategoriat = arvioitavatKategoriatMap.map { (kategoria, kokonaisuudet) ->
            SeurantajaksonArviointiKategoriaDTO(
                kategoria?.nimi,
                kategoria?.jarjestysnumero,
                kokonaisuudet.map {
                    SeurantajaksonArviointiKokonaisuusDTO(
                        it?.nimi,
                        arvioitavatKokonaisuudetMap[it]
                    )
                })
        }.sortedBy { it.jarjestysnumero }

        val suoritemerkinnat =
            suoritemerkintaService.findForSeurantajakso(userId, alkamispaiva, paattymispaiva)
        val oppimistavoitteetMap = suoritemerkinnat.groupBy { it.oppimistavoite }
        val oppimistavoitteet = oppimistavoitteetMap.map { (tavoite, suoritemerkinnat) ->
            SeurantajaksonSuoritemerkintaDTO(tavoite?.nimi, suoritemerkinnat)
        }

        val koulutusjaksotDTO = koulutusjaksoService.findForSeurantajakso(koulutusjaksot, userId)
        val osaamistavoitteet =
            koulutusjaksotDTO.map { jakso -> jakso.osaamistavoitteet.map { it.nimi } }.flatten()
                .distinct()
        val muutTavoitteet =
            koulutusjaksotDTO.mapNotNull { jakso -> jakso.muutOsaamistavoitteet }
                .distinct()
        val teoriakoulutukset =
            teoriakoulutusService.findForSeurantajakso(userId, alkamispaiva, paattymispaiva)
        return SeurantajaksonTiedotDTO(
            osaamistavoitteet = osaamistavoitteet,
            muutOsaamistavoitteet = muutTavoitteet,
            arvioinnit = kategoriat,
            arviointienMaara = arvioinnit.size,
            suoritemerkinnat = oppimistavoitteet,
            suoritemerkinnatMaara = suoritemerkinnat.size,
            teoriakoulutukset = teoriakoulutukset
        )
    }

    override fun findSeurantajaksonTiedot(id: Long, userId: String): SeurantajaksonTiedotDTO {
        val seurantajakso = seurantajaksoRepository.findByIdAndKouluttajaUserId(id, userId)

        return findSeurantajaksonTiedot(
            seurantajakso?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
            seurantajakso.alkamispaiva!!,
            seurantajakso.paattymispaiva!!,
            seurantajakso.koulutusjaksot?.map { it.id!! }.orEmpty()
        )
    }

    override fun delete(id: Long, userId: String) {
        val seurantajakso =
            seurantajaksoRepository.findByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)

        if (seurantajakso != null) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
                "seurantajaksoPoistettu.html",
                "email.seurantajaksopoistettu.title",
                properties = mapOf(
                    Pair(
                        MailProperty.NAME,
                        seurantajakso.erikoistuvaLaakari?.kayttaja?.user?.getName().toString()
                    )
                )
            )

            // Poistetaan lukot jos ei päällekkäisiä seurantajaksoja
            val seurantajaksot =
                seurantajaksoRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
                    .filter { it.id != seurantajakso.id }
            val arvioinnit =
                suoritusarviointiRepository.findForSeurantajakso(
                    userId,
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
                userId, seurantajakso.alkamispaiva!!, seurantajakso.paattymispaiva!!
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
