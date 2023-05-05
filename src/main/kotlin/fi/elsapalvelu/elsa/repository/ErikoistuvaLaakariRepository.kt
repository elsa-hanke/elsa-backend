package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long>,
    JpaSpecificationExecutor<ErikoistuvaLaakari> {

    fun findOneByKayttajaUserId(userId: String): ErikoistuvaLaakari?

    fun findOneByKayttajaId(kayttajaId: Long): ErikoistuvaLaakari?

    @Query(
        """
        select distinct e from ErikoistuvaLaakari e join fetch e.opintooikeudet o where e.kayttaja.user.id = :userId
        and :now >= o.opintooikeudenMyontamispaiva
        and (o.tila in :validStates or (o.tila in :endedStates and :now <= o.viimeinenKatselupaiva))
        and o.erikoisala.liittynytElsaan = true
        """
    )
    fun findOneByKayttajaUserIdWithValidOpintooikeudet(
        userId: String,
        now: LocalDate,
        validStates: List<OpintooikeudenTila>,
        endedStates: List<OpintooikeudenTila>
    ): ErikoistuvaLaakari?
}
