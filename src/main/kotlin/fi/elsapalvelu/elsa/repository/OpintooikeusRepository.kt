package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintooikeus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface OpintooikeusRepository : JpaRepository<Opintooikeus, Long> {

    fun existsByErikoistuvaLaakariKayttajaUserId(userId: String): Boolean
}
