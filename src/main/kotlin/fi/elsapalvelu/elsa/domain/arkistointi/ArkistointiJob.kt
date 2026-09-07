package fi.elsapalvelu.elsa.domain.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "arkistointi_job")
data class ArkistointiJob(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "university", nullable = false, length = 50)
    var university: YliopistoEnum? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 50)
    var caseType: CaseType? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ArkistointiJobStatus = ArkistointiJobStatus.ODOTTAA,

    @get:NotNull
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    var payload: String? = null,

    @get:NotNull
    @get:Size(max = 255)
    @Column(name = "job_key", nullable = false, unique = true, length = 255)
    var key: String? = null,

    @get:Size(max = 255)
    @Column(name = "external_id", length = 255)
    var externalId: String? = null,

    @get:Size(max = 100)
    @Column(name = "error_code", length = 100)
    var errorCode: String? = null,

    @get:Size(max = 1000)
    @Column(name = "last_error", length = 1000)
    var lastError: String? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = Instant.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = Instant.now(),

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @OneToMany(
        mappedBy = "job",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @OrderBy("jarjestysnumero ASC")
    var asiakirjat: MutableList<ArkistointiJobAsiakirja> = mutableListOf()

) : Serializable {

    fun lisaaAsiakirja(asiakirja: ArkistointiJobAsiakirja) {
        asiakirja.job = this
        asiakirjat.add(asiakirja)
    }

    fun transitionTo(newStatus: ArkistointiJobStatus) {
        require(status.canTransitionTo(newStatus)) {
            "ArkistointiJob ei voi siirtya tilasta $status tilaan $newStatus"
        }
        status = newStatus
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArkistointiJob) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArkistointiJob{" +
        "id=$id" +
        ", university=$university" +
        ", caseType=$caseType" +
        ", status=$status" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}
