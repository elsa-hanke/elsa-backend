package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface TeoriakoulutusRepository : JpaRepository<Teoriakoulutus, Long> {

    fun findAllByOpintooikeusId(opintooikeusId: Long): MutableList<Teoriakoulutus>

    fun findOneByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Teoriakoulutus?

    fun deleteByIdAndOpintooikeusId(id: Long, opintooikeusId: Long)

    @Query(
        """
        select tk
        from Teoriakoulutus tk
        join tk.opintooikeus o
        join o.erikoistuvaLaakari e
        join e.kayttaja k
        join k.user u
        where tk.alkamispaiva between :alkamispaiva and :paattymispaiva and u.id = :userId
        """
    )
    fun findForSeurantajakso(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<Teoriakoulutus>
}
