package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.PaivakirjaAihekategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaivakirjaAihekategoriaRepository : JpaRepository<PaivakirjaAihekategoria, Long>
