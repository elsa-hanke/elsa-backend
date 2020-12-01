package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class KuntaDTO(

    var id: String? = null,

    @get: NotNull
    var abbreviation: String? = null,

    var shortName: String? = null,

    var longName: String? = null,

    @Lob
    var description: String? = null,

    var kortnamn: String? = null,

    var korvaavaKoodi: String? = null,

    var langtNamn: String? = null,

    var maakunta: String? = null,

    var sairaanhoitopiiri: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KuntaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
