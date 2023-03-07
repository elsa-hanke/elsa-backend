package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.security.AuditRevisionListener
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionEntity
import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener::class)
data class AuditRevisionEntity (

    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Column(name = "modified_date", nullable = false)
    var modifiedDate: LocalDateTime

) : DefaultRevisionEntity()
