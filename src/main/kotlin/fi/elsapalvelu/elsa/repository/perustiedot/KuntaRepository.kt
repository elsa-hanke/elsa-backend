package fi.elsapalvelu.elsa.repository.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.Kunta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KuntaRepository : JpaRepository<Kunta, String>
