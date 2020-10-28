package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OsaalueenArviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OsaalueenArviointiRepository : JpaRepository<OsaalueenArviointi, Long>
