package fi.elsapalvelu.elsa.repository.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KeskeytysaikaRepository : JpaRepository<Keskeytysaika, Long> {

    fun findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId: Long): List<Keskeytysaika>

    fun findAllByTyoskentelyjaksoOpintooikeusIdOrderByAlkamispaivaAsc(opintooikeusId: Long): List<Keskeytysaika>

    fun findOneByIdAndTyoskentelyjaksoOpintooikeusId(id: Long, opintooikeusId: Long): Keskeytysaika?
}
