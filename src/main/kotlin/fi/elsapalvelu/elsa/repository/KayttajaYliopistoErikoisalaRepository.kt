package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KayttajaYliopistoErikoisalaRepository : JpaRepository<KayttajaYliopistoErikoisala, Long>
