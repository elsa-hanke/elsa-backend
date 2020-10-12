package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Osoite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Osoite] entity.
 */
@Suppress("unused")
@Repository
interface OsoiteRepository : JpaRepository<Osoite, Long>
