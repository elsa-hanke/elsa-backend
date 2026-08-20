package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninKommentti
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SuoritusarvioinninKommenttiRepository : JpaRepository<SuoritusarvioinninKommentti, Long>
