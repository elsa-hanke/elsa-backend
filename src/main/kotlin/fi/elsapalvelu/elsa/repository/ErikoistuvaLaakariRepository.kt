package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long> {

    @Query(
        "select el" +
            " from ErikoistuvaLaakari el" +
            " inner join fetch el.kayttaja k" +
            " inner join fetch k.user u" +
            " where u.id = ?1"
    )
    fun findOneByKayttajaUserId(id: String): Optional<ErikoistuvaLaakari>
}
