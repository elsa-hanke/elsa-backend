package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.PalauteService
import fi.elsapalvelu.elsa.service.dto.PalauteDTO
import org.springframework.stereotype.Service

@Service
class PalauteServiceImpl(
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val applicationProperties: ApplicationProperties
) : PalauteService {

    override fun send(palauteDTO: PalauteDTO, userId: String) {
        kayttajaRepository.findOneByUserId(userId).ifPresent {
            mailService.sendEmailFromTemplate(
                applicationProperties.getFeedback().to,
                "palaute.html",
                "email.palaute.title",
                properties = mapOf(
                    Pair(MailProperty.FEEDBACK_TOPIC, palauteDTO.palautteenAihe!!),
                    Pair(MailProperty.FEEDBACK, palauteDTO.palaute!!)
                ),
            )
        }
    }
}
