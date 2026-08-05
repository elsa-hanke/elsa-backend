package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.kayttaja.PalauteService
import fi.elsapalvelu.elsa.service.dto.kayttaja.PalauteDTO
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

            mailService.sendFeedbackEmail(
                applicationProperties.getFeedback().to.toString(),
                templateName = "mail/palaute.html",
                properties = mapOf(
                    Pair(MailProperty.FEEDBACK_TOPIC, palauteDTO.palautteenAihe!!),
                    Pair(MailProperty.FEEDBACK_YLIOPISTO, palauteDTO.palauteYliopisto!!),
                    Pair(MailProperty.FEEDBACK, palauteDTO.palaute!!),
                    Pair(MailProperty.FEEDBACK_SENDER, feedbackSender)
                ),
            )
        }
    }
}
