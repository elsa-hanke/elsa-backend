package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueenKategoriaDTO
import java.util.*

interface EpaOsaamisalueenKategoriaService {

    fun save(epaOsaamisalueenKategoriaDTO: EpaOsaamisalueenKategoriaDTO): EpaOsaamisalueenKategoriaDTO

    fun findAll(): List<EpaOsaamisalueenKategoriaDTO>

    fun findOne(id: Long): Optional<EpaOsaamisalueenKategoriaDTO>

    fun delete(id: Long)
}
