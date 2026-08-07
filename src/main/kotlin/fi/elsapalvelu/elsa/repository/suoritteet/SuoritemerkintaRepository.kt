package fi.elsapalvelu.elsa.repository.suoritteet

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SuoritemerkintaRepository : JpaRepository<Suoritemerkinta, Long> {
    fun findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId: Long): List<Suoritemerkinta>

    @Query(
        """
        select m from Suoritemerkinta m
        join m.tyoskentelyjakso t
        join t.opintooikeus o
        where m.suorituspaiva between :alkamispaiva and :paattymispaiva and o.id = :opintooikeusId
        """
    )
    fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<Suoritemerkinta>

    fun findOneById(id: Long): Optional<Suoritemerkinta>

    fun existsBySuoriteId(suoriteId: Long): Boolean
}
