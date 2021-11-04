package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface TeoriakoulutusRepository : JpaRepository<Teoriakoulutus, Long> {

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<Teoriakoulutus>

    fun findOneByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Teoriakoulutus?

    fun deleteByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String)

    @Query(
        "select tk " +
            "from Teoriakoulutus tk " +
            "join tk.erikoistuvaLaakari e " +
            "join e.kayttaja k " +
            "join k.user u " +
            "where tk.alkamispaiva between :alkamispaiva and :paattymispaiva and u.id = :userId"
    )
    fun findForSeurantajakso(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<Teoriakoulutus>
}
