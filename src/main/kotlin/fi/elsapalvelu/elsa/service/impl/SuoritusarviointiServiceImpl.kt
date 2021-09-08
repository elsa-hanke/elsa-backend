package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
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
            kayttajaRepository.findById(suoritusarviointi.arvioinninAntaja?.id!!).get(),
            "arviointipyyntoKouluttajalleEmail.html",
            "email.arviointipyyntokouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id!!.toString()))
        )
        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    override fun save(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        kayttajaId: String
    ): SuoritusarviointiDTO {
        var suoritusarviointi =
            suoritusarviointiRepository.findOneById(suoritusarviointiDTO.id!!).get()

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari
        ) {
            suoritusarviointi = handleErikoistuva(suoritusarviointiDTO, suoritusarviointi)
        }

        val kirjautunutKayttaja = kayttajaRepository.findById(kayttajaId)
        if (kirjautunutKayttaja.isPresent && kirjautunutKayttaja.get() == suoritusarviointi.arvioinninAntaja) {
            suoritusarviointi = handleKouluttaja(suoritusarviointiDTO, suoritusarviointi)
        }

        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    private fun handleErikoistuva(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        suoritusarviointi: Suoritusarviointi
    ): Suoritusarviointi {
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

        return suoritusarviointiRepository.save(suoritusarviointi)
    }

    private fun handleKouluttaja(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        suoritusarviointi: Suoritusarviointi
    ): Suoritusarviointi {
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
            kayttajaRepository.findById(suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari?.kayttaja?.id!!)
                .get(),
            templateName,
            titleKey,
            properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id!!.toString()))
        )

        return result
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): List<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
            kayttajaId
        )
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
            id,
            kayttajaId
        ).map(suoritusarviointiMapper::toDto)
    }

    override fun findOneByIdAndArvioinninAntajaId(
        id: Long,
        kayttajaId: String
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndArvioinninAntajaId(id, kayttajaId)
            .map(suoritusarviointiMapper::toDto)
    }

    override fun delete(id: Long, kayttajaId: String) {
        suoritusarviointiRepository.findOneById(id).ifPresent {
            val kirjautunutErikoistuvaLaakari =
                erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
            val canDelete = it.arviointiAika == null && !it.lukittu
            if (kirjautunutErikoistuvaLaakari != null
                && kirjautunutErikoistuvaLaakari == it.tyoskentelyjakso?.erikoistuvaLaakari
                && canDelete
            ) {
                suoritusarviointiRepository.deleteById(id)
            }
        }
    }
}
