package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ObjectUtils
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Service
@Transactional
class SuoritusarviointiServiceImpl(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val epaOsaamisalueRepository: EpaOsaamisalueRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper,
    private val arviointityokaluRepository: ArviointityokaluRepository,
    private val mailService: MailService,
) : SuoritusarviointiService {

    override fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO {
        var suoritusarviointi = suoritusarviointiMapper.toEntity(suoritusarviointiDTO)
        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(suoritusarviointi.arvioinninAntaja?.id!!).get().user!!,
            "arviointipyyntoKouluttajalleEmail.html",
            "email.arviointipyyntokouluttajalle.title",
            id = suoritusarviointi.id!!
        )
        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    override fun save(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        userId: String
    ): SuoritusarviointiDTO {
        var suoritusarviointi = suoritusarviointiRepository
            .findOneById(suoritusarviointiDTO.id!!).get()
        val isNewArviointi = suoritusarviointi.arviointiAika == null

        // Erikoistuva lääkäri
        suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari.let {
            val kirjautunutErikoistuvaLaakari =
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
            if (kirjautunutErikoistuvaLaakari != null && kirjautunutErikoistuvaLaakari == it) {
                val isItsearviointiNotEmpty =
                    !ObjectUtils.isEmpty(suoritusarviointiDTO.itsearviointiVaativuustaso) &&
                        !ObjectUtils.isEmpty(suoritusarviointiDTO.itsearviointiLuottamuksenTaso) &&
                        !ObjectUtils.isEmpty(suoritusarviointiDTO.sanallinenItsearviointi)

                // Itsearvioinnin tekeminen
                if (isItsearviointiNotEmpty) {
                    suoritusarviointi.itsearviointiVaativuustaso =
                        suoritusarviointiDTO.itsearviointiVaativuustaso
                    suoritusarviointi.itsearviointiLuottamuksenTaso =
                        suoritusarviointiDTO.itsearviointiLuottamuksenTaso
                    suoritusarviointi.sanallinenItsearviointi =
                        suoritusarviointiDTO.sanallinenItsearviointi
                    suoritusarviointi.itsearviointiAika = LocalDate.now(ZoneId.systemDefault())
                } else {
                    // Arviointipyynnön muokkaus
                    suoritusarviointi.arvioitavaOsaalue = epaOsaamisalueRepository
                        .findByIdOrNull(suoritusarviointiDTO.arvioitavaOsaalueId)
                    suoritusarviointi.arvioitavaTapahtuma = suoritusarviointiDTO.arvioitavaTapahtuma
                    suoritusarviointi.lisatiedot = suoritusarviointiDTO.lisatiedot
                    suoritusarviointi.tapahtumanAjankohta = suoritusarviointiDTO.tapahtumanAjankohta
                    suoritusarviointi.tyoskentelyjakso = tyoskentelyjaksoRepository
                        .findByIdOrNull(suoritusarviointiDTO.tyoskentelyjaksoId)
                }
            }
        }

        // Arvioinnin antaja
        suoritusarviointi.arvioinninAntaja.let {
            val kirjautunutKayttaja = kayttajaRepository
                .findOneByUserLogin(userId)

            val isArviointiNotEmpty = !ObjectUtils.isEmpty(suoritusarviointiDTO.vaativuustaso) &&
                !ObjectUtils.isEmpty(suoritusarviointiDTO.luottamuksenTaso) &&
                !ObjectUtils.isEmpty(suoritusarviointiDTO.sanallinenArviointi)

            // Arvioinnin tekeminen
            if (kirjautunutKayttaja.isPresent &&
                kirjautunutKayttaja.get() == it &&
                isArviointiNotEmpty
            ) {
                suoritusarviointi.vaativuustaso = suoritusarviointiDTO.vaativuustaso
                suoritusarviointi.luottamuksenTaso = suoritusarviointiDTO.luottamuksenTaso
                suoritusarviointi.sanallinenArviointi = suoritusarviointiDTO.sanallinenArviointi
                suoritusarviointi.arviointiAika = LocalDate.now(ZoneId.systemDefault())
                suoritusarviointi.arviointityokalut = arviointityokaluRepository.findAllByIdIn(
                    suoritusarviointiDTO.arviointityokalut?.map(
                        ArviointityokaluDTO::id
                    ).orEmpty()
                )
                suoritusarviointi.arviointiPerustuu = suoritusarviointiDTO.arviointiPerustuu
                suoritusarviointi.muuPeruste = suoritusarviointiDTO.muuPeruste
            }
        }

        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)

        if (suoritusarviointi.arviointiAika != null) {
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
                kayttajaRepository.findById(suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!,
                templateName,
                titleKey,
                id = suoritusarviointi.id!!
            )
        }

        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAll(pageable)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariId(
        erikoistuvaLaakariId: Long,
        pageable: Pageable
    ): Page<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariId(
            erikoistuvaLaakariId,
            pageable
        )
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String,
        pageable: Pageable
    ): Page<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
            userId,
            pageable
        )
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): MutableList<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
            userId
        )
            .mapTo(mutableListOf(), suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findById(id)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
            id,
            userId
        ).map(suoritusarviointiMapper::toDto)
    }

    override fun findOneByIdAndArvioinninAntajauserLogin(
        id: Long,
        userLogin: String
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndArvioinninAntajaUserLogin(
            id,
            userLogin
        ).map(suoritusarviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        suoritusarviointiRepository.deleteById(id)
    }

    override fun delete(id: Long, userId: String) {
        val suoritusarviointiOpt = suoritusarviointiRepository.findOneById(id)
        if (suoritusarviointiOpt.isPresent) {
            val suoritusarviointi = suoritusarviointiOpt.get()
            suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari.let {
                val kirjautunutErikoistuvaLaakari =
                    erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                if (
                    kirjautunutErikoistuvaLaakari != null &&
                    kirjautunutErikoistuvaLaakari == it &&
                    suoritusarviointi.arviointiAika == null &&
                    !suoritusarviointi.lukittu
                ) {
                    suoritusarviointiRepository.deleteById(id)
                }
            }
        }
    }
}
