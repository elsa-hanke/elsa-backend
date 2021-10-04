package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.PalauteDTO

interface PalauteService {
    fun send(palauteDTO: PalauteDTO, userId: String)
}
