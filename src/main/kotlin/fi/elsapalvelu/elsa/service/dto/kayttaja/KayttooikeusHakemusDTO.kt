package fi.elsapalvelu.elsa.service.dto.kayttaja

import jakarta.validation.constraints.NotNull

data class KayttooikeusHakemusDTO(

    @get: NotNull
    var yliopisto: Long? = null

)
