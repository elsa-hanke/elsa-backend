package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long>
