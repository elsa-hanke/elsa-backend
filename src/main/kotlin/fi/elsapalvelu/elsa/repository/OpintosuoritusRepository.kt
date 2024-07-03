package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintosuoritus
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
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

    @Query(
        """
            select distinct os from Opintosuoritus os join os.opintooikeus oo left join fetch os.osakokonaisuudet
            where oo.erikoistuvaLaakari.id = :erikoistuvaLaakariId and oo.erikoisala.id = :erikoisalaId and os.kurssikoodi not in
            (select kk.tunniste from OpintosuoritusKurssikoodi kk where kk.isOsakokonaisuus = true)
        """
    )
    fun findAllByErikoistuvaLaakariIdAndErikoisalaId(erikoistuvaLaakariId: Long, erikoisalaId: Long): List<Opintosuoritus>

    @Query(
        """
            select distinct o from Opintosuoritus o left join fetch o.osakokonaisuudet
            where o.opintooikeus.id = :opintooikeusId and o.tyyppi.nimi = :tyyppi
            and o.kurssikoodi not in
            (select kk.tunniste from OpintosuoritusKurssikoodi kk where kk.isOsakokonaisuus = true)
        """
    )
    fun findAllByOpintooikeusIdAndTyyppi(opintooikeusId: Long, tyyppi: OpintosuoritusTyyppiEnum): List<Opintosuoritus>

}
