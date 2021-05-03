package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.service.projection.AsiakirjaItemProjection
import fi.elsapalvelu.elsa.service.projection.AsiakirjaListProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AsiakirjaRepository : JpaRepository<Asiakirja, Long> {

    @Query(
        "select a.id, a.nimi, a.lisattypvm from asiakirja a left join erikoistuva_laakari e on a.erikoistuva_laakari_id = e.id left join kayttaja k " +
            "on e.kayttaja_id = k.id where k.user_id = :userId", nativeQuery = true
    )
    fun findAllByErikoistuvaLaakari(userId: String): List<AsiakirjaListProjection>

    @Query(
        "select a.id, a.nimi, a.lisattypvm from asiakirja a left join erikoistuva_laakari e on a.erikoistuva_laakari_id = e.id left join kayttaja k " +
            "on e.kayttaja_id = k.id where k.user_id = :userId and a.tyoskentelyjakso_id = :tyoskentelyJaksoId", nativeQuery = true
    )
    fun findAllByErikoistuvaLaakariAndTyoskentelyjakso(
        userId: String,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaListProjection>

    @Query(
        "select a.nimi, a.tyyppi, a.data from asiakirja a left join erikoistuva_laakari e on a.erikoistuva_laakari_id = e.id left join kayttaja k " +
            "on e.kayttaja_id = k.id where k.user_id = :userId and a.id = :asiakirjaId", nativeQuery = true
    )
    fun findAsiakirjaByIdAndErikoistuvaLaakari(userId: String, asiakirjaId: Long): AsiakirjaItemProjection?

}
