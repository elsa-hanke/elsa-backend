package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import java.io.Serializable
import java.time.LocalDate
import java.util.*

data class TerveyskeskuskoulutusjaksonHyvaksyntaDTO(

    var id: Long? = null,

    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanNimi: String? = null,

    var erikoistuvanAvatar: ByteArray? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    var erikoistuvanSyntymaaika: LocalDate? = null,

    var erikoistuvanYliopisto: YliopistoEnum? = null,

    var terveyskeskuskoulutusjaksonKesto: Double? = null,

    var laillistamispaiva: LocalDate? = null,

    var laillistamispaivanLiite: ByteArray? = null,

    var laillistamispaivanLiitteenNimi: String? = null,

    var laillistamispaivanLiitteenTyyppi: String? = null,

    var asetus: String? = null,

    var tyoskentelyjaksot: List<TyoskentelyjaksoDTO>? = listOf(),

    var vastuuhenkilonNimi: String? = null,

    var vastuuhenkilonNimike: String? = null,

    var tila: TerveyskeskuskoulutusjaksoTila? = null,

    var korjausehdotus: String? = null,

    var lisatiedotVirkailijalta: String? = null,

    var virkailijanNimi: String? = null,

    var virkailijanNimike: String? = null,

    var virkailijanKuittausaika: LocalDate? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerveyskeskuskoulutusjaksonHyvaksyntaDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = 31
}
