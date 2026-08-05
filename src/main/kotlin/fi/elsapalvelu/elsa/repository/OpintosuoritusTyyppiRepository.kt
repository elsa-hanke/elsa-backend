package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi
import org.springframework.data.jpa.repository.JpaRepository

interface OpintosuoritusTyyppiRepository : JpaRepository<OpintosuoritusTyyppi, Long>
