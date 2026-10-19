package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository

@Repository
interface ArkistointiJobRepository : JpaRepository<ArkistointiJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ArkistointiJob job where job.id = :id")
    fun findByIdForStateChange(@Param("id") id: Long): ArkistointiJob?

    fun findByKey(key: String): ArkistointiJob?

    fun findAllByStatus(status: ArkistointiJobStatus): List<ArkistointiJob>
}
