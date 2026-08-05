package fi.elsapalvelu.elsa.repository.seuranta

import fi.elsapalvelu.elsa.domain.seuranta.PaivakirjaAihekategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaivakirjaAihekategoriaRepository : JpaRepository<PaivakirjaAihekategoria, Long>
