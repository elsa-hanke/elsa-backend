package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KoejaksonKehittamistoimenpiteet
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKehittamistoimenpiteetRepository
import fi.elsapalvelu.elsa.repository.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.service.KoejaksonKehittamistoimenpiteetService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKehittamistoimenpiteetDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKehittamistoimenpiteetMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
@Transactional
class KoejaksonKehittamistoimenpiteetServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonKehittamistoimenpiteetMapper: KoejaksonKehittamistoimenpiteetMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonKehittamistoimenpiteetService {

    override fun create(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        kayttajaId: String
    ): KoejaksonKehittamistoimenpiteetDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetMapper.toEntity(koejaksonKehittamistoimenpiteetDTO)
        kehittamistoimenpiteet.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

        // Sähköposti kouluttajalle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(kehittamistoimenpiteet.lahikouluttaja?.id!!).get(),
            "kehittamistoimenpiteetKouluttajalle.html",
            "email.kehittamistoimenpiteetkouluttajalle.title",
            properties = mapOf(Pair(MailProperty.ID, kehittamistoimenpiteet.id!!.toString()))
        )

        return koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
    }

    override fun update(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        kayttajaId: String
    ): KoejaksonKehittamistoimenpiteetDTO {

        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(koejaksonKehittamistoimenpiteetDTO.id!!)
                .orElseThrow { EntityNotFoundException("Kehittämistoimenpiteitä ei löydy") }

        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetMapper.toEntity(koejaksonKehittamistoimenpiteetDTO)

        if (kehittamistoimenpiteet.erikoistuvaLaakari?.kayttaja?.id == kayttajaId
            && kehittamistoimenpiteet.lahiesimiesHyvaksynyt
        ) {
            kehittamistoimenpiteet = handleErikoistuva(kehittamistoimenpiteet)
        }

        if (kehittamistoimenpiteet.lahikouluttaja?.id == kayttajaId
            && !kehittamistoimenpiteet.lahiesimiesHyvaksynyt
        ) {
            kehittamistoimenpiteet =
                handleKouluttaja(kehittamistoimenpiteet, updatedKehittamistoimenpiteet)
        }

        if (kehittamistoimenpiteet.lahiesimies?.id == kayttajaId
            && kehittamistoimenpiteet.lahikouluttajaHyvaksynyt
        ) {
            kehittamistoimenpiteet =
                handleEsimies(kehittamistoimenpiteet, updatedKehittamistoimenpiteet)
        }

        return koejaksonKehittamistoimenpiteetMapper.toDto(kehittamistoimenpiteet)
    }

    private fun handleErikoistuva(kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet): KoejaksonKehittamistoimenpiteet {
        kehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true
        kehittamistoimenpiteet.erikoistuvanAllekirjoitusaika = LocalDate.now()

        val result = koejaksonKehittamistoimenpiteetRepository.save(kehittamistoimenpiteet)

        // Sähköposti kouluttajalle ja esimiehelle allekirjoitetusta väliarvioinnista
        if (result.erikoistuvaAllekirjoittanut) {
            val erikoistuvaLaakariKayttaja =
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!).get()
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahikouluttaja?.id!!).get(),
                "kehittamistoimenpiteetHyvaksytty.html",
                "email.kehittamistoimenpiteethyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, result.id!!.toString()),
                    Pair(
                        MailProperty.NAME,
                        "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}"
                    )
                )
            )
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(result.lahiesimies?.id!!).get(),
                "kehittamistoimenpiteetHyvaksytty.html",
                "email.kehittamistoimenpiteethyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, result.id!!.toString()),
                    Pair(
                        MailProperty.NAME,
                        "${erikoistuvaLaakariKayttaja.etunimi} ${erikoistuvaLaakariKayttaja.sukunimi}"
                    )
                )
            )
        }

        return result
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
                kayttajaRepository.findById(result.lahiesimies?.id!!).get(),
                "kehittamistoimenpiteetKuitattava.html",
                "email.kehittamistoimenpiteetkuitattava.title",
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
                kayttajaRepository.findById(result.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get(),
                "kehittamistoimenpiteetKuitattava.html",
                "email.kehittamistoimenpiteetkuitattava.title",
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
                    .get(),
                "kehittamistoimenpiteetPalautettu.html",
                "email.kehittamistoimenpiteetpalautettu.title",
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
            .map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        return koejaksonKehittamistoimenpiteetRepository.findByErikoistuvaLaakariKayttajaId(
            kayttajaId
        )
            .map(koejaksonKehittamistoimenpiteetMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteet = koejaksonKehittamistoimenpiteetRepository.findOneByIdAndLahikouluttajaId(
            id,
            kayttajaId
        )
        return applyWithKehittamistoimenpiteetDescription(kehittamistoimenpiteet)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteet = koejaksonKehittamistoimenpiteetRepository.findOneByIdAndLahiesimiesId(
            id,
            kayttajaId
        )
        return applyWithKehittamistoimenpiteetDescription(kehittamistoimenpiteet)
    }

    private fun applyWithKehittamistoimenpiteetDescription(kehittamistoimenpiteet: Optional<KoejaksonKehittamistoimenpiteet>): Optional<KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteetDto = kehittamistoimenpiteet.map(koejaksonKehittamistoimenpiteetMapper::toDto)
        if (kehittamistoimenpiteet.isPresent) {
            kehittamistoimenpiteet.get().erikoistuvaLaakari?.kayttaja?.id?.let { kayttajaId ->
                koejaksonValiarviointiRepository.findByErikoistuvaLaakariKayttajaId(kayttajaId).let {
                    kehittamistoimenpiteetDto.get().apply {
                        this.kehittamistoimenpiteetKuvaus = if (it.isPresent) it.get().kehittamistoimenpiteet else null
                    }
                }
            }
        }
        return kehittamistoimenpiteetDto
    }

    @Transactional(readOnly = true)
    override fun findAllByKouluttajaId(kayttajaId: String): Map<KayttajaDTO, KoejaksonKehittamistoimenpiteetDTO> {
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findAllByLahikouluttajaIdOrLahiesimiesId(
                kayttajaId
            )
        return kehittamistoimenpiteet.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonKehittamistoimenpiteetMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonKehittamistoimenpiteetRepository.deleteById(id)
    }
}
