package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SuoritusarviointiRepository : JpaRepository<Suoritusarviointi, Long>,
    JpaSpecificationExecutor<Suoritusarviointi> {

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariId(id: Long, pageable: Pageable): Page<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String,
        pageable: Pageable
    ): Page<Suoritusarviointi>

    fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<Suoritusarviointi>
}
