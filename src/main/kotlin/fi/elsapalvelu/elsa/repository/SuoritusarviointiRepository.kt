package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Suoritusarviointi] entity.
 */
@Suppress("unused")
@Repository
interface SuoritusarviointiRepository : JpaRepository<Suoritusarviointi, Long>
