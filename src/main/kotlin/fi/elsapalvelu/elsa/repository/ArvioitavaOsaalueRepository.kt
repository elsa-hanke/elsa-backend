package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArvioitavaOsaalueRepository : JpaRepository<ArvioitavaOsaalue, Long>
