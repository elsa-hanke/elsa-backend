package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintooikeus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OpintooikeusRepository : JpaRepository<Opintooikeus, Long> {

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
        select case when count(o) > 0 then true else false end from Opintooikeus o
        join o.erikoistuvaLaakari e
        join e.kayttaja k
        join k.user u
        where :paiva between o.opintooikeudenMyontamispaiva and o.opintooikeudenPaattymispaiva and u.id = :userId
        """
    )
    fun findByErikoistuvaLaakariKayttajaUserIdAndBetweenDate(
        userId: String,
        paiva: LocalDate
    ): List<Opintooikeus>

    @Query(
        """
        select o from Opintooikeus o
        where o.kaytossa = true and current_date between o.opintooikeudenMyontamispaiva and o.opintooikeudenPaattymispaiva
        and o.erikoisala.id = :erikoisalaId and o.yliopisto.id = :yliopistoId
        """
    )
    fun findByErikoisalaAndYliopisto(erikoisalaId: Long, yliopistoId: Long): List<Opintooikeus>

    @Query(
        """
        select o from Opintooikeus o
        join o.erikoistuvaLaakari e
        join e.annetutValtuutukset v
        where o.kaytossa = true and current_date between o.opintooikeudenMyontamispaiva and o.opintooikeudenPaattymispaiva
        and v.valtuutettu.id = :kayttajaId
        and current_date between v.alkamispaiva and v.paattymispaiva
        """
    )
    fun findByKouluttajaValtuutus(kayttajaId: Long): List<Opintooikeus>
}
