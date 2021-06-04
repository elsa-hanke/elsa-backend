package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class HyvaksytytKoejaksoVaiheetDTO(

    var aloituskeskusteluId: Long? = null,

    var aloituskeskusteluPvm: LocalDate? = null,

    var valiarviointiId: Long? = null,

    var valiarviointiPvm: LocalDate? = null,

    var kehittamistoimenpiteetId: Long? = null,

    var kehittamistoimenpiteetPvm: LocalDate? = null,

    var loppukeskusteluId: Long? = null,

    var loppukeskusteluPvm: LocalDate? = null

) : Serializable
