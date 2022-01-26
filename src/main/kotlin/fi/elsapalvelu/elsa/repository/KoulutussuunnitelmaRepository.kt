package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface KoulutussuunnitelmaRepository : JpaRepository<Koulutussuunnitelma, Long> {

    fun findOneByOpintooikeusId(opintooikeusId: Long): Koulutussuunnitelma?

    @Query("select k from Koulutussuunnitelma k where k.koulutussuunnitelmaAsiakirja.id = :id or " +
        "k.motivaatiokirjeAsiakirja.id = :id")
    fun findOneByKoulutussuunnitelmaAsiakirjaIdOrMotivaatiokirjeAsiakirjaId(id: Long): Koulutussuunnitelma?
}
