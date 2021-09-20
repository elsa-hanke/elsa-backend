package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ArvioitavanKokonaisuudenKategoriaRepository : JpaRepository<ArvioitavanKokonaisuudenKategoria, Long> {

    @Query("select k from ArvioitavanKokonaisuudenKategoria k where k.voimassaoloAlkaa <= ?1 and (k.voimassaoloLoppuu is null or k.voimassaoloLoppuu >= ?1)")
    fun findAllValid(now: LocalDate): List<ArvioitavanKokonaisuudenKategoria>
}
