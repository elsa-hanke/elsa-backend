package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Arviointityokalu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluRepository : JpaRepository<Arviointityokalu, Long> {

    @Query("select a from Arviointityokalu a left join a.kayttaja k left join k.user u where a.kayttaja is null or (u.login = ?1)")
    fun findAllByKayttajaIsNullOrKayttajaUserLogin(id: String): List<Arviointityokalu>

    fun findAllByIdIn(ids: List<Long?>): MutableSet<Arviointityokalu>?
}
