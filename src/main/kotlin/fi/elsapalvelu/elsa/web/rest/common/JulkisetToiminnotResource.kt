package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import fi.elsapalvelu.elsa.service.ApplicationSettingService
import fi.elsapalvelu.elsa.service.IlmoitusService
import fi.elsapalvelu.elsa.service.dto.IlmoitusDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/julkinen/")
class JulkisetToiminnotResource(
    private val applicationSettingService: ApplicationSettingService,
    private val ilmoitusService: IlmoitusService
) {

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

    @GetMapping("/ilmoitukset")
    fun getIlmoitukset(
        principal: Principal?
    ): ResponseEntity<List<IlmoitusDTO>> {
        return ResponseEntity.ok(
            ilmoitusService.findAll()
        )
    }

    @GetMapping("/ilmoitukset/{id}")
    fun getIlmoitus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<IlmoitusDTO> {
        return ilmoitusService.findOne(id)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
