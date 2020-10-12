package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Tyoskentelyjakso] entity.
 */
@Suppress("unused")
@Repository
interface TyoskentelyjaksoRepository : JpaRepository<Tyoskentelyjakso, Long>
