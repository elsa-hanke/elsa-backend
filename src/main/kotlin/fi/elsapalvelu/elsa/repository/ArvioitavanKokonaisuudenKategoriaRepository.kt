package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ArvioitavanKokonaisuudenKategoriaRepository :
    JpaRepository<ArvioitavanKokonaisuudenKategoria, Long> {

    @Query(
        "select distinct akk from ArvioitavanKokonaisuudenKategoria akk " +
            "left join fetch akk.arvioitavatKokonaisuudet ak " +
            "left join akk.erikoisala e " +
            "where e.id = ?1 " +
            "and ak.kategoria.id = akk.id " +
            "and ak.voimassaoloAlkaa <= ?2 " +
            "and (ak.voimassaoloLoppuu is null or ak.voimassaoloLoppuu >= ?2)"
    )
    fun findAllByErikoisalaIdAndValid(
        id: Long?,
        valid: LocalDate
    ): List<ArvioitavanKokonaisuudenKategoria>

    @Query(
        "select distinct akk from ArvioitavanKokonaisuudenKategoria akk " +
            "left join fetch akk.arvioitavatKokonaisuudet ak " +
            "left join akk.erikoisala e " +
            "where e.id = ?1 " +
            "and ak.kategoria.id = akk.id"
    )
    fun findAllByErikoisalaId(id: Long): List<ArvioitavanKokonaisuudenKategoria>
}
