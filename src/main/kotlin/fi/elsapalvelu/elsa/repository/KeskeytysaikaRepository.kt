package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KeskeytysaikaRepository : JpaRepository<Keskeytysaika, Long> {

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id: String): List<Keskeytysaika>

    fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Keskeytysaika?
}
