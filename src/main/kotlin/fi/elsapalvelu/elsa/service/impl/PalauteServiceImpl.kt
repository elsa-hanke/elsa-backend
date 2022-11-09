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
            val feedbackSender = when {
                palauteDTO.anonyymiPalaute -> ""
                it.user?.email != null -> "${it.id}, ${it.getNimi()}, ${it.user?.email}"
                else -> "${it.id}, ${it.getNimi()}"
            }

            mailService.sendEmailFromTemplate(
                applicationProperties.getFeedback().to,
                templateName = "palaute.html",
                titleKey = "email.palaute.title",
                properties = mapOf(
                    Pair(MailProperty.FEEDBACK_TOPIC, palauteDTO.palautteenAihe!!),
                    Pair(MailProperty.FEEDBACK, palauteDTO.palaute!!),
                    Pair(MailProperty.FEEDBACK_SENDER, feedbackSender)
                ),
            )
        }
    }
}
