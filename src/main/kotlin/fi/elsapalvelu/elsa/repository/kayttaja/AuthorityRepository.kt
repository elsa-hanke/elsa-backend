package fi.elsapalvelu.elsa.repository.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityRepository : JpaRepository<Authority, String>
