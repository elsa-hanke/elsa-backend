package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AsiakirjaRepository : JpaRepository<Asiakirja, Long> {

    @Query(
        """
        select a from Asiakirja a join a.opintooikeus o
        where o.id in :opintooikeusId and a.arviointi.id is null and a.itsearviointi.id is null
        """
    )
    fun findAllByOpintooikeusId(opintooikeusId: Long): List<Asiakirja>

    fun findAllByOpintooikeusIdAndTyoskentelyjaksoId(
        opintooikeusId: Long,
        tyoskentelyJaksoId: Long?
    ): List<Asiakirja>

    fun findAllByTyoskentelyjaksoId(tyoskentelyJaksoId: Long?): List<Asiakirja>

    fun findOneByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Asiakirja?

    @Query(
        """
        select a from Asiakirja a join a.tyoskentelyjakso t join t.tyoskentelypaikka p join t.opintooikeus o
        where a.id = :id and p.tyyppi = :tyyppi and o.yliopisto.id in :yliopistoIds
    """
    )
    fun findOneByIdAndTyoskentelyjaksoTyoskentelypaikkaTyyppi(
        id: Long,
        tyyppi: TyoskentelyjaksoTyyppi,
        yliopistoIds: List<Long>
    ): Asiakirja?

    @Query(
        """
        select a from Asiakirja a join a.tyoskentelyjakso t join t.opintooikeus o
        where a.id = :id and t.liitettyKoejaksoon = true and o.yliopisto.id in :yliopistoIds
    """
    )
    fun findOneByIdAndLiitettyKoejaksoon(
        id: Long,
        yliopistoIds: List<Long>
    ): Asiakirja?

    fun findOneByIdAndTyoskentelyjaksoLiitettyKoejaksoonTrue(id: Long): Asiakirja?

    @Query(
        """
        select a from Asiakirja a
        where a.id = :id and a.opintooikeus.id = :opintooikeusId and (a.arviointi.id = :arviointiId or a.itsearviointi.id = :arviointiId)
    """
    )
    fun findOneByIdAndOpintooikeusIdAndArviointiId(
        id: Long,
        opintooikeusId: Long,
        arviointiId: Long
    ): Asiakirja?

    @Query(
        """
        select a from Asiakirja a left join a.arviointi ar left join ar.arvioinninAntaja ara left join ara.user arau left join a.itsearviointi iar left join iar.arvioinninAntaja iara left join iara.user iarau
        where a.id = :id and ((ar.id = :arviointiId and arau.id = :userId) or (iar.id = :arviointiId and iarau.id = :userId))
    """
    )
    fun findOneByIdAndArviointiIdAndArviointiArvioinninAntajaUserId(
        id: Long,
        arviointiId: Long,
        userId: String
    ): Asiakirja?
}
