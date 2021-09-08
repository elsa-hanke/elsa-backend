package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TyoskentelyjaksoRepository : JpaRepository<Tyoskentelyjakso, Long> {

    fun findAllByErikoistuvaLaakariKayttajaId(id: String): List<Tyoskentelyjakso>

    fun findOneByIdAndErikoistuvaLaakariKayttajaId(id: Long, kayttajaId: String): Tyoskentelyjakso?

    fun findOneByErikoistuvaLaakariKayttajaIdAndLiitettyKoejaksoonTrue(kayttajaId: String): Tyoskentelyjakso?
}
