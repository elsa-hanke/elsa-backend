package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Arviointiasteikko
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ArviointiasteikkoRepository : JpaRepository<Arviointiasteikko, Long> {

    @Query("select distinct a from Arviointiasteikko a join fetch a.tasot")
    fun findAllWithTasot(): List<Arviointiasteikko>
}
