package fi.elsapalvelu.elsa.repository.tyoskentely

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TyoskentelyjaksoRepository : JpaRepository<Tyoskentelyjakso, Long> {

    fun findAllByOpintooikeusId(id: Long): List<Tyoskentelyjakso>

    @Query(
        "select distinct t from Tyoskentelyjakso t " +
            "left join fetch t.keskeytykset " +
            "left join fetch t.suoritusarvioinnit " +
            "left join fetch t.suoritemerkinnat " +
            "where t.opintooikeus.id = :id " +
            "and t.alkamispaiva <= :untilDate"
    )
    fun findAllByOpintooikeusUntilDateEagerWithRelationships(
        id: Long,
        untilDate: LocalDate
    ): List<Tyoskentelyjakso>

    @Query(
        "select t from Tyoskentelyjakso t " +
            "left join fetch t.keskeytykset " +
            "where t.id = :id " +
            "and t.opintooikeus.id = :opintooikeusId"
    )
    fun findOneByIdAndOpintooikeusIdEagerWithKeskeytykset(
        id: Long,
        opintooikeusId: Long
    ): Tyoskentelyjakso?

    fun findOneByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Tyoskentelyjakso?

    fun findAllByOpintooikeusIdAndLiitettyKoejaksoonTrue(opintooikeusId: Long): List<Tyoskentelyjakso>

    fun findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppi(
        id: Long,
        tyyppi: TyoskentelyjaksoTyyppi
    ): List<Tyoskentelyjakso>

    fun findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppiAndKaytannonKoulutus(
        id: Long,
        tyyppi: TyoskentelyjaksoTyyppi,
        kaytannonKoulutus: KaytannonKoulutusTyyppi
    ): List<Tyoskentelyjakso>

    fun existsByOpintooikeusIdAndKaytannonKoulutus(
        id: Long,
        kaytannonKoulutus: KaytannonKoulutusTyyppi
    ): Boolean

}
