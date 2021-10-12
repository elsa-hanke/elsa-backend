package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ArvioitavanKokonaisuudenKategoriaRepository : JpaRepository<ArvioitavanKokonaisuudenKategoria, Long> {

    @Query(
        "select k from ArvioitavanKokonaisuudenKategoria k " +
            "where k.voimassaoloAlkaa <= ?1 and (k.voimassaoloLoppuu is null or k.voimassaoloLoppuu >= ?1)"
    )
    fun findAllValid(now: LocalDate): List<ArvioitavanKokonaisuudenKategoria>

    @Query(
        "select akk from ArvioitavanKokonaisuudenKategoria akk " +
            "left join fetch akk.arvioitavatKokonaisuudet ak " +
            "left join ak.erikoisala e " +
            "where e.id = ?1 " +
            "and ak.kategoria.id = akk.id " +
            "and ak.voimassaoloAlkaa <= ?2 " +
            "and (ak.voimassaoloLoppuu is null or ak.voimassaoloLoppuu >= ?2) " +
            "and akk.voimassaoloAlkaa <= ?2 " +
            "and (akk.voimassaoloLoppuu is null or akk.voimassaoloLoppuu >= ?2)"
    )
    fun findAllByErikoisalaIdAndValid(id: Long?, valid: LocalDate): List<ArvioitavanKokonaisuudenKategoria>
}
