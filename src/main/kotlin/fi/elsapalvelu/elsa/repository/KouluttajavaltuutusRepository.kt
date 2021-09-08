package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface KouluttajavaltuutusRepository : JpaRepository<Kouluttajavaltuutus, Long> {
    fun findAllByValtuuttajaKayttajaIdAndPaattymispaivaAfter(id: String, pvm: LocalDate): List<Kouluttajavaltuutus>
    fun findByValtuuttajaKayttajaIdAndValtuutettuIdAndPaattymispaivaAfter(
        valtuuttajaId: String,
        valtuutettuId: String,
        pvm: LocalDate
    ): Optional<Kouluttajavaltuutus>
}
