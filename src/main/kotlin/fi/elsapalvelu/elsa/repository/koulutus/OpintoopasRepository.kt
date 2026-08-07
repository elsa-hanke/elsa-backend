package fi.elsapalvelu.elsa.repository.koulutus

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.koulutus.Opintoopas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository


@Repository
interface OpintoopasRepository : JpaRepository<Opintoopas, Long> {

    fun findAllByErikoisalaId(erikoisalaId: Long): List<Opintoopas>

    @Query(
        """
        select o from Opintoopas o
        join o.erikoisala e
        where e.id = :erikoisalaId and ((:voimassaDate between o.voimassaoloAlkaa and o.voimassaoloPaattyy)
        or (:voimassaDate >= o.voimassaoloAlkaa and o.voimassaoloPaattyy is null))
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
