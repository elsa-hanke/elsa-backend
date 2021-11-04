package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Seurantajakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SeurantajaksoRepository : JpaRepository<Seurantajakso, Long> {

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): List<Seurantajakso>

    fun findByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Seurantajakso?

    fun deleteByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String)
}
