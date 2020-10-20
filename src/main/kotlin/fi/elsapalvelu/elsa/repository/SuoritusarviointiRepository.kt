package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SuoritusarviointiRepository : JpaRepository<Suoritusarviointi, Long> {

    @Query("select s" +
        " from Suoritusarviointi s" +
        " inner join fetch s.tyoskentelyjakso t" +
        " inner join fetch t.erikoistuvaLaakari el" +
        " where el.id = ?1")
    fun findAllByErikoistuvaLaakari(id: Long): MutableList<Suoritusarviointi>

    /*
    @Query("select s" +
        " from Suoritusarviointi s" +
        " inner join fetch s.tyoskentelyjakso t" +
        " inner join fetch t.erikoistuvaLaakari el" +
        " inner join fetch el.kayttaja k" +
        " inner join fetch k.user u" +
        " where u.id = ?1")
     */
    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: String,
        pageable: Pageable
    ): Page<Suoritusarviointi>
}
