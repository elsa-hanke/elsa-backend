package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.service.dto.KayttajahallintaYliopistoErikoisalaDTO
import jakarta.validation.constraints.NotNull
import java.io.Serializable
import java.time.LocalDate

data class KayttajahallintaErikoistujaJaKouluttajaListItemDTO(

    @get: NotNull
    var kayttajaId: Long? = null,

    @get: NotNull
    var etunimi: String? = null,

    @get: NotNull
    var sukunimi: String? = null,

    var syntymaaika: LocalDate? = null,

    @get: NotNull
    var yliopistotAndErikoisalat: List<KayttajahallintaYliopistoErikoisalaDTO> = listOf(),

    @get: NotNull
    var kayttajatilinTila: KayttajatilinTila? = null,

    @get: NotNull
    var authorities: List<String>? = listOf(),

    @get: NotNull
    var sahkoposti: String,

    ) : Serializable {
    override fun toString() = "KayttajahallintaKayttajaListItemDTO"
}

