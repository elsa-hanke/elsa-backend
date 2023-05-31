package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKehittamistoimenpiteet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface KoejaksonKehittamistoimenpiteetRepository :
    JpaRepository<KoejaksonKehittamistoimenpiteet, Long> {

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    @Query(
        "select kt from KoejaksonKehittamistoimenpiteet kt " +
            "where kt.id = :id and (kt.lahikouluttaja.user.id = :vastuuhenkiloUserId or kt.lahiesimies.user.id = :vastuuhenkiloUserId or kt.lahikouluttajaHyvaksynyt = true and kt.lahiesimiesHyvaksynyt = true " +
            "and kt.opintooikeus.id in (select va.opintooikeus.id from KoejaksonVastuuhenkilonArvio va " +
            "where va.vastuuhenkilo.user.id = :vastuuhenkiloUserId))"
    )
    fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonKehittamistoimenpiteet>

    @Query(
        "select k from KoejaksonKehittamistoimenpiteet k left join k.lahikouluttaja lk left join k.lahiesimies le " +
            "where lk.user.id = :userId or (le.user.id = :userId and (k.lahikouluttajaHyvaksynyt = true or (k.korjausehdotus != null and k.korjausehdotus != '')))"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonKehittamistoimenpiteet>

    @Query(
        "select k from KoejaksonKehittamistoimenpiteet k left join k.lahikouluttaja lk left join k.lahiesimies le " +
            "where (lk.user.id = :userId and k.lahikouluttajaHyvaksynyt = false) or (le.user.id = :userId and k.lahikouluttajaHyvaksynyt = true and k.lahiesimiesHyvaksynyt = false)"
    )
    fun findAllAvoinByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonKehittamistoimenpiteet>

    @Transactional
    @Modifying
    @Query("update KoejaksonKehittamistoimenpiteet k set k.lahikouluttaja.id = :newKayttaja where k.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonKehittamistoimenpiteet k set k.lahiesimies.id = :newKayttaja where k.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: Long, newKayttaja: Long)

    fun existsByLahikouluttajaIdOrLahiesimiesId(kouluttajaId: Long, lahiesimiesId: Long): Boolean
}
