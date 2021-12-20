package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintooikeus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OpintooikeusRepository : JpaRepository<Opintooikeus, Long> {

    fun existsByErikoistuvaLaakariKayttajaUserId(userId: String): Boolean

    fun findOneByErikoistuvaLaakariKayttajaUserId(userId: String): Opintooikeus?

    @Query(
        """
        select case when count(o) > 0 then true else false end from Opintooikeus o
        join o.erikoistuvaLaakari e
        join e.kayttaja k
        join k.user u
        where :paiva between o.opintooikeudenMyontamispaiva and o.opintooikeudenPaattymispaiva and u.id = :userId
        """
    )
    fun existsByErikoistuvaLaakariKayttajaUserId(
        userId: String,
        paiva: LocalDate
    ): Boolean
}
