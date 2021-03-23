package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KoejaksonKoulutussopimusRepository : JpaRepository<KoejaksonKoulutussopimus, Long>
