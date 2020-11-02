package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KouluttajavaltuutusRepository : JpaRepository<Kouluttajavaltuutus, Long> {
    fun findAllByValtuuttajaKayttajaUserId(id: String): List<Kouluttajavaltuutus>
}
