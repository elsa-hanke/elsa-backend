package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KoulutussuunnitelmaRepository : JpaRepository<Koulutussuunnitelma, Long> {

    fun findOneByErikoistuvaLaakariId(erikoistuvaLaakariId: Long): Koulutussuunnitelma?
}
