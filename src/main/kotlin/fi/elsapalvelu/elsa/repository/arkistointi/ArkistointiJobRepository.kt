package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ArkistointiJobRepository : JpaRepository<ArkistointiJob, UUID> {

    fun findByKey(key: String): ArkistointiJob?
}
