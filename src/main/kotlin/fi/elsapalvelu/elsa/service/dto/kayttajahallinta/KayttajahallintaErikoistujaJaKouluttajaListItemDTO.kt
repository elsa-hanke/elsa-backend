package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajahallintaYliopistoErikoisalaDTO
import jakarta.validation.constraints.NotNull
import java.io.Serializable

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

    companion object {
        private const val serialVersionUID = 1L
    }
}

