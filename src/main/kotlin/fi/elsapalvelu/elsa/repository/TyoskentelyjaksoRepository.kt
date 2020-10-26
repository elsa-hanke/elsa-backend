package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Suppress("unused")
@Repository
interface TyoskentelyjaksoRepository : JpaRepository<Tyoskentelyjakso, Long> {
    fun findAllByErikoistuvaLaakariKayttajaUserId(id: String): List<Tyoskentelyjakso>
}
