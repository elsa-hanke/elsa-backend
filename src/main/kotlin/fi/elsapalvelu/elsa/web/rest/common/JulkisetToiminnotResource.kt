package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import fi.elsapalvelu.elsa.service.ApplicationSettingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/julkinen/")
class JulkisetToiminnotResource(private val applicationSettingService: ApplicationSettingService) {

    @GetMapping("/seuraava-paivitys")
    fun getSeuraavaPaivitys(
        principal: Principal?
    ): ResponseEntity<Instant> {
        val paivitysAika = applicationSettingService.getDatetimeSettingValue(
            ApplicationSettingTyyppi.SEURAAVAN_PAIVITYKSEN_AIKA
        )
        val now = Instant.now()
        if (paivitysAika != null && paivitysAika.isAfter(now) && ChronoUnit.DAYS.between(
                now,
                paivitysAika
            ) < 3
        ) {
            return ResponseEntity.ok(
                paivitysAika
            )
        }
        return ResponseEntity.ok(
            null
        )
    }
}
