package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Oppimistavoite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OppimistavoiteRepository : JpaRepository<Oppimistavoite, Long>
