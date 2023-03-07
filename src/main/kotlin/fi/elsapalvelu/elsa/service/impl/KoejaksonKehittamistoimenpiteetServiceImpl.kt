package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonKehittamistoimenpiteet
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKehittamistoimenpiteetRepository
import fi.elsapalvelu.elsa.repository.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.KoejaksonKehittamistoimenpiteetService
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KoejaksonKehittamistoimenpiteetDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKehittamistoimenpiteetMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import jakarta.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonKehittamistoimenpiteetServiceImpl(
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonKehittamistoimenpiteetMapper: KoejaksonKehittamistoimenpiteetMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) : KoejaksonKehittamistoimenpiteetService {

    override fun create(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        opintooikeusId: Long
    ): KoejaksonKehittamistoimenpiteetDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var kehittamistoimenpiteet =
                koejaksonKehittamistoimenpiteetMapper.toEntity(koejaksonKehittamistoimenpiteetDTO)
            kehittamistoimenpiteet.opintooikeus = it
            kehittamistoimenpiteet =
                koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

            kouluttajavaltuutusService.lisaaValtuutus(
                kehittamistoimenpiteet.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                kehittamistoimenpiteet.lahikouluttaja?.id!!
            )
            kouluttajavaltuutusService.lisaaValtuutus(
                kehittamistoimenpiteet.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.id!!,
                kehittamistoimenpiteet.lahiesimies?.id!!
            )

            // Sähköposti kouluttajalle
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(kehittamistoimenpiteet.lahikouluttaja?.id!!)
                    .get().user!!,
                templateName = "kehittamistoimenpiteetKouluttajalle.html",
                titleKey = "email.kehittamistoimenpiteetkouluttajalle.title",
                properties = mapOf(Pair(MailProperty.ID, kehittamistoimenpiteet.id!!.toString()))
            )

            koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
        }
    }

    override fun update(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        userId: String
    ): KoejaksonKehittamistoimenpiteetDTO {
        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(koejaksonKehittamistoimenpiteetDTO.id!!)
                .orElseThrow { EntityNotFoundException("Kehittämistoimenpiteitä ei löydy") }

        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetMapper.toEntity(koejaksonKehittamistoimenpiteetDTO)

        if (kehittamistoimenpiteet.lahikouluttaja?.user?.id == userId
            && !kehittamistoimenpiteet.lahiesimiesHyvaksynyt
        ) {
            kehittamistoimenpiteet =
                handleKouluttaja(kehittamistoimenpiteet, updatedKehittamistoimenpiteet)
        }

        if (kehittamistoimenpiteet.lahiesimies?.user?.id == userId
            && kehittamistoimenpiteet.lahikouluttajaHyvaksynyt
        ) {
            kehittamistoimenpiteet =
                handleEsimies(kehittamistoimenpiteet, updatedKehittamistoimenpiteet)
        }

        return koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
    }

    private fun handleKouluttaja(
        kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet,
        updated: KoejaksonKehittamistoimenpiteet
    ): KoejaksonKehittamistoimenpiteet {
        kehittamistoimenpiteet.kehittamistoimenpiteetRiittavat =
            updated.kehittamistoimenpiteetRiittavat
        kehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        kehittamistoimenpiteet.lahikouluttajanKuittausaika = LocalDate.now(ZoneId.systemDefault())
        kehittamistoimenpiteet.korjausehdotus = null

        val result = koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta väliarvioinnista
        if (result.lahikouluttajaHyvaksynyt) {
            // Sähköposti esimiehelle kouluttajan hyväksymästä kehittämistoimenpiteestä
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahiesimies?.id!!).get().user!!,
                templateName = "kehittamistoimenpiteetKuitattava.html",
                titleKey = "email.kehittamistoimenpiteetkuitattava.title",
                properties = mapOf(Pair(MailProperty.ID, result.id!!.toString()))
            )
        }

        return result
    }

    private fun handleEsimies(
        kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet,
        updated: KoejaksonKehittamistoimenpiteet
    ): KoejaksonKehittamistoimenpiteet {
        // Hyväksytty
        if (updated.korjausehdotus.isNullOrBlank()) {
            kehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
            kehittamistoimenpiteet.lahiesimiehenKuittausaika =
                LocalDate.now(ZoneId.systemDefault())
        }
        // Palautettu korjattavaksi
        else {
            kehittamistoimenpiteet.korjausehdotus = updated.korjausehdotus
            kehittamistoimenpiteet.lahikouluttajaHyvaksynyt = false
            kehittamistoimenpiteet.lahikouluttajanKuittausaika = null
        }

        val result = koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

        // Sähköposti erikoistuvalle esimiehen hyväksymästä kehittämistoimenpiteestä
        if (result.lahikouluttajaHyvaksynyt) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!,
                templateName = "kehittamistoimenpiteetKuitattava.html",
                titleKey = "email.kehittamistoimenpiteetkuitattava.title",
                properties = mapOf(
                    Pair(
                        MailProperty.ID,
                        result.id!!.toString()
                    )
                )
            )
        }
        // Sähköposti kouluttajalle korjattavasta kehittämistoimenpiteestä
        else {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!)
                    .get().user!!,
                templateName = "kehittamistoimenpiteetPalautettu.html",
                titleKey = "email.kehittamistoimenpiteetpalautettu.title",
                properties = mapOf(
                    Pair(
                        MailProperty.ID,
                        result.id!!.toString()
                    )
                )
            )
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findById(id)
            .map(this::mapKehittamistoimenpiteet)
    }

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findByOpintooikeusId(
            opintooikeusId
        )
            .map(this::mapKehittamistoimenpiteet)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findOneByIdAndLahikouluttajaUserId(
                id,
                userId
            )
        return applyWithKehittamistoimenpiteetDescription(kehittamistoimenpiteet)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findOneByIdAndLahiesimiesUserId(
                id,
                userId
            )
        return applyWithKehittamistoimenpiteetDescription(kehittamistoimenpiteet)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
            id,
            vastuuhenkiloUserId
        )
            .map(this::mapKehittamistoimenpiteet)
    }

    private fun applyWithKehittamistoimenpiteetDescription(
        kehittamistoimenpiteet: Optional<KoejaksonKehittamistoimenpiteet>
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteetDto =
            kehittamistoimenpiteet.map(koejaksonKehittamistoimenpiteetMapper::toDto)
        if (kehittamistoimenpiteet.isPresent) {
            kehittamistoimenpiteet.get().opintooikeus?.id?.let { opintooikeusId ->
                koejaksonValiarviointiRepository.findByOpintooikeusId(opintooikeusId).let {
                    kehittamistoimenpiteetDto.get().apply {
                        this.kehittamistoimenpiteetKuvaus =
                            if (it.isPresent) it.get().kehittamistoimenpiteet else null
                        this.kehittamistoimenpideKategoriat =
                            if (it.isPresent) it.get().kehittamistoimenpideKategoriat?.toList() else null
                        this.muuKategoria = if (it.isPresent) it.get().muuKategoria else null
                    }
                }
            }
        }
        return kehittamistoimenpiteetDto
    }

    override fun delete(id: Long) {
        koejaksonKehittamistoimenpiteetRepository.deleteById(id)
    }

    private fun mapKehittamistoimenpiteet(kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet): KoejaksonKehittamistoimenpiteetDTO {
        val result = koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
        koejaksonValiarviointiRepository.findByOpintooikeusId(kehittamistoimenpiteet.opintooikeus?.id!!)
            .let {
                result.kehittamistoimenpideKategoriat =
                    it.get().kehittamistoimenpideKategoriat?.toList()
                result.muuKategoria = it.get().muuKategoria
            }
        return result
    }
}
