package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OpintooikeusHerate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OpintooikeusHerateRepository : JpaRepository<OpintooikeusHerate, Long> {

    fun findOneByErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusHerate?

}
