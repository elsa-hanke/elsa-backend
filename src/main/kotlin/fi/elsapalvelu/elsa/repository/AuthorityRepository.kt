package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityRepository : JpaRepository<Authority, String>
