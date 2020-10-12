package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [EpaOsaamisalue] entity.
 */
@Suppress("unused")
@Repository
interface EpaOsaamisalueRepository : JpaRepository<EpaOsaamisalue, Long>
