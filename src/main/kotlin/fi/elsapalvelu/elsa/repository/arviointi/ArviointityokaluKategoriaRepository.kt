package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluKategoriaRepository : JpaRepository<ArviointityokaluKategoria, Long> {

    fun findAllByIdIn(ids: List<Long?>): MutableSet<ArviointityokaluKategoria>?

    fun findAllByKaytossaTrue(): List<ArviointityokaluKategoria>

}
