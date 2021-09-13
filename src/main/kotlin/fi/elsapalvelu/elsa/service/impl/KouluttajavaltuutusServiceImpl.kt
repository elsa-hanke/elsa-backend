package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.mapper.KouluttajavaltuutusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@Transactional
class KouluttajavaltuutusServiceImpl(
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kouluttajavaltuutusMapper: KouluttajavaltuutusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val mailService: MailService
) : KouluttajavaltuutusService {

    override fun save(
        userId: String,
        kouluttajavaltuutusDTO: KouluttajavaltuutusDTO
    ): KouluttajavaltuutusDTO {
        var kouluttajavaltuutus = kouluttajavaltuutusMapper.toEntity(kouluttajavaltuutusDTO)

        if (kouluttajavaltuutus.id == null) {
            kouluttajavaltuutus.valtuuttaja =
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
            kouluttajavaltuutus.valtuutuksenLuontiaika = Instant.now()
        } else {
            kouluttajavaltuutusRepository.findById(kouluttajavaltuutus.id!!).ifPresent {
                kouluttajavaltuutus = it
                kouluttajavaltuutus.paattymispaiva = kouluttajavaltuutusDTO.paattymispaiva
            }
        }
        if (kouluttajavaltuutus.valtuuttaja?.kayttaja?.user?.id == userId) {
            kouluttajavaltuutus.valtuutuksenMuokkausaika = Instant.now()
            kouluttajavaltuutus = kouluttajavaltuutusRepository.save(kouluttajavaltuutus)

            mailService.sendEmailFromTemplate(
                kouluttajavaltuutus.valtuutettu?.user!!,
                "katseluoikeudet.html",
                "email.katseluoikeudet.title",
                properties = mapOf(
                    Pair(
                        MailProperty.NAME,
                        kouluttajavaltuutus.valtuuttaja?.kayttaja!!.getNimi()
                    ),
                    Pair(
                        MailProperty.DATE, kouluttajavaltuutus.paattymispaiva!!.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        )
                    )
                )
            )
        }

        return kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findAll()
            .map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllValtuutettuByValtuuttajaKayttajaUserId(id: String): List<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findAllByValtuuttajaKayttajaUserIdAndPaattymispaivaAfter(
            id,
            LocalDate.now().minusDays(1)
        ).map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findValtuutettuByValtuuttajaAndValtuutettu(
        valtuutettuId: String,
        valtuuttajaId: String
    ): Optional<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findByValtuuttajaKayttajaUserIdAndValtuutettuUserIdAndPaattymispaivaAfter(
            valtuutettuId,
            valtuuttajaId,
            LocalDate.now().minusDays(1)
        )
            .map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findById(id)
            .map(kouluttajavaltuutusMapper::toDto)
    }

    override fun delete(id: Long) {
        kouluttajavaltuutusRepository.deleteById(id)
    }
}
