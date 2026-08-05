package fi.elsapalvelu.elsa.repository.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeusHerate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OpintooikeusHerateRepository : JpaRepository<OpintooikeusHerate, Long> {

    fun findOneByErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusHerate?

}
