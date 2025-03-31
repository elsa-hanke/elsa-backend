package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymys
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluKysymysRepository : JpaRepository<ArviointityokaluKysymys, Long>
