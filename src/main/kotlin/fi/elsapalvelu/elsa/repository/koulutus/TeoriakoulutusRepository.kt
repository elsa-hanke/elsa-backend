package fi.elsapalvelu.elsa.repository.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository


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
        where tk.alkamispaiva between :alkamispaiva and :paattymispaiva and o.id = :opintooikeusId
        """
    )
    fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<Teoriakoulutus>
}
