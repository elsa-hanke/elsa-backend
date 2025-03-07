package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintooikeus
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

    fun findOneByErikoisalaIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Opintooikeus?

    fun findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrueAndErikoisalaId(userId: String, erikoisalaId: Long): Opintooikeus?

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
        where current_date between o.opintooikeudenMyontamispaiva and o.viimeinenKatselupaiva
        and o.erikoisala.liittynytElsaan = true and u.id = :userId
        """
    )
    fun findAllValidByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<Opintooikeus>

    @Query(
        """
        select o from Opintooikeus o
        where current_date between o.opintooikeudenMyontamispaiva and o.viimeinenKatselupaiva
        and o.erikoisala.liittynytElsaan = true
        """
    )
    fun findAllValid(): List<Opintooikeus>

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

    fun findOneById(id: Long): Opintooikeus?

    @Query(
        """
        select o from Opintooikeus o
        where current_date between o.opintooikeudenMyontamispaiva and o.viimeinenKatselupaiva
        and o.erikoisala.liittynytElsaan = true
        and o.terveyskoulutusjaksoSuoritettu = false
        """
    )
    fun findAllByTerveyskoulutusjaksoSuorittamatta(
    ): List<Opintooikeus>

    @Query(
        """
            select o from Opintooikeus o
            where o.opintooikeudenPaattymispaiva between :from and :to
            and current_date between o.opintooikeudenMyontamispaiva and o.viimeinenKatselupaiva
            and o.erikoisala.liittynytElsaan = true
        """
    )
    fun findAllPaattyvatByTimeFrame(
        from: LocalDate,
        to: LocalDate
    ): List<Opintooikeus>
}
