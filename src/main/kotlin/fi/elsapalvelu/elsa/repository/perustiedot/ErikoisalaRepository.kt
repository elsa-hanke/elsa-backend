package fi.elsapalvelu.elsa.repository.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ErikoisalaRepository : JpaRepository<Erikoisala, Long> {

    fun findOneByVirtaPatevyyskoodi(virtaPatevyyskoodi: String): Erikoisala?

    fun findAllByLiittynytElsaanTrue(): List<Erikoisala>

    fun findAllByIdIs(erikoisalaId: Long): List<Erikoisala>

}
