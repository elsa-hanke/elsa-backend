package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import java.io.Serializable
import java.time.LocalDate

data class KoejaksonVaiheDTO(

    var id: Long? = null,

    var tyyppi: KoejaksoTyyppi? = null,

    var tila: KoejaksoTila? = null,

    var erikoistuvanNimi: String? = null,

    var erikoistuvanAvatar: ByteArray? = null,

    var pvm: LocalDate? = null,

    var hyvaksytytVaiheet: MutableList<HyvaksyttyKoejaksonVaiheDTO> = mutableListOf()

) : Serializable {
    override fun toString() = "KoejaksonVaiheDTO"
}
