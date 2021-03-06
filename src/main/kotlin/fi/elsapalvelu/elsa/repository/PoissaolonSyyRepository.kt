package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PoissaolonSyyRepository : JpaRepository<PoissaolonSyy, Long>
