package fi.elsapalvelu.elsa.domain

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.Clock
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class MuokkauspaivanKuuntelija(private val clock: Clock) {
    @PrePersist
    @PreUpdate
    fun paivita(entiteetti: Any) {
        (entiteetti as Muokkauspaivallinen).muokkauspaiva = LocalDate.now(clock)
    }
}
