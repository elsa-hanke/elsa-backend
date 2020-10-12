package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OsaamisalueenArviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [OsaamisalueenArviointi] entity.
 */
@Suppress("unused")
@Repository
interface OsaamisalueenArviointiRepository : JpaRepository<OsaamisalueenArviointi, Long>
