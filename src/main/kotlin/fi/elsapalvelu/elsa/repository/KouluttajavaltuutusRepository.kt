package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Repository
interface KouluttajavaltuutusRepository : JpaRepository<Kouluttajavaltuutus, Long> {

    fun findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaAfter(
        valtuuttajaOpintooikeusId: Long,
        pvm: LocalDate
    ): List<Kouluttajavaltuutus>

    fun findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
        valtuuttajaOpintooikeusId: Long,
        before: LocalDate,
        after: LocalDate,
    ): List<Kouluttajavaltuutus>

    fun findAllByValtuutettuUserIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
        valtuutettuUserId: String,
        before: LocalDate,
        after: LocalDate,
    ): List<Kouluttajavaltuutus>

    fun findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(
        valtuuttajaOpintooikeusId: Long,
        valtuutettuId: String,
        pvm: LocalDate
    ): Optional<Kouluttajavaltuutus>

    fun findByValtuuttajaOpintooikeusIdAndValtuutettuUserId(
        valtuuttajaOpintooikeusId: Long,
        valtuutettuId: String
    ): Optional<Kouluttajavaltuutus>

    @Transactional
    @Modifying
    @Query("update Kouluttajavaltuutus k set k.valtuutettu.id = :newKayttajaId where k.valtuutettu.id = :currentKayttajaId")
    fun changeKouluttaja(currentKayttajaId: Long, newKayttajaId: Long)
}
