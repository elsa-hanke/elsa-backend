package fi.elsapalvelu.elsa.service.impl.arviointi

import fi.elsapalvelu.elsa.required

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArviointityokalunVastaus
import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.repository.koejakso.*
import fi.elsapalvelu.elsa.repository.tyoskentely.*
import fi.elsapalvelu.elsa.repository.arviointi.*
import fi.elsapalvelu.elsa.repository.suoritteet.*
import fi.elsapalvelu.elsa.repository.koulutus.*
import fi.elsapalvelu.elsa.repository.seuranta.*
import fi.elsapalvelu.elsa.repository.valmistuminen.*
import fi.elsapalvelu.elsa.repository.kayttaja.*
import fi.elsapalvelu.elsa.repository.perustiedot.*
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.arviointi.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDataDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarvioinninArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ObjectUtils
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
@Transactional
class SuoritusarviointiServiceImpl(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper,
    private val suoritusarvioinninArvioitavaKokonaisuusMapper: SuoritusarvioinninArvioitavaKokonaisuusMapper,
    private val arviointityokaluRepository: ArviointityokaluRepository,
    private val mailService: MailService,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val asiakirjaService: AsiakirjaService,
    private val asiakirjaMapper: AsiakirjaMapper,
    private val arviointityokaluKysymysRepository: ArviointityokaluKysymysRepository,
    private val arviointityokaluKysymysVaihtoehtoRepository: ArviointityokaluKysymysVaihtoehtoRepository,
    private val pdfTextFieldValidator: PdfTextFieldValidator
) : SuoritusarviointiService {

    override fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO {
        validatePdfText(
            suoritusarviointiDTO,
            listOf("arvioitava-tapahtuma" to suoritusarviointiDTO.arvioitavaTapahtuma)
        )
        var suoritusarviointi = suoritusarviointiMapper.toEntity(suoritusarviointiDTO)
        suoritusarviointi.arvioitavatKokonaisuudet.forEach {
            it.suoritusarviointi = suoritusarviointi
        }
        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(suoritusarviointi.arvioinninAntaja?.id.required()).get().user.required(),
            templateName = "arviointipyyntoKouluttajalleEmail.html",
            titleKey = "email.arviointipyyntokouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id.required().toString()))
        )
        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    override fun save(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?,
        userId: String
    ): SuoritusarviointiDTO {
        var suoritusarviointi =
            suoritusarviointiRepository.findOneById(suoritusarviointiDTO.id.required()).get()

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == suoritusarviointi.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari
        ) {
            suoritusarviointi = handleErikoistuva(
                suoritusarviointiDTO,
                suoritusarviointi,
                newAsiakirjat,
                deletedAsiakirjaIds
            )
        }

        val kirjautunutKayttaja = kayttajaRepository.findOneByUserId(userId)
        if (kirjautunutKayttaja.isPresent && kirjautunutKayttaja.get() == suoritusarviointi.arvioinninAntaja) {
            suoritusarviointi =
                handleKouluttajaOrVastuuhenkilo(
                    suoritusarviointiDTO,
                    suoritusarviointi,
                    newAsiakirjat,
                    deletedAsiakirjaIds
                )
        }

        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    private fun handleErikoistuva(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        suoritusarviointi: Suoritusarviointi,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): Suoritusarviointi {
        val isItsearviointiNotEmpty =
            !ObjectUtils.isEmpty(suoritusarviointiDTO.sanallinenItsearviointi)
        // Itsearvioinnin tekeminen
        if (isItsearviointiNotEmpty) {
            validatePdfText(
                suoritusarviointiDTO,
                listOf(
                    "sanallinen-itsearviointi" to suoritusarviointiDTO.sanallinenItsearviointi,
                    *newAsiakirjat.map { "liitetiedoston-nimi" to it.nimi }.toTypedArray()
                )
            )
            suoritusarviointi.itsearviointiVaativuustaso =
                suoritusarviointiDTO.itsearviointiVaativuustaso
            suoritusarviointi.sanallinenItsearviointi =
                suoritusarviointiDTO.sanallinenItsearviointi
            suoritusarviointi.itsearviointiAika = LocalDate.now(ZoneId.systemDefault())
            suoritusarviointiDTO.arvioitavatKokonaisuudet?.forEach {
                val arvioitavaKokonaisuus =
                    suoritusarviointi.arvioitavatKokonaisuudet.first { k -> k.id == it.id }
                arvioitavaKokonaisuus.itsearviointiArviointiasteikonTaso =
                    it.itsearviointiArviointiasteikonTaso
            }
            mapAsiakirjat(suoritusarviointi, newAsiakirjat, deletedAsiakirjaIds, true)
        } else {
            // Arviointipyynnön muokkaus
            validatePdfText(
                suoritusarviointiDTO,
                listOf("arvioitava-tapahtuma" to suoritusarviointiDTO.arvioitavaTapahtuma)
            )
            suoritusarviointi.arvioitavaTapahtuma = suoritusarviointiDTO.arvioitavaTapahtuma
            suoritusarviointi.lisatiedot = suoritusarviointiDTO.lisatiedot
            suoritusarviointi.tapahtumanAjankohta = suoritusarviointiDTO.tapahtumanAjankohta
            suoritusarviointi.tyoskentelyjakso = suoritusarviointiDTO.tyoskentelyjaksoId
                ?.let { tyoskentelyjaksoRepository.findByIdOrNull(it) }
            val ids =
                suoritusarviointi.arvioitavatKokonaisuudet.map { it.arvioitavaKokonaisuus?.id }
            val dtoIds =
                suoritusarviointiDTO.arvioitavatKokonaisuudet?.map { it.arvioitavaKokonaisuusId }
            val poistetut =
                suoritusarviointi.arvioitavatKokonaisuudet.filter { dtoIds?.contains(it.arvioitavaKokonaisuus?.id) == false }
            suoritusarviointi.arvioitavatKokonaisuudet.removeAll(poistetut)
            val uudet =
                suoritusarviointiDTO.arvioitavatKokonaisuudet?.filter { !ids.contains(it.arvioitavaKokonaisuusId) }
                    ?.map {
                        val result = suoritusarvioinninArvioitavaKokonaisuusMapper.toEntity(it)
                        result.suoritusarviointi = suoritusarviointi
                        result
                    }.orEmpty()
            suoritusarviointi.arvioitavatKokonaisuudet.addAll(uudet)
        }

        return suoritusarviointiRepository.save(suoritusarviointi)
    }

    private fun handleKouluttajaOrVastuuhenkilo(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        suoritusarviointi: Suoritusarviointi,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): Suoritusarviointi {
        validateKouluttajanPdfText(suoritusarviointiDTO, newAsiakirjat)
        suoritusarviointi.vaativuustaso = suoritusarviointiDTO.vaativuustaso
        suoritusarviointi.sanallinenArviointi = suoritusarviointiDTO.sanallinenArviointi
        suoritusarviointi.arviointityokalut = arviointityokaluRepository.findAllByIdIn(
            suoritusarviointiDTO.arviointityokalut?.map(
                ArviointityokaluDTO::id
            ).orEmpty()
        )
        suoritusarviointi.arviointiPerustuu = suoritusarviointiDTO.arviointiPerustuu
        suoritusarviointi.muuPeruste = suoritusarviointiDTO.muuPeruste
        suoritusarviointi.arvioitavatKokonaisuudet.forEach {
            it.arviointiasteikonTaso = suoritusarviointiDTO.arvioitavatKokonaisuudet?.first { k -> k.id == it.id }?.arviointiasteikonTaso
        }

        val existingVastauksetById = suoritusarviointi.arviointityokaluVastaukset.associateBy { it.id }.toMutableMap()
        val dtoVastausIds = suoritusarviointiDTO.arviointityokaluVastaukset.mapNotNull { it.id }.toSet()
        val newOrUpdatedVastaukset = suoritusarviointiDTO.arviointityokaluVastaukset.map { dto ->
            existingVastauksetById[dto.id]?.apply {
                tekstiVastaus = dto.tekstiVastaus
                valittuVaihtoehto = dto.valittuVaihtoehtoId?.let { arviointityokaluKysymysVaihtoehtoRepository.findByIdOrNull(it) }
            } ?: SuoritusarvioinninArviointityokalunVastaus(
                suoritusarviointi = suoritusarviointi,
                arviointityokalu = dto.arviointityokaluId?.let { arviointityokaluRepository.findByIdOrNull(it) },
                arviointityokaluKysymys = dto.arviointityokaluKysymysId?.let { arviointityokaluKysymysRepository.findByIdOrNull(it) },
                tekstiVastaus = dto.tekstiVastaus,
                valittuVaihtoehto = dto.valittuVaihtoehtoId?.let { arviointityokaluKysymysVaihtoehtoRepository.findByIdOrNull(it) }
            )
        }
        suoritusarviointi.arviointityokaluVastaukset.removeIf { it.id !in dtoVastausIds }
        newOrUpdatedVastaukset.forEach { newVastaus ->
            if (!suoritusarviointi.arviointityokaluVastaukset.contains(newVastaus)) {
                suoritusarviointi.arviointityokaluVastaukset.add(newVastaus)
            }
        }

        mapAsiakirjat(suoritusarviointi, newAsiakirjat, deletedAsiakirjaIds, false)

        suoritusarviointi.keskenerainen = suoritusarviointiDTO.keskenerainen
        suoritusarviointi.arviointiAika = if (!suoritusarviointiDTO.keskenerainen) LocalDate.now(ZoneId.systemDefault()) else null

        val result = suoritusarviointiRepository.save(suoritusarviointi)

        if (!suoritusarviointi.keskenerainen) {
            val isNewArviointi = suoritusarviointi.arviointiAika == null
            val templateName = if (isNewArviointi) {
                "arviointiAnnettuEmail"
            } else {
                "arviointiaMuokattuEmail"
            }
            val titleKey = if (isNewArviointi) {
                "email.arviointiannettu.title"
            } else {
                "email.arviointiamuokattu.title"
            }

            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(suoritusarviointi.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id.required())
                    .get().user.required(),
                templateName = templateName,
                titleKey = titleKey,
                properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id.required().toString()))
            )
        }
        return result
    }

    private fun validatePdfText(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        fields: List<Pair<String, String?>>
    ) {
        pdfTextFieldValidator.validate(
            fields = fields,
            pdfSource = "suoritusarviointi",
            sourceId = suoritusarviointiDTO.id,
            sourceDate = suoritusarviointiDTO.tapahtumanAjankohta
        )
    }

    private fun validateKouluttajanPdfText(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        newAsiakirjat: Set<AsiakirjaDTO>
    ) {
        validatePdfText(
            suoritusarviointiDTO,
            listOf(
                "sanallinen-kokonaisarviointi" to suoritusarviointiDTO.sanallinenArviointi,
                "arviointi-perustuu-muu" to suoritusarviointiDTO.muuPeruste,
                *newAsiakirjat.map { "liitetiedoston-nimi" to it.nimi }.toTypedArray()
            )
        )
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoOpintooikeusId(
        opintooikeusId: Long
    ): List<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(
            opintooikeusId
        ).map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndTyoskentelyjaksoOpintooikeusId(
        id: Long,
        opintooikeusId: Long
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(
            id,
            opintooikeusId
        ).map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAsiakirjaBySuoritusarviointiIdAndTyoskentelyjaksoOpintooikeusId(
        id: Long,
        opintooikeusId: Long,
        asiakirjaId: Long
    ): AsiakirjaDTO? {
        asiakirjaRepository.findOneByIdAndOpintooikeusIdAndArviointiId(
            asiakirjaId,
            opintooikeusId,
            id
        )?.let {
            return AsiakirjaDTO(
                id = it.id,
                nimi = it.nimi,
                tyyppi = it.tyyppi,
                asiakirjaData = AsiakirjaDataDTO(fileInputStream = ByteArrayInputStream(it.asiakirjaData?.data))
            )
        }
        asiakirjaService.warnIfDeleted(asiakirjaId)
        return null
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndArvioinninAntajauserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndArvioinninAntajaUserId(id, userId)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAsiakirjaBySuoritusarviointiIdAndArvioinninAntajauserId(
        id: Long,
        userId: String,
        asiakirjaId: Long
    ): AsiakirjaDTO? {
        asiakirjaRepository.findOneByIdAndArviointiIdAndArviointiArvioinninAntajaUserId(
            asiakirjaId,
            id,
            userId
        )?.let {
            return AsiakirjaDTO(
                id = it.id,
                nimi = it.nimi,
                tyyppi = it.tyyppi,
                asiakirjaData = AsiakirjaDataDTO(fileInputStream = ByteArrayInputStream(it.asiakirjaData?.data))
            )
        }
        asiakirjaService.warnIfDeleted(asiakirjaId)
        return null
    }

    @Transactional(readOnly = true)
    override fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findForSeurantajakso(
            opintooikeusId,
            alkamispaiva,
            paattymispaiva
        ).map(suoritusarviointiMapper::toDto)
    }

    override fun delete(id: Long, opintooikeusId: Long) {
        suoritusarviointiRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(id, opintooikeusId)
            .ifPresent {
                if (it.arviointiAika == null && !it.lukittu) {
                    suoritusarviointiRepository.deleteById(id)
                }
            }
    }

    override fun findAvoimetByKouluttajaOrVastuuhenkiloUserId(userId: String): List<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByArvioinninAntajaUserIdAndArviointiAikaNull(
            userId
        )
            .map(suoritusarviointiMapper::toDto)
    }

    override fun existsByArvioitavaKokonaisuusId(arvioitavaKokonaisuusId: Long): Boolean {
        return suoritusarviointiRepository.existsByArvioitavatKokonaisuudetArvioitavaKokonaisuusId(
            arvioitavaKokonaisuusId
        )
    }

    private fun mapAsiakirjat(
        suoritusarviointi: Suoritusarviointi,
        newAsiakirjat: Set<AsiakirjaDTO>,
        deletedAsiakirjaIds: Set<Int>?,
        itsearviointi: Boolean
    ): Suoritusarviointi {
        newAsiakirjat.let {
            val asiakirjaEntities = it.map { asiakirjaDTO ->
                asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                    this.lisattypvm = LocalDateTime.now()
                    this.opintooikeus = suoritusarviointi.tyoskentelyjakso?.opintooikeus
                    if (itsearviointi) {
                        this.itsearviointi = suoritusarviointi
                    } else {
                        this.arviointi = suoritusarviointi
                    }
                    this.asiakirjaData?.data =
                        asiakirjaDTO.asiakirjaData?.fileInputStream?.readAllBytes()
                }
            }

            if (itsearviointi) {
                suoritusarviointi.itsearviointiAsiakirjat.addAll(asiakirjaEntities)
            } else {
                suoritusarviointi.arviointiAsiakirjat.addAll(asiakirjaEntities)
            }
        }

        deletedAsiakirjaIds?.map { x -> x.toLong() }?.let {
            if (itsearviointi) {
                suoritusarviointi.itsearviointiAsiakirjat.removeIf { asiakirja ->
                    asiakirja.id in it
                }
            } else {
                suoritusarviointi.arviointiAsiakirjat.removeIf { asiakirja ->
                    asiakirja.id in it
                }
            }
        }

        return suoritusarviointi
    }
}
