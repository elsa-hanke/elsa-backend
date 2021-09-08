package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, String> {

    @Query("select k from Kayttaja k left join k.authorities a where a.name = :authority")
    fun findAllByAuthority(authority: String): List<Kayttaja>

    @Query(
        "select k from Kayttaja k left join k.authorities a where a.name = :authority " +
            "and k.yliopisto.id = :yliopistoId"
    )
    fun findAllByAuthorityAndYliopistoId(authority: String, yliopistoId: Long?): List<Kayttaja>

    @Query("select k from Kayttaja k left join fetch k.authorities")
    fun findAllWithAuthorities(): List<Kayttaja>

    fun findOneBySahkopostiosoite(sahkopostiosoite: String): Optional<Kayttaja>
}
