package fi.elsapalvelu.elsa.service.kayttaja

import fi.elsapalvelu.elsa.service.dto.kayttaja.PalauteDTO

interface PalauteService {
    fun send(palauteDTO: PalauteDTO, userId: String)
}
