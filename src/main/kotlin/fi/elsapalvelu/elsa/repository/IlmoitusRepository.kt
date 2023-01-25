package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Ilmoitus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IlmoitusRepository : JpaRepository<Ilmoitus, Long> {

    fun findAllByOrderByIdDesc(): List<Ilmoitus>
}
