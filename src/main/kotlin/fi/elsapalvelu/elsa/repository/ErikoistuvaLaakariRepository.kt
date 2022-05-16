package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long>,
    JpaSpecificationExecutor<ErikoistuvaLaakari> {

    fun findOneByKayttajaUserId(userId: String): ErikoistuvaLaakari?

    @Query("select distinct e from ErikoistuvaLaakari e join fetch e.opintooikeudet where e.id = :id")
    fun findByIdWithOpintooikeudet(id: Long): ErikoistuvaLaakari?

    fun findOneByKayttajaId(kayttajaId: Long): ErikoistuvaLaakari?

    @Query(
        """
        select distinct e from ErikoistuvaLaakari e
        join e.opintooikeudet o
        where o.kaytossa = true and current_time between o.opintooikeudenMyontamispaiva and o.opintooikeudenPaattymispaiva
        and o.erikoisala.id = :erikoisalaId and o.yliopisto.id = :yliopistoId
        """
    )
    fun findAllForVastuuhenkilo(erikoisalaId: Long, yliopistoId: Long): List<ErikoistuvaLaakari>

    @Query(
        """
        select distinct e from ErikoistuvaLaakari e
        join e.opintooikeudet o
        where o.yliopisto.id = :yliopistoId
        """
    )
    fun findAllByYliopistoId(pageable: Pageable, yliopistoId: Long): Page<ErikoistuvaLaakari>
}
