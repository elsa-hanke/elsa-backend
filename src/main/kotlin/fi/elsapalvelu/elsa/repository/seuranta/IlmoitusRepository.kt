package fi.elsapalvelu.elsa.repository.seuranta

import fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IlmoitusRepository : JpaRepository<Ilmoitus, Long> {

    fun findAllByOrderByIdDesc(): List<Ilmoitus>
}
