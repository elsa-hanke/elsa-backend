package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.Pikaviesti
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Pikaviesti] entity.
 */
@Suppress("unused")
@Repository
interface PikaviestiRepository : JpaRepository<Pikaviesti, Long>
