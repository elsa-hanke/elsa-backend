package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.Opintooikeustiedot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Opintooikeustiedot] entity.
 */
@Suppress("unused")
@Repository
interface OpintooikeustiedotRepository : JpaRepository<Opintooikeustiedot, Long>
