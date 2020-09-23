package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.Tyoskentelypaikka
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Tyoskentelypaikka] entity.
 */
@Suppress("unused")
@Repository
interface TyoskentelypaikkaRepository : JpaRepository<Tyoskentelypaikka, Long>
