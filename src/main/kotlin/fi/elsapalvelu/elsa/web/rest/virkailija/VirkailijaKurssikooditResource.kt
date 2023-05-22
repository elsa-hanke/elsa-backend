package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import tech.jhipster.web.util.ResponseUtil
import java.net.URI
import java.security.Principal

private const val KURSSIKOODI_ENTITY_NAME = "opintosuoritus_kurssikoodi"

@RestController
@RequestMapping("/api/virkailija/kurssikoodit")
class VirkailijaKurssikooditResource(
    private val userService: UserService,
    private val opintosuoritusKurssikooditService: OpintosuoritusKurssikooditService,
    private val opintosuoritusTyyppiService: OpintosuoritusTyyppiService
) {
    @GetMapping("")
    fun getKurssikoodit(principal: Principal?): ResponseEntity<List<OpintosuoritusKurssikoodiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(opintosuoritusKurssikooditService.findAllForVirkailija(user.id!!))
    }

    @GetMapping("/tyypit")
    fun getOpintosuoritusTyypit(): ResponseEntity<List<OpintosuoritusTyyppiDTO>> {
        return ResponseEntity.ok(opintosuoritusTyyppiService.findAll())
    }

    @GetMapping("/{id}")
    fun getKurssikoodi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<OpintosuoritusKurssikoodiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseUtil.wrapOrNotFound(opintosuoritusKurssikooditService.findOne(id, user.id!!))
    }

    @PostMapping("")
    fun createKurssikoodi(
        principal: Principal?,
        @RequestBody opintosuoritusKurssikoodiDTO: OpintosuoritusKurssikoodiDTO
    ): ResponseEntity<OpintosuoritusKurssikoodiDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (opintosuoritusKurssikoodiDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi kurssikoodi ei saa sisältää id:tä",
                KURSSIKOODI_ENTITY_NAME,
                "idexists"
            )
        }

        opintosuoritusKurssikooditService.save(user.id!!, opintosuoritusKurssikoodiDTO)?.let {
            return ResponseEntity
                .created(URI("/api/virkailija/kurssikoodi/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("")
    fun updateKurssikoodi(
        principal: Principal?,
        @RequestBody opintosuoritusKurssikoodiDTO: OpintosuoritusKurssikoodiDTO
    ): ResponseEntity<OpintosuoritusKurssikoodiDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (opintosuoritusKurssikoodiDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                KURSSIKOODI_ENTITY_NAME,
                "idexists"
            )
        }

        return ResponseEntity.ok(
            opintosuoritusKurssikooditService.save(
                user.id!!,
                opintosuoritusKurssikoodiDTO
            )
        )
    }

    @DeleteMapping("/{id}")
    fun deleteKurssikoodi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        opintosuoritusKurssikooditService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }
}
