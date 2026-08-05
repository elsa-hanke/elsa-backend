package fi.elsapalvelu.elsa.repository.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi
import org.springframework.data.jpa.repository.JpaRepository

interface OpintosuoritusTyyppiRepository : JpaRepository<OpintosuoritusTyyppi, Long>
