package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.SuoritusarvioinninKommentti
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SuoritusarvioinninKommenttiRepository : JpaRepository<SuoritusarvioinninKommentti, Long>
