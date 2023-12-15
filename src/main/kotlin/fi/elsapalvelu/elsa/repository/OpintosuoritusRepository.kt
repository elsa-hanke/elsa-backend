package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintosuoritus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OpintosuoritusRepository : JpaRepository<Opintosuoritus, Long> {

    fun findOneByOpintooikeusYliopistoOpintooikeusIdAndKurssikoodi(
        yliopistoOpintooikeusId: String,
        kurssikoodi: String
    ): Opintosuoritus?

    @Query(
        """
            select distinct o from Opintosuoritus o left join fetch o.osakokonaisuudet
            where o.opintooikeus.id = :opintooikeusId and o.kurssikoodi not in
            (select kk.tunniste from OpintosuoritusKurssikoodi kk where kk.isOsakokonaisuus = true)
        """
    )
    fun findAllByOpintooikeusId(opintooikeusId: Long): List<Opintosuoritus>

}
