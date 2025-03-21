package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymysVaihtoehto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluKysymysVaihtoehtoRepository : JpaRepository<ArviointityokaluKysymysVaihtoehto, Long>
