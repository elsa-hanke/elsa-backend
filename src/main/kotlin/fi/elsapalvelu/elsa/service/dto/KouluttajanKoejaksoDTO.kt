package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import java.io.Serializable
import java.time.LocalDate

data class KouluttajanKoejaksoDTO(

    var id: Long? = null,

    var tyyppi: KoejaksoTyyppi? = null,

    var tila: KoejaksoTila? = null,

    var erikoistuvanNimi: String? = null,

    var pvm: LocalDate? = null,

    var aiemmat: HyvaksytytKoejaksoVaiheetDTO = HyvaksytytKoejaksoVaiheetDTO()

) : Serializable
