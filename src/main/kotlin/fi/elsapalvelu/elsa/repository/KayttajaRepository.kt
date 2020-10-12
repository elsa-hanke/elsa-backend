package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data  repository for the [Kayttaja] entity.
 */
@Suppress("unused")
@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long>
