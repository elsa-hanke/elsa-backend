package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueenKategoriaDTO
import java.util.Optional

interface EpaOsaamisalueenKategoriaService {

    fun save(epaOsaamisalueenKategoriaDTO: EpaOsaamisalueenKategoriaDTO): EpaOsaamisalueenKategoriaDTO

    fun findAll(): MutableList<EpaOsaamisalueenKategoriaDTO>

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<EpaOsaamisalueenKategoriaDTO>

    fun findOne(id: Long): Optional<EpaOsaamisalueenKategoriaDTO>

    fun delete(id: Long)
}
