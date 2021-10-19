package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface TeoriakoulutusRepository : JpaRepository<Teoriakoulutus, Long> {

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<Teoriakoulutus>

    fun findOneByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Teoriakoulutus?

    fun deleteByIdAndErikoistuvaLaakariKayttajaUserId(id: Long, userId: String)
}
