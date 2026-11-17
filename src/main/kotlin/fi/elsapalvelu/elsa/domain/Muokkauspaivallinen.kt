package fi.elsapalvelu.elsa.domain

import java.time.LocalDate

interface Muokkauspaivallinen {
    var muokkauspaiva: LocalDate?
}
