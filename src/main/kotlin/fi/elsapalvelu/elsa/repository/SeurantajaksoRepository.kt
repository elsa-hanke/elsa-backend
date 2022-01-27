package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Seurantajakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SeurantajaksoRepository : JpaRepository<Seurantajakso, Long> {

    fun findByOpintooikeusId(opintooikeusId: Long): List<Seurantajakso>

    fun findByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Seurantajakso?

    fun findByKouluttajaUserId(userId: String): List<Seurantajakso>

    fun findByIdAndKouluttajaUserId(id: Long, userId: String): Seurantajakso?
}
