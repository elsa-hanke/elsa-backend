package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ErikoistujienSeurantaDTO(

    var kayttajaYliopistoErikoisalat: List<KayttajaErikoisalatPerYliopistoDTO>? = listOf(),

    var erikoisalat: MutableSet<String>? = mutableSetOf(),

    var erikoistujienEteneminen: MutableList<ErikoistujanEteneminenDTO>? = mutableListOf()

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErikoistujienSeurantaDTO

        if (erikoisalat != other.erikoisalat) return false
        if (erikoistujienEteneminen != other.erikoistujienEteneminen) return false

        return true
    }


}
