package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OpintooikeusRepository : JpaRepository<Opintooikeus, Long>,
    JpaSpecificationExecutor<Opintooikeus> {

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<Opintooikeus>

    fun findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId: String): Opintooikeus?

    fun findOneByErikoistuvaLaakariIdAndYliopistoOpintooikeusId(
        erikoistuvaLaakariId: Long,
        yliopistoOpintooikeusId: String
    ): Opintooikeus?

    fun findOneByErikoistuvaLaakariIdAndYliopistoIdAndErikoisalaIdAndYliopistoOpintooikeusIdIsNull(
        erikoistuvaLaakariId: Long,
        yliopistoId: Long,
        erikoisalaId: Long
    ): Opintooikeus?

    @Query(
        """
        select o from Opintooikeus o
        join o.erikoistuvaLaakari e
        join e.kayttaja k
        join k.user u
        where :now >= o.opintooikeudenMyontamispaiva
        and (o.tila in :validStates or (o.tila in :endedStates and :now <= o.viimeinenKatselupaiva))
        and o.erikoisala.liittynytElsaan = true and u.id = :userId
        """
    )
    fun findAllValidByErikoistuvaLaakariKayttajaUserId(
        userId: String,
        now: LocalDate,
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>
    ): List<Opintooikeus>

    @Query(
        """
        select o from Opintooikeus o
        where :now >= o.opintooikeudenMyontamispaiva
        and (o.tila in :validStates or (o.tila in :endedStates and :now <= o.viimeinenKatselupaiva))
        and o.erikoisala.liittynytElsaan = true
        """
    )
    fun findAllValid(
        now: LocalDate,
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>
    ): List<Opintooikeus>

    @Query(
        """
        select o from Opintooikeus o
        join o.erikoistuvaLaakari e
        where o.id = :id and e.id = :erikoistuvaLaakariId
        and :paiva >= o.opintooikeudenMyontamispaiva
        """
    )
    fun findOneByIdAndErikoistuvaLaakariIdAndBetweenDate(
        id: Long,
        erikoistuvaLaakariId: Long,
        paiva: LocalDate
    ): Opintooikeus?

    @Query(
        """
        select o from Opintooikeus o
        where current_date >= o.opintooikeudenMyontamispaiva
        and (o.tila in :validStates or (o.tila in :endedStates and current_date <= o.viimeinenKatselupaiva))
        and o.erikoisala.id = :erikoisalaId and o.yliopisto.id = :yliopistoId
        """
    )
    fun findByErikoisalaAndYliopisto(
        erikoisalaId: Long,
        yliopistoId: Long,
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>,
        pageable: Pageable
    ): Page<Opintooikeus>

    @Query(
        """
        select o from Opintooikeus o
        join o.erikoistuvaLaakari e
        join o.annetutValtuutukset v
        where current_date >= o.opintooikeudenMyontamispaiva
        and (o.tila in :validStates or (o.tila in :endedStates and current_date <= o.viimeinenKatselupaiva))
        and v.valtuutettu.id = :kayttajaId
        and current_date between v.alkamispaiva and v.paattymispaiva
        """
    )
    fun findByKouluttajaValtuutus(
        kayttajaId: Long,
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>
    ): List<Opintooikeus>

    fun findOneById(id: Long): Opintooikeus?

    @Query(
        """
        select o from Opintooikeus o
        where :now >= o.opintooikeudenMyontamispaiva
        and o.tila in :validStates and o.erikoisala.liittynytElsaan = true
        and o.terveyskoulutusjaksoSuoritettu = false
        """
    )
    fun findAllByTerveyskoulutusjaksoSuorittamatta(
        now: LocalDate,
        validStates: List<OpintooikeudenTila>
    ): List<Opintooikeus>
}
