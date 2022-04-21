package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class ErikoistujienSeurantaOptionsVirkailijaDTO(

    var erikoisalat: Set<ErikoisalaDTO>? = setOf(),

    var asetukset: Set<AsetusDTO>? = setOf()

): Serializable {
    override fun toString(): String {
        return "ErikoistujienSeurantaOptionsDTO()"
    }

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErikoistujienSeurantaOptionsVirkailijaDTO

        if (erikoisalat != other.erikoisalat) return false
        if (asetukset != other.asetukset) return false

        return true
    }
}
