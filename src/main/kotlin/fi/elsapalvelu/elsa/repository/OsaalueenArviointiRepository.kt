package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OsaalueenArviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [OsaalueenArviointi] entity.
 */
@Suppress("unused")
@Repository
interface OsaalueenArviointiRepository : JpaRepository<OsaalueenArviointi, Long>
