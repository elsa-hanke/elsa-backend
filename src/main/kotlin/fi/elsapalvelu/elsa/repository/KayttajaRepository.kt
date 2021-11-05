package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long> {

    fun findOneByUserId(id: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join k.user u left join u.authorities a where a.name in :authorities")
    fun findAllByUserAuthorities(authorities: List<String>): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistot y " +
            "left join k.erikoisalat e where a.name in :authorities " +
            "and :yliopistoId in (y) and :erikoisalaId in (e)"
    )
    fun findAllByAuthoritiesAndYliopistoAndErikoisala(
        authorities: List<String>,
        yliopistoId: Long?,
        erikoisalaId: Long?
    ): MutableList<Kayttaja>
}
