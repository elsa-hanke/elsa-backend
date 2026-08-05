package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluKysymysRepository : JpaRepository<ArviointityokaluKysymys, Long>
