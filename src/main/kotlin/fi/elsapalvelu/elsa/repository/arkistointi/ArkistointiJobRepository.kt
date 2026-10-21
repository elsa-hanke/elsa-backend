package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ArkistointiJobRepository : JpaRepository<ArkistointiJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ArkistointiJob job where job.id = :id")
    fun findByIdForStateChange(@Param("id") id: UUID): ArkistointiJob?

    fun findByKey(key: String): ArkistointiJob?
}
