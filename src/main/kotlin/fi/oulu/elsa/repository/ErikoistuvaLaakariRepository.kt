package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.ErikoistuvaLaakari
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [ErikoistuvaLaakari] entity.
 */
@Suppress("unused")
@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long>
