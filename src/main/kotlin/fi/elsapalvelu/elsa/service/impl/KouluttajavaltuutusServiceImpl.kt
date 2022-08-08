package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
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
    private val kayttajaRepository: KayttajaRepository,
    private val mailService: MailService
) : KouluttajavaltuutusService {

    override fun save(
        userId: String,
        kouluttajavaltuutusDTO: KouluttajavaltuutusDTO,
        sendMail: Boolean
    ): KouluttajavaltuutusDTO {
        var kouluttajavaltuutus = kouluttajavaltuutusMapper.toEntity(kouluttajavaltuutusDTO)
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val opintooikeus = erikoistuvaLaakari?.getOpintooikeusKaytossa()

        if (kouluttajavaltuutus.id == null) {
            kouluttajavaltuutusRepository.findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(
                opintooikeus?.id!!,
                kayttajaRepository.findById(kouluttajavaltuutusDTO.valtuutettu?.id!!)
                    .get().user?.id!!,
                LocalDate.now().minusDays(1)
            ).ifPresentOrElse({
                kouluttajavaltuutus = it
                kouluttajavaltuutus.paattymispaiva = kouluttajavaltuutusDTO.paattymispaiva
            }, {
                kouluttajavaltuutus.valtuuttajaOpintooikeus = opintooikeus
                kouluttajavaltuutus.valtuutuksenLuontiaika = Instant.now()
            })
        } else {
            kouluttajavaltuutusRepository.findById(kouluttajavaltuutus.id!!).ifPresent {
                kouluttajavaltuutus = it
                kouluttajavaltuutus.paattymispaiva = kouluttajavaltuutusDTO.paattymispaiva
            }
        }
        if (kouluttajavaltuutus.valtuuttajaOpintooikeus?.id == opintooikeus?.id) {
            kouluttajavaltuutus.valtuutuksenMuokkausaika = Instant.now()
            kouluttajavaltuutus = kouluttajavaltuutusRepository.save(kouluttajavaltuutus)

            if (sendMail) {
                mailService.sendEmailFromTemplate(
                    kouluttajavaltuutus.valtuutettu?.user!!,
                    templateName = "katseluoikeudet.html",
                    titleKey = "email.katseluoikeudet.title",
                    properties = mapOf(
                        Pair(
                            MailProperty.NAME,
                            erikoistuvaLaakari?.kayttaja!!.getNimi()
                        ),
                        Pair(
                            MailProperty.ERIKOISALA,
                            opintooikeus?.erikoisala?.nimi!!
                        ),
                        Pair(
                            MailProperty.YLIOPISTO,
                            opintooikeus.yliopisto?.nimi?.toString()!!
                        ),
                        Pair(
                            MailProperty.DATE, kouluttajavaltuutus.paattymispaiva!!.format(
                                DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            )
                        )
                    )
                )
            }
        }

        return kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findAll()
            .map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllValtuutettuByValtuuttajaKayttajaUserId(userId: String): List<KouluttajavaltuutusDTO> {
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        return kouluttajavaltuutusRepository.findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaAfter(
            erikoistuvaLaakari?.getOpintooikeusKaytossa()?.id!!,
            LocalDate.now().minusDays(1)
        ).sortedBy { it.paattymispaiva }.map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findValtuutettuByValtuuttajaAndValtuutettu(
        userId: String,
        valtuutettuId: String,
    ): Optional<KouluttajavaltuutusDTO> {
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        return kouluttajavaltuutusRepository.findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(
            erikoistuvaLaakari?.getOpintooikeusKaytossa()?.id!!,
            valtuutettuId,
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

    override fun lisaaValtuutus(erikoistuvaUserId: String, valtuutettuKayttajaId: Long) {
        save(
            erikoistuvaUserId,
            KouluttajavaltuutusDTO(
                alkamispaiva = LocalDate.now(),
                paattymispaiva = LocalDate.now().plusMonths(6),
                valtuutettu = KayttajaDTO(id = valtuutettuKayttajaId)
            )
        )
    }
}
