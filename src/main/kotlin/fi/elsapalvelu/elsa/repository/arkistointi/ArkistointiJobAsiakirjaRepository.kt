package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArkistointiJobAsiakirjaRepository : JpaRepository<ArkistointiJobAsiakirja, Long> {

    fun findAllByJobIdOrderByJarjestysnumeroAsc(jobId: Long): List<ArkistointiJobAsiakirja>
}
