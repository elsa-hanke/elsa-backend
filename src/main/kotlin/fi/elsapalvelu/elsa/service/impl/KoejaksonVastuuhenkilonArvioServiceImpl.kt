package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class KoejaksonVastuuhenkilonArvioServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository,
    private val koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KoejaksonVastuuhenkilonArvioService {

    override fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)
        vastuuhenkilonArvio.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        // Sähköposti vastuuhenkilölle
        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!).get().user!!,
            "vastuuhenkilonArvioKuitattava.html",
            "email.vastuuhenkilonarviokuitattava.title",
            properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
        )

        return koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
    }

    override fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO {

        var vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioRepository.findById(koejaksonVastuuhenkilonArvioDTO.id!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön arviota ei löydy") }

        val updatedVastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioMapper.toEntity(koejaksonVastuuhenkilonArvioDTO)

        if (vastuuhenkilonArvio.erikoistuvaLaakari?.kayttaja?.user?.id == userId
            && vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null
        ) {
            vastuuhenkilonArvio.erikoistuvaAllekirjoittanut = true
        }

        if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {
            vastuuhenkilonArvio.vastuuhenkiloHyvaksynyt =
                updatedVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt
            vastuuhenkilonArvio.vastuuhenkilonKuittausaika = LocalDate.now(ZoneId.systemDefault())
        }

        vastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.save(vastuuhenkilonArvio)

        // Sähköposti vastuuhenkilölle allekirjoitetusta loppukeskustelusta
        if (vastuuhenkilonArvio.erikoistuvaLaakari?.kayttaja?.user?.id == userId
            && vastuuhenkilonArvio.erikoistuvaAllekirjoittanut
        ) {
            val erikoistuvaLaakari =
                kayttajaRepository.findById(vastuuhenkilonArvio?.erikoistuvaLaakari?.kayttaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(vastuuhenkilonArvio.vastuuhenkilo?.id!!)
                    .get().user!!,
                "vastuuhenkilonArvioHyvaksytty.html",
                "email.vastuuhenkilonarviohyvaksytty.title",
                properties = mapOf(
                    Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()),
                    Pair(MailProperty.NAME, erikoistuvaLaakari.getName())
                )
            )
        } else if (vastuuhenkilonArvio.vastuuhenkilo?.user?.id == userId) {

            // Sähköposti erikoistuvalle vastuuhenkilon kuittaamasta arviosta
            if (vastuuhenkilonArvio.vastuuhenkilonKuittausaika != null) {
                mailService.sendEmailFromTemplate(
                    kayttajaRepository.findById(vastuuhenkilonArvio?.erikoistuvaLaakari?.kayttaja?.id!!)
                        .get().user!!,
                    "vastuuhenkilonArvioKuitattava.html",
                    "email.vastuuhenkilonarviokuitattava.title",
                    properties = mapOf(Pair(MailProperty.ID, vastuuhenkilonArvio.id!!.toString()))
                )
            }
        }

        return koejaksonVastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvio)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findById(id)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO> {
        return koejaksonVastuuhenkilonArvioRepository.findOneByIdAndVastuuhenkiloUserId(id, userId)
            .map(koejaksonVastuuhenkilonArvioMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByVastuuhenkiloUserId(userId: String): Map<KayttajaDTO, KoejaksonVastuuhenkilonArvioDTO> {
        val arviot =
            koejaksonVastuuhenkilonArvioRepository.findAllByVastuuhenkiloUserId(userId)
        return arviot.associate {
            kayttajaMapper.toDto(it.erikoistuvaLaakari?.kayttaja!!) to koejaksonVastuuhenkilonArvioMapper.toDto(
                it
            )
        }
    }

    override fun delete(id: Long) {
        koejaksonVastuuhenkilonArvioRepository.deleteById(id)
    }
}
