package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintosuoritus
import org.springframework.data.jpa.repository.JpaRepository

interface OpintosuoritusRepository : JpaRepository<Opintosuoritus, Long> {

    fun findOneByOpintooikeusYliopistoOpintooikeusIdAndKurssikoodi(
        yliopistoOpintooikeusId: String,
        kurssikoodi: String
    ): Opintosuoritus?
}
