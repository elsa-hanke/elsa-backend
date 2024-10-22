package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface SeurantajaksoRepository : JpaRepository<Seurantajakso, Long> {

    fun findByOpintooikeusId(opintooikeusId: Long): List<Seurantajakso>

    fun findByIdAndOpintooikeusId(id: Long, opintooikeusId: Long): Seurantajakso?

    fun findByKouluttajaUserId(userId: String): List<Seurantajakso>

    fun findByIdAndKouluttajaUserId(id: Long, userId: String): Seurantajakso?

    fun findAllByKouluttajaUserId(userId: String): List<Seurantajakso>

    @Query(
        """
        select sj from Seurantajakso sj
        join sj.kouluttaja k
        where sj.hyvaksytty = false and (sj.kouluttajanArvio is null or sj.seurantakeskustelunYhteisetMerkinnat is not null) and sj.korjausehdotus is null and k.user.id = :userId
        order by sj.tallennettu
        """
    )
    fun findAvoinByKouluttajaUserId(userId: String): List<Seurantajakso>

    fun existsByKouluttajaIdAndHyvaksyttyFalse(id: Long): Boolean

    @Transactional
    @Modifying
    @Query("update Seurantajakso s set s.kouluttaja.id = :newKayttaja where s.kouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update Seurantajakso s set s.kouluttaja.id = :newKayttaja where s.kouluttaja.id = :currentKayttaja and s.hyvaksytty = false")
    fun changeAvoinKouluttaja(currentKayttaja: Long, newKayttaja: Long)
}
