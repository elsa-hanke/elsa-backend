package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.SYSTEM_ACCOUNT
import fi.elsapalvelu.elsa.domain.AuditRevisionEntity
import org.hibernate.envers.RevisionListener
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import java.time.LocalDateTime

class AuditRevisionListener : RevisionListener {

    override fun newRevision(revisionEntity: Any?) {
        val audit = revisionEntity as AuditRevisionEntity
        val context = SecurityContextHolder.getContext()
        audit.userId =
            if (context.authentication != null) {
                val authentication = context.authentication as Saml2Authentication
                val principal = authentication.principal as AuthenticatedPrincipal
                principal.name
            } else {
                SYSTEM_ACCOUNT
            }
        audit.modifiedDate = LocalDateTime.now().toString()
    }
}
