package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Arviointiosaalue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Arviointiosaalue] entity.
 */
@Suppress("unused")
@Repository
interface ArviointiosaalueRepository : JpaRepository<Arviointiosaalue, Long>
