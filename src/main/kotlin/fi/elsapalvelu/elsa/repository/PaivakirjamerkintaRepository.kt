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

    fun findAllByErikoistuvaLaakariKayttajaUserId(pageable: Pageable, userId: String): Page<Paivakirjamerkinta>

    fun findOneByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Paivakirjamerkinta?

    fun deleteByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String)

}
