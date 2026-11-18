package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.SYSTEM_ACCOUNT
import fi.elsapalvelu.elsa.domain.AuditRevisionEntity
import org.hibernate.envers.RevisionListener
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class AuditRevisionListener(private val clock: Clock) : RevisionListener {

    override fun newRevision(revisionEntity: Any?) {
        val audit = revisionEntity as AuditRevisionEntity
        val context = SecurityContextHolder.getContext()
        audit.userId =
            if (context.authentication is Saml2Authentication) {
                val authentication = context.authentication as Saml2Authentication
                val principal = authentication.principal as AuthenticatedPrincipal
                principal.name
            } else {
                SYSTEM_ACCOUNT
            }
        val muokkaushetki = clock.instant()
        audit.modifiedDate = LocalDateTime.ofInstant(muokkaushetki, clock.zone)
        audit.timestamp = muokkaushetki.toEpochMilli()
    }
}
