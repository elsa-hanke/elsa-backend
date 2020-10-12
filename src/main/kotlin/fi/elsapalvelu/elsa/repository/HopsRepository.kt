package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Hops
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Hops] entity.
 */
@Suppress("unused")
@Repository
interface HopsRepository : JpaRepository<Hops, Long>
