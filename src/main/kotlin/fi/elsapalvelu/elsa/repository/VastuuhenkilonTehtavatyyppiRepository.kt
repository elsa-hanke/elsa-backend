package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.VastuuhenkilonTehtavatyyppi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VastuuhenkilonTehtavatyyppiRepository : JpaRepository<VastuuhenkilonTehtavatyyppi, Long>
