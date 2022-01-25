package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asiakirja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AsiakirjaRepository : JpaRepository<Asiakirja, Long> {

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<Asiakirja>

    fun findAllByOpintooikeusIdAndTyoskentelyjaksoId(
        opintooikeusId: Long,
        tyoskentelyJaksoId: Long?
    ): List<Asiakirja>

    fun findOneByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Asiakirja?
}
