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

    fun findOneByIdAndKouluttajatKouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKoulutussopimus>

    fun findOneByIdAndVastuuhenkiloId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKoulutussopimus>

    fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat ko join ko.kouluttaja k " +
            "where k.id = :kayttajaId and (ks.lahetetty = true or ks.korjausehdotus != null)"
    )
    fun findAllByKouluttajatKouluttajaId(
        kayttajaId: String
    ): List<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat kt " +
            "where ks.vastuuhenkilo.id = :kayttajaId and (ks.korjausehdotus != null or not exists (select k from ks.kouluttajat k where k.sopimusHyvaksytty = false))"
    )
    fun findAllByVastuuhenkiloId(
        kayttajaId: String
    ): List<KoejaksonKoulutussopimus>

    @Transactional
    @Modifying
    @Query("update KoulutussopimuksenKouluttaja k set k.kouluttaja.id = :newKayttaja where k.kouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: String, newKayttaja: String)
}
