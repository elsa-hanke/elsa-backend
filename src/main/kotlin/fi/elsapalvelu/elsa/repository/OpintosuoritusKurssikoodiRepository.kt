package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OpintosuoritusKurssikoodi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OpintosuoritusKurssikoodiRepository : JpaRepository<OpintosuoritusKurssikoodi, Long> {

    fun findAllByYliopistoNimi(nimi: String): List<OpintosuoritusKurssikoodi>
}
