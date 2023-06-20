package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface KoejaksonKoulutussopimusRepository : JpaRepository<KoejaksonKoulutussopimus, Long> {

    fun findOneByIdAndKouluttajatKouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimus>

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimus>

    fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat ko join ko.kouluttaja k " +
            "where k.user.id = :userId and (ks.lahetetty = true or ks.korjausehdotus != null or ks.vastuuhenkilonKorjausehdotus != null)"
    )
    fun findAllByKouluttajatKouluttajaUserId(
        userId: String
    ): List<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat ko join ko.kouluttaja k " +
            "where k.user.id = :userId and ks.lahetetty = true and ko.sopimusHyvaksytty = false"
    )
    fun findAllAvoinByKouluttajatKouluttajaUserId(
        userId: String
    ): List<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat kt join ks.opintooikeus o " +
            "where o.yliopisto.id = :yliopistoId and o.erikoisala.id = :erikoisalaId and (ks.vastuuhenkilonKorjausehdotus != null or not exists (select k from ks.kouluttajat k where k.sopimusHyvaksytty = false))"
    )
    fun findAllByVastuuhenkiloUserId(
        yliopistoId: Long,
        erikoisalaId: Long
    ): List<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat kt join ks.opintooikeus o " +
            "where o.yliopisto.id = :yliopistoId and o.erikoisala.id = :erikoisalaId and ks.vastuuhenkiloHyvaksynyt = false and not exists (select k from ks.kouluttajat k where k.sopimusHyvaksytty = false)"
    )
    fun findAllAvoinForVastuuhenkilo(
        yliopistoId: Long,
        erikoisalaId: Long
    ): List<KoejaksonKoulutussopimus>

    @Transactional
    @Modifying
    @Query("update KoulutussopimuksenKouluttaja k set k.kouluttaja.id = :newKayttaja where k.kouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoulutussopimuksenKouluttaja k set k.kouluttaja.id = :newKayttaja where k.kouluttaja.id = :currentKayttaja and k.id in (select k2.id from KoulutussopimuksenKouluttaja k2 where k2.koulutussopimus.vastuuhenkiloHyvaksynyt = false)")
    fun changeAvoinKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    fun existsByKouluttajatIdAndVastuuhenkiloHyvaksynytFalse(id: Long): Boolean
}
