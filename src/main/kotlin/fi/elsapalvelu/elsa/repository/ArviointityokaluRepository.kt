package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Arviointityokalu
import fi.elsapalvelu.elsa.domain.ArviointityokaluKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluRepository : JpaRepository<Arviointityokalu, Long> {

    @Query("select a from Arviointityokalu a left join a.kayttaja k left join k.user u where a.kayttaja is null or (u.id = ?1)")
    fun findAllByKayttajaIsNullOrKayttajaUserId(id: String): List<Arviointityokalu>

    fun findAllByIdIn(ids: List<Long?>): MutableSet<Arviointityokalu>?

    fun findAllByKaytossaTrue(): List<Arviointityokalu>

    fun findAllByKategoria(arviointityokaluKategoria: ArviointityokaluKategoria): List<Arviointityokalu>

}
