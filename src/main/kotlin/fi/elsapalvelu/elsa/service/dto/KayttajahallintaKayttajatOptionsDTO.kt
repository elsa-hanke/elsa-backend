package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class KayttajahallintaKayttajatOptionsDTO(

    var erikoisalat: Set<ErikoisalaDTO>? = setOf()

): Serializable {
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

