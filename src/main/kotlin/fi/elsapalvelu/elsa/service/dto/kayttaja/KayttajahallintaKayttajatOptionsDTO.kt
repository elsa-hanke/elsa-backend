package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
class KayttajahallintaKayttajatOptionsDTO(

    var erikoisalat: Set<ErikoisalaDTO>? = setOf()

): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString(): String {
        return "KayttajahallintaKayttajatOptionsDTO()"
    }

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KayttajahallintaKayttajatOptionsDTO

        if (erikoisalat != other.erikoisalat) return false

        return true
    }
}
