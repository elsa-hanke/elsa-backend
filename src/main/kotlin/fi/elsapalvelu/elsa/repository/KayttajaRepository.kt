package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long> {

    fun findOneByUserId(id: String): Optional<Kayttaja>

    fun findOneByUserLogin(id: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join k.user u left join u.authorities a where a.name = :authority")
    fun findAllByUserAuthority(authority: String): MutableList<Kayttaja>
}
