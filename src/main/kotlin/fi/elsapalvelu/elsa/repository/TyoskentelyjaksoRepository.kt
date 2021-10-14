package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TyoskentelyjaksoRepository : JpaRepository<Tyoskentelyjakso, Long> {

    fun findAllByErikoistuvaLaakariKayttajaUserId(id: String): List<Tyoskentelyjakso>

    @Query(
        "select distinct t from Tyoskentelyjakso t " +
            "left join fetch t.keskeytykset " +
            "left join fetch t.suoritusarvioinnit " +
            "left join fetch t.suoritemerkinnat " +
            "where t.erikoistuvaLaakari.kayttaja.user.id = :id " +
            "and t.alkamispaiva <= :untilDate"
    )
    fun findAllByErikoistuvaUntilDateEagerWithRelationships(
        id: String,
        untilDate: LocalDate
    ): List<Tyoskentelyjakso>

    fun findOneByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Tyoskentelyjakso?

    fun findAllByErikoistuvaLaakariKayttajaUserIdAndLiitettyKoejaksoonTrue(userId: String): List<Tyoskentelyjakso>

}
