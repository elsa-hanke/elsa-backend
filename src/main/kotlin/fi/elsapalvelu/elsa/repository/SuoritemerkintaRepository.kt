package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritemerkinta
import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface SuoritemerkintaRepository : JpaRepository<Suoritemerkinta, Long> {
    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id: String): List<Suoritemerkinta>

    @Query(
        "select m from Suoritemerkinta m " +
            "join m.tyoskentelyjakso t " +
            "join t.erikoistuvaLaakari e " +
            "join e.kayttaja k " +
            "join k.user u " +
            "where m.suorituspaiva between :alkamispaiva and :paattymispaiva and u.id = :userId"
    )
    fun findForSeurantajakso(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<Suoritemerkinta>

    fun findOneById(id: Long): Optional<Suoritemerkinta>
}
