package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKategoriaService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import tech.jhipster.web.util.ResponseUtil
import java.security.Principal

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaArviointityokalutResource(
    private val arviointityokaluService: ArviointityokaluService,
    private val arviointityokaluKategoriaService: ArviointityokaluKategoriaService,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/arviointityokalut/kategoriat")
    fun getArviointityokaluKategoriat(): ResponseEntity<List<ArviointityokaluKategoriaDTO>> {
        return ResponseEntity.ok(arviointityokaluKategoriaService.findAll())
    }

    @GetMapping("/arviointityokalut/kategoria/{id}")
    fun getArviointityokaluKategoria(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ArviointityokaluKategoriaDTO> {
        val arviointityokaluKategoriaDTO = arviointityokaluKategoriaService.findOne(id)
        return ResponseUtil.wrapOrNotFound(arviointityokaluKategoriaDTO)
    }

    @PostMapping("/arviointityokalut/kategoria")
    fun createArviointityokaluKategoria(
        @Valid @RequestBody arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO
    ): ResponseEntity<ArviointityokaluKategoriaDTO> {
        return ResponseEntity.ok(arviointityokaluKategoriaService.create(arviointityokaluKategoriaDTO))
    }

    @PatchMapping("/arviointityokalut/kategoria")
    fun updateArviointityokaluKategoria(
        @Valid @RequestBody arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO
    ): ResponseEntity<ArviointityokaluKategoriaDTO> {
        return ResponseEntity.ok(arviointityokaluKategoriaService.update(arviointityokaluKategoriaDTO))
    }

    @DeleteMapping("/arviointityokalut/kategoria/{id}")
    fun deleteArviointityokaluKategoria(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        arviointityokaluKategoriaService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/arviointityokalu")
    fun createArviointityokalu(
        @RequestPart("data") dataJson: String,
        @RequestPart(value = "liiteData", required = false) liiteData: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ArviointityokaluDTO> {
        val arviointityokaluDTO = objectMapper.readValue(dataJson, ArviointityokaluDTO::class.java)
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(arviointityokaluService.save(arviointityokaluDTO, user, liiteData))
    }

    @PatchMapping("/arviointityokalu")
    fun updateArviointityokalu(
        @RequestPart("data") dataJson: String,
        @RequestPart(value = "liiteData", required = false) liiteData: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ArviointityokaluDTO> {
        val arviointityokaluDTO = objectMapper.readValue(dataJson, ArviointityokaluDTO::class.java)
        return ResponseEntity.ok(arviointityokaluService.update(arviointityokaluDTO, liiteData))
    }

    @GetMapping("/arviointityokalut")
    fun getArviointityokalut(): ResponseEntity<List<ArviointityokaluDTO>> {
        return ResponseEntity.ok(arviointityokaluService.findAll())
    }

    @GetMapping("/arviointityokalu/{id}")
    fun getArviointityokalu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ArviointityokaluDTO> {
        val arviointityokaluDTO = arviointityokaluService.findOne(id)
        return ResponseUtil.wrapOrNotFound(arviointityokaluDTO)
    }

    @DeleteMapping("/arviointityokalu/{id}")
    fun deleteArviointityokalu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        arviointityokaluService.delete(id)
        return ResponseEntity.noContent().build()
    }

}
