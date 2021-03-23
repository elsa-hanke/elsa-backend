package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Koejakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KoejaksoRepository : JpaRepository<Koejakso, Long> {

    fun findByErikoistuvaLaakariKayttajaUserId(id: String): Koejakso
}
