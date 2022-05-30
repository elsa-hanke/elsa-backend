package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.service.dto.YliopistoErikoisalaDTO
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KayttajahallintaKayttajaListItemDTO(

    @get: NotNull
    var kayttajaId: Long? = null,

    @get: NotNull
    var etunimi: String? = null,

    @get: NotNull
    var sukunimi: String? = null,

    var syntymaaika: LocalDate? = null,

    @get: NotNull
    var yliopistotAndErikoisalat: List<YliopistoErikoisalaDTO> = listOf(),

    @get: NotNull
    var kayttajatilinTila: KayttajatilinTila? = null

) : Serializable {
    override fun toString() = "KayttajahallintaKayttajaListItemDTO"
}

