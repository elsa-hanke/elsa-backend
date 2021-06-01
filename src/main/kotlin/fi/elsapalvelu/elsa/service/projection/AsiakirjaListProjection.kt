package fi.elsapalvelu.elsa.service.projection

import java.time.LocalDateTime

interface AsiakirjaListProjection {
    val id: Long
    val nimi: String
    val lisattypvm: LocalDateTime
}
