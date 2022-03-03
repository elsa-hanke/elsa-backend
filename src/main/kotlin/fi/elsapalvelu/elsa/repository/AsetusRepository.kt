package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asetus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AsetusRepository : JpaRepository<Asetus, Long> {

    fun findOneByNimi(nimi: String): Asetus?

}
