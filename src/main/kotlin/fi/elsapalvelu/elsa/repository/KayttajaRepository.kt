package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KayttajaRepository : JpaRepository<Kayttaja, Long>, JpaSpecificationExecutor<Kayttaja> {

    fun findOneByUserId(id: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join fetch k.user u join fetch u.authorities where u.id = :id")
    fun findOneByUserIdWithAuthorities(id: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join k.user u left join fetch k.yliopistot left join fetch k.yliopistotAndErikoisalat ye left join fetch ye.vastuuhenkilonTehtavat where u.id = :id")
    fun findOneByUserIdWithErikoisalat(id: String): Optional<Kayttaja>

    fun findOneByUserEmail(email: String): Optional<Kayttaja>

    @Query("select k from Kayttaja k join k.user u left join u.authorities a where a.name in :authorities")
    fun findAllByUserAuthorities(authorities: List<String>): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat y " +
            "where a.name in :authorities and k.id = y.kayttaja.id and y.yliopisto.id = :yliopistoId and y.erikoisala.id = :erikoisalaId " +
            "and k.tila != fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila.PASSIIVINEN"
    )
    fun findAllByAuthoritiesAndYliopistoAndErikoisala(
        authorities: List<String>,
        yliopistoId: Long?,
        erikoisalaId: Long?
    ): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat y " +
            "where a.name in :authorities and k.id = y.kayttaja.id and y.yliopisto.id = :yliopistoId " +
            "and k.tila != fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila.PASSIIVINEN"
    )
    fun findAllByAuthoritiesAndYliopisto(
        authorities: List<String>,
        yliopistoId: Long?
    ): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat y " +
            "where a.name in :authorities and k.id = y.kayttaja.id and y.erikoisala.id = :erikoisalaId " +
            "and k.tila != fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila.PASSIIVINEN"
    )
    fun findAllByAuthoritiesAndErikoisala(
        authorities: List<String>,
        erikoisalaId: Long?
    ): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistot y " +
            "where a.name in :authorities and y.id = :yliopistoId"
    )
    fun findAllByAuthoritiesAndRelYliopisto(
        authorities: List<String>,
        yliopistoId: Long?
    ): MutableList<Kayttaja>

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat ye " +
            "left join ye.vastuuhenkilonTehtavat vt where a.name in :authorities and k.id = ye.kayttaja.id " +
            "and ye.yliopisto.id = :yliopistoId and ye.erikoisala.id = :erikoisalaId and vt.nimi = :vastuuhenkilonTehtavatyyppi"
    )
    fun findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
        authorities: List<String>,
        yliopistoId: Long?,
        erikoisalaId: Long?,
        vastuuhenkilonTehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum
    ): Kayttaja?

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat ye " +
            "left join ye.vastuuhenkilonTehtavat vt where a.name in :authorities and k.id = ye.kayttaja.id " +
            "and ye.yliopisto.id = :yliopistoId and vt.nimi = :vastuuhenkilonTehtavatyyppi"
    )
    fun findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
        authorities: List<String>,
        yliopistoId: Long?,
        vastuuhenkilonTehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum
    ): Kayttaja?

    @Query(
        "select k from Kayttaja k join k.user u left join u.authorities a left join k.yliopistotAndErikoisalat y " +
            "where a.name in :authorities and k.id = y.kayttaja.id and y.yliopisto.id in :yliopistoIds " +
            "and y.erikoisala.id in :erikoisalaIds"
    )
    fun findAllByAuthoritiesAndYliopistotAndErikoisalat(
        authorities: List<String>,
        yliopistoIds: List<Long?>,
        erikoisalaIds: List<Long?>
    ): MutableList<Kayttaja>
}
