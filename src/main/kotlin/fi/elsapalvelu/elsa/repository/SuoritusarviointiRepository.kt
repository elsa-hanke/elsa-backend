package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Repository
interface SuoritusarviointiRepository :
    JpaRepository<Suoritusarviointi, Long>,
    JpaSpecificationExecutor<Suoritusarviointi> {

    fun findOneById(id: Long): Optional<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoOpintooikeusId(
        opintooikeusId: Long,
        pageable: Pageable
    ): Page<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoOpintooikeusId(
        opintooikeusId: Long
    ): List<Suoritusarviointi>

    fun findOneByIdAndTyoskentelyjaksoOpintooikeusId(
        id: Long,
        opintooikeusId: Long
    ): Optional<Suoritusarviointi>

    fun findOneByIdAndArvioinninAntajaUserId(
        id: Long,
        userId: String
    ): Optional<Suoritusarviointi>

    @Query(
        """
        select a from Suoritusarviointi a
        join a.tyoskentelyjakso t
        join t.opintooikeus o
        join o.erikoistuvaLaakari e
        join e.kayttaja k
        join k.user u
        where a.tapahtumanAjankohta between :alkamispaiva and :paattymispaiva and u.id = :userId and a.arviointiAika is not null
        """
    )
    fun findForSeurantajakso(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<Suoritusarviointi>

    @Transactional
    @Modifying
    @Query("update Suoritusarviointi s set s.arvioinninAntaja.id = :newKayttaja where s.arvioinninAntaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)
}
