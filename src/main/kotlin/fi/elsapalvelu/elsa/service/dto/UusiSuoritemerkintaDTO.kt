package fi.elsapalvelu.elsa.service.dto

import jakarta.validation.constraints.NotNull
import java.io.Serializable
import java.time.LocalDate

data class UusiSuoritemerkintaDTO(

    @get: NotNull
    var suorituspaiva: LocalDate? = null,

    var lisatiedot: String? = null,

    var tyoskentelyjaksoId: Long? = null,

    var suoritteet: List<SuoritemerkinnanSuoriteDTO>? = listOf(),

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable
