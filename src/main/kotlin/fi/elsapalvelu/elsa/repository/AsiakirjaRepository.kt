package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AsiakirjaRepository : JpaRepository<Asiakirja, Long> {

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
}
