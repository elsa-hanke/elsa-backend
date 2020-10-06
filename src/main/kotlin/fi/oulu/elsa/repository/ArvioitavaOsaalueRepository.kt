package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.ArvioitavaOsaalue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [ArvioitavaOsaalue] entity.
 */
@Suppress("unused")
@Repository
interface ArvioitavaOsaalueRepository : JpaRepository<ArvioitavaOsaalue, Long>
