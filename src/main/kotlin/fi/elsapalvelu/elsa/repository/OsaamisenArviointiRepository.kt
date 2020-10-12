package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OsaamisenArviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [OsaamisenArviointi] entity.
 */
@Suppress("unused")
@Repository
interface OsaamisenArviointiRepository : JpaRepository<OsaamisenArviointi, Long>
