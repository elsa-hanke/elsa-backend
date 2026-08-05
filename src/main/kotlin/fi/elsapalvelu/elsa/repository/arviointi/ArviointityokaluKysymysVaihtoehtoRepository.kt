package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymysVaihtoehto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluKysymysVaihtoehtoRepository : JpaRepository<ArviointityokaluKysymysVaihtoehto, Long>
