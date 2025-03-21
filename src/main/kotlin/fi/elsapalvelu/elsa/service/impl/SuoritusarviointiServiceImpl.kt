package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.SuoritusarvioinninArviointityokalunVastaus
import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritusarvioinninArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ObjectUtils
import java.io.ByteArrayInputStream
import java.time.LocalDate
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
    private val asiakirjaMapper: AsiakirjaMapper,
    private val arviointityokaluKysymysRepository: ArviointityokaluKysymysRepository,
    private val arviointityokaluKysymysVaihtoehtoRepository: ArviointityokaluKysymysVaihtoehtoRepository,
) : SuoritusarviointiService {

    override fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO {
        var suoritusarviointi = suoritusarviointiMapper.toEntity(suoritusarviointiDTO)
        suoritusarviointi.arvioitavatKokonaisuudet.forEach {
            it.suoritusarviointi = suoritusarviointi
        }
        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(suoritusarviointi.arvioinninAntaja?.id!!).get().user!!,
            templateName = "arviointipyyntoKouluttajalleEmail.html",
            titleKey = "email.arviointipyyntokouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id!!.toString()))
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
            suoritusarviointiRepository.findOneById(suoritusarviointiDTO.id!!).get()

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
            suoritusarviointi.arvioitavaTapahtuma = suoritusarviointiDTO.arvioitavaTapahtuma
            suoritusarviointi.lisatiedot = suoritusarviointiDTO.lisatiedot
            suoritusarviointi.tapahtumanAjankohta = suoritusarviointiDTO.tapahtumanAjankohta
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjaksoRepository
                .findByIdOrNull(suoritusarviointiDTO.tyoskentelyjaksoId)
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
                    } ?: listOf()
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
        suoritusarviointi.vaativuustaso = suoritusarviointiDTO.vaativuustaso
        suoritusarviointi.sanallinenArviointi = suoritusarviointiDTO.sanallinenArviointi
        suoritusarviointi.arviointiAika = LocalDate.now(ZoneId.systemDefault())
        suoritusarviointi.arviointityokalut = arviointityokaluRepository.findAllByIdIn(
            suoritusarviointiDTO.arviointityokalut?.map(
                ArviointityokaluDTO::id
            ).orEmpty()
        )
        suoritusarviointi.arviointiPerustuu = suoritusarviointiDTO.arviointiPerustuu
        suoritusarviointi.muuPeruste = suoritusarviointiDTO.muuPeruste
        suoritusarviointi.arvioitavatKokonaisuudet.forEach {
            it.arviointiasteikonTaso =
                suoritusarviointiDTO.arvioitavatKokonaisuudet?.first { k -> k.id == it.id }?.arviointiasteikonTaso
        }

        val existingVastauksetById = suoritusarviointi.arviointityokaluVastaukset.associateBy { it.id }.toMutableMap()
        val dtoVastausIds = suoritusarviointiDTO.arviointityokaluVastaukset.mapNotNull { it.id }.toSet()
        val newOrUpdatedVastaukset = suoritusarviointiDTO.arviointityokaluVastaukset.map { dto ->
            existingVastauksetById[dto.id]?.apply {
                tekstiVastaus = dto.tekstiVastaus
                valittuVaihtoehto = dto.valittuVaihtoehtoId?.let { arviointityokaluKysymysVaihtoehtoRepository.findByIdOrNull(it) }
            } ?: SuoritusarvioinninArviointityokalunVastaus(
                suoritusarviointi = suoritusarviointi,
                arviointityokalu = arviointityokaluRepository.findByIdOrNull(dto.arviointityokaluId),
                arviointityokaluKysymys = arviointityokaluKysymysRepository.findByIdOrNull(dto.arviointityokaluKysymysId),
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

        val result = suoritusarviointiRepository.save(suoritusarviointi)

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
            kayttajaRepository.findById(suoritusarviointi.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                .get().user!!,
            templateName = templateName,
            titleKey = titleKey,
            properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id!!.toString()))
        )

        return result
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
