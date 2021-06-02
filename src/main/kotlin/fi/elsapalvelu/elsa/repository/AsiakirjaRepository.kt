package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asiakirja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AsiakirjaRepository : JpaRepository<Asiakirja, Long> {

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<Asiakirja>

    fun findAllByErikoistuvaLaakariKayttajaUserIdAndTyoskentelyjaksoId(
        userId: String,
        tyoskentelyJaksoId: Long?
    ): List<Asiakirja>

    fun findOneByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Asiakirja
}
