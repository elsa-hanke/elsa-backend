package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class TerveyskeskuskoulutusjaksoDTO(

    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanNimi: String? = null,

    var erikoistuvanAvatar: ByteArray? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    var erikoistuvanSyntymaaika: LocalDate? = null,

    var terveyskoulutusjaksonKesto: Double? = null,

    var laillistamispaiva: String? = null,

    var asetus: String? = null,

    var tyoskentelyjaksot: List<TyoskentelyjaksoDTO>? = listOf(),

    var vastuuhenkilonNimi: String? = null,

    var vastuuhenkilonNimike: String? = null,

    ) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TerveyskeskuskoulutusjaksoDTO

        if (erikoistuvanErikoisala != other.erikoistuvanErikoisala) return false
        if (erikoistuvanNimi != other.erikoistuvanNimi) return false
        if (erikoistuvanAvatar != other.erikoistuvanAvatar) return false
        if (erikoistuvanOpiskelijatunnus != other.erikoistuvanOpiskelijatunnus) return false
        if (erikoistuvanSyntymaaika != other.erikoistuvanSyntymaaika) return false
        if (terveyskoulutusjaksonKesto != other.terveyskoulutusjaksonKesto) return false
        if (laillistamispaiva != other.laillistamispaiva) return false
        if (asetus != other.asetus) return false
        if (tyoskentelyjaksot != other.tyoskentelyjaksot) return false
        if (vastuuhenkilonNimi != other.vastuuhenkilonNimi) return false
        if (vastuuhenkilonNimike != other.vastuuhenkilonNimike) return false

        return true
    }
}
