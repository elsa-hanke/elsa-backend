package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Spring Data  repository for the [ErikoistuvaLaakari] entity.
 */
@Suppress("unused")
@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long> {

    @Query("select el" +
        " from ErikoistuvaLaakari el" +
        " inner join fetch el.kayttaja k" +
        " inner join fetch k.user u" +
        " where u.id = ?1")
    fun findOneByKayttajaUserId(id: String): Optional<ErikoistuvaLaakari>
}
