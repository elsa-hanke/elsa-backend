package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

/** The resource selects one operation; a user's other roles must not advance other workflow steps. */
enum class KoejaksoUpdateActor(private val authority: String) {
    TRAINEE(ERIKOISTUVA_LAAKARI),
    TRAINER(KOULUTTAJA),
    REVIEWER(OPINTOHALLINNON_VIRKAILIJA),
    APPROVER(VASTUUHENKILO);

    fun requireAuthority() {
        if (SecurityContextHolder.getContext().authentication?.authorities?.none { it.authority == authority } != false) {
            throw AccessDeniedException("Koejakson kasittelyoikeus puuttuu")
        }
    }
}
