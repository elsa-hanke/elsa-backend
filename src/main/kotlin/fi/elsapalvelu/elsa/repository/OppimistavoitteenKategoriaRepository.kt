package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OppimistavoitteenKategoriaRepository : JpaRepository<OppimistavoitteenKategoria, Long> {

    fun findAllByErikoisalaId(id: Long): List<OppimistavoitteenKategoria>
}
