package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long> {

    fun findOneByKayttajaId(id: String): ErikoistuvaLaakari?
}
