package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Asiakirja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AsiakirjaRepository : JpaRepository<Asiakirja, Long> {

    fun findAllByErikoistuvaLaakariKayttajaId(kayttajaId: String): List<Asiakirja>

    fun findAllByErikoistuvaLaakariKayttajaIdAndTyoskentelyjaksoId(
        kayttajaId: String,
        tyoskentelyJaksoId: Long?
    ): List<Asiakirja>

    fun findOneByIdAndErikoistuvaLaakariKayttajaId(id: Long, kayttajaId: String): Asiakirja
}
