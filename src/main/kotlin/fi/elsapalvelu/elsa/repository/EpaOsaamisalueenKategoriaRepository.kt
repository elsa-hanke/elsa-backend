package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.EpaOsaamisalueenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EpaOsaamisalueenKategoriaRepository : JpaRepository<EpaOsaamisalueenKategoria, Long>
