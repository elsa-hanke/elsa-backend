package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Suppress("unused")
@Repository
interface SuoritusarviointiRepository : JpaRepository<Suoritusarviointi, Long> {

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariId(id: Long): MutableList<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: String,
        pageable: Pageable
    ): Page<Suoritusarviointi>
}
