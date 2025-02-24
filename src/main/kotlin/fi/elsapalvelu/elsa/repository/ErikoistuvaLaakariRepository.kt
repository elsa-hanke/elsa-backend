package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ErikoistuvaLaakariRepository : JpaRepository<ErikoistuvaLaakari, Long>,
    JpaSpecificationExecutor<ErikoistuvaLaakari> {

    fun findOneByKayttajaUserId(userId: String): ErikoistuvaLaakari?

    fun findOneByKayttajaId(kayttajaId: Long): ErikoistuvaLaakari?

    @Query(
        """
        select distinct e from ErikoistuvaLaakari e join fetch e.opintooikeudet o where e.kayttaja.user.id = :userId
        and current_date between o.opintooikeudenMyontamispaiva and o.viimeinenKatselupaiva
        and o.erikoisala.liittynytElsaan = true
        """
    )
    fun findOneByKayttajaUserIdWithValidOpintooikeudet(userId: String): ErikoistuvaLaakari?
}
