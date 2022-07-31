package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintoopas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface OpintoopasRepository : JpaRepository<Opintoopas, Long> {

    fun findAllByErikoisalaId(erikoisalaId: Long): List<Opintoopas>

    @Query(
        """
        select o from Opintoopas o
        join o.erikoisala e
        where e.id = :erikoisalaId and :voimassaDate between o.voimassaoloAlkaa and o.voimassaoloPaattyy
        """
    )
    fun findOneByErikoisalaIdAndVoimassaDate(
        erikoisalaId: Long,
        voimassaDate: LocalDate
    ): Opintoopas?

    fun findFirstByErikoisalaIdOrderByVoimassaoloAlkaaDesc(
        erikoisalaId: Long
    ): Opintoopas?
}
