package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Erikoisala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ErikoisalaRepository : JpaRepository<Erikoisala, Long> {

    fun findOneByVirtaPatevyyskoodi(virtaPatevyyskoodi: String): Erikoisala?

    fun findAllByLiittynytElsaanTrue(): List<Erikoisala>
}
