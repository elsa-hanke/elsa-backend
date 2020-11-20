package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.SoteOrganisaatio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface SoteOrganisaatioRepository :
    JpaRepository<SoteOrganisaatio, String>,
    JpaSpecificationExecutor<SoteOrganisaatio>
