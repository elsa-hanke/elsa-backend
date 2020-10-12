package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Erikoisala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Erikoisala] entity.
 */
@Suppress("unused")
@Repository
interface ErikoisalaRepository : JpaRepository<Erikoisala, Long>
