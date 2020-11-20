package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritemerkinta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SuoritemerkintaRepository : JpaRepository<Suoritemerkinta, Long> {
    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id: String): List<Suoritemerkinta>

    fun findOneById(id: Long): Optional<Suoritemerkinta>
}
