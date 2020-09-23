package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.Koejakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Koejakso] entity.
 */
@Suppress("unused")
@Repository
interface KoejaksoRepository : JpaRepository<Koejakso, Long>
