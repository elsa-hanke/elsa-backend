package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob

data class ValmistumispyyntoOsaamisenArviointiDTO (

    var id: Long? = null,

    var tila: ValmistumispyynnonTila? = null,

    var muokkauspaiva: LocalDate? = null,

    var erikoistujanNimi: String? = null,

    @Lob
    var erikoistujanAvatar: ByteArray? = null,

    var erikoistujanOpiskelijatunnus: String? = null,

    var erikoistujanSyntymaaika: String? = null,

    var erikoistujanYliopisto: YliopistoEnum? = null,

    var erikoistujanErikoisala: String? = null,

    var erikoistujanLaillistamispaiva: LocalDate? = null,

    @Lob
    var erikoistujanLaillistamistodistus: ByteArray? = null,

    var erikoistujanLaillistamistodistusNimi: String? = null,

    var erikoistujanLaillistamistodistusTyyppi: String? = null,

    var erikoistujanAsetus: String? = null,

    var opintooikeusId: Long? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var vastuuhenkiloOsaamisenArvioijaNimi: String? = null,

    var vastuuhenkiloOsaamisenArvioijaNimike: String? = null,

    var vastuuhenkiloOsaamisenArvioijaKuittausaika: LocalDate? = null,

    var vastuuhenkiloOsaamisenArvioijaPalautusaika: LocalDate? = null,

    var vastuuhenkiloOsaamisenArvioijaKorjausehdotus: String? = null,

    var virkailijanKorjausehdotus: String? = null,

    var vastuuhenkiloHyvaksyjaKorjausehdotus: String? = null

) : Serializable {
    override fun toString() = "ValmistumispyyntoOsaamisenArviointiDTO"

    override fun hashCode(): Int {
        return erikoistujanLaillistamistodistus?.contentHashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValmistumispyyntoOsaamisenArviointiDTO

        if (erikoistujanAvatar != null) {
            if (other.erikoistujanAvatar == null) return false
            if (!erikoistujanAvatar.contentEquals(other.erikoistujanAvatar)) return false
        } else if (other.erikoistujanAvatar != null) return false
        if (erikoistujanLaillistamistodistus != null) {
            if (other.erikoistujanLaillistamistodistus == null) return false
            if (!erikoistujanLaillistamistodistus.contentEquals(other.erikoistujanLaillistamistodistus)) return false
        } else if (other.erikoistujanLaillistamistodistus != null) return false

        return true
    }
}

