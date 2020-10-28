package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Authority
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityRepository : JpaRepository<Authority, String>
