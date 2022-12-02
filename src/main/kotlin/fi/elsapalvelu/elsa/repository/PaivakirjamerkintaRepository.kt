package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Paivakirjamerkinta
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PaivakirjamerkintaRepository : JpaRepository<Paivakirjamerkinta, Long>,
    JpaSpecificationExecutor<Paivakirjamerkinta> {

    fun findAllByOpintooikeusId(pageable: Pageable, opintooikeusId: Long): Page<Paivakirjamerkinta>

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<Paivakirjamerkinta>

    fun findOneByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Paivakirjamerkinta?

    fun deleteByIdAndOpintooikeusId(id: Long, opintooikeusId: Long)

    fun findAllByTeoriakoulutusId(id: Long): List<Paivakirjamerkinta>

}
