package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArkistointiJobRepository : JpaRepository<ArkistointiJob, Long> {

    fun findByKey(key: String): ArkistointiJob?

    fun findAllByStatus(status: ArkistointiJobStatus): List<ArkistointiJob>
}
