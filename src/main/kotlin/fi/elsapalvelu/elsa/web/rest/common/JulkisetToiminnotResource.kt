package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import fi.elsapalvelu.elsa.service.ApplicationSettingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.Instant

@RestController
@RequestMapping("/api/julkinen/")
class JulkisetToiminnotResource(private val applicationSettingService: ApplicationSettingService) {

    @GetMapping("/seuraava-paivitys")
    fun getSeuraavaPaivitys(
        principal: Principal?
    ): ResponseEntity<Instant> {
        return ResponseEntity.ok(
            applicationSettingService.getDatetimeSettingValue(
                ApplicationSettingTyyppi.SEURAAVAN_PAIVITYKSEN_AIKA
            )
        )
    }
}
