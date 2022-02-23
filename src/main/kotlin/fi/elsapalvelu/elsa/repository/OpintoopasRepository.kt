package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintoopas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface OpintoopasRepository : JpaRepository<Opintoopas, Long> {

    fun findAllByErikoisalaId(erikoisalaId: Long): List<Opintoopas>

    fun findOneByVoimassaoloAlkaa(voimassaoloAlkaa: LocalDate): Opintoopas?

}
