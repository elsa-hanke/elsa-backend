package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonVastuuhenkilonArvioRepository :
    JpaRepository<KoejaksonVastuuhenkilonArvio, Long> {

    fun findOneByIdAndVastuuhenkiloId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonVastuuhenkilonArvio>

    fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonVastuuhenkilonArvio>

    fun findAllByVastuuhenkiloId(
        kayttajaId: String
    ): List<KoejaksonVastuuhenkilonArvio>
}
