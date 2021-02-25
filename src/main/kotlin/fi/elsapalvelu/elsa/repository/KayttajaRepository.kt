package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long> {

    fun findOneByUserId(id: String): Optional<Kayttaja>

    fun findOneByUserLogin(id: String): Optional<Kayttaja>
}
