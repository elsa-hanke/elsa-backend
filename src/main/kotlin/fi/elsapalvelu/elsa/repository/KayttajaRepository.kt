package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long> {

    fun findOneByUserId(id: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join k.user u join fetch k.yliopistotAndErikoisalat where u.id = :id")
    fun findOneByUserIdWithErikoisalat(id: String): Optional<Kayttaja>

    fun findOneByUserEmail(email: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join k.user u left join u.authorities a where a.name in :authorities")
    fun findAllByUserAuthorities(authorities: List<String>): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat y " +
            "where a.name in :authorities and k.id = y.kayttaja.id and y.yliopisto.id = :yliopistoId and y.erikoisala.id = :erikoisalaId"
    )
    fun findAllByAuthoritiesAndYliopistoAndErikoisala(
        authorities: List<String>,
        yliopistoId: Long?,
        erikoisalaId: Long?
    ): MutableList<Kayttaja>
}
