package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_KOEJAKSON_SOPIMUS = "koejakson_koulutussopimus"
private const val ENTITY_KOEJAKSON_ALOITUSKESKUSTELU = "koejakson_aloituskeskustelu"
private const val ENTITY_KOEJAKSON_VALIARVIOINTI = "koejakson_valiarviointi"
private const val ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET = "koejakson_kehittamistoimenpiteet"
private const val ENTITY_KOEJAKSON_LOPPUKESKUSTELU = "koejakson_loppukeskustelu"

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaKoejaksoResource(
    private val kayttajaService: KayttajaService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejaksot")
    fun getKoejaksot(principal: Principal?): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val koejaksonVaiheet = koejaksonVaiheetService.findAllByKouluttajaKayttajaId(kayttaja.id!!)

        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/koulutussopimus/{id}")
    fun getKoulutussopimus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val koulutussopimusDTO =
            koejaksonKoulutussopimusService.findOneByIdAndKouluttajaKayttajaId(id, kayttaja.id!!)

        return ResponseUtil.wrapOrNotFound(koulutussopimusDTO)
    }

    @PutMapping("/koejakso/koulutussopimus")
    fun updateKoulutussopimus(
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        validateId(koulutussopimusDTO.id, ENTITY_KOEJAKSON_SOPIMUS)

        if (koulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true
            || koulutussopimusDTO.vastuuhenkilo?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Koulutussopimus ei saa sisältää vastuuhenkilön kuittausta. " +
                    "Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val existingKoulutussopimusDTO =
            koejaksonKoulutussopimusService.findOne(koulutussopimusDTO.id!!)

        if (existingKoulutussopimusDTO.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Koulutussopimusta ei saa muokata, jos erikoistuva ei ole allekirjoittanut sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        val result =
            koejaksonKoulutussopimusService.update(koulutussopimusDTO, kayttaja.id!!)

        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_SOPIMUS,
                    koulutussopimusDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/koejakso/aloituskeskustelu/{id}")
    fun getAloituskeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var aloituskeskusteluDTO =
            koejaksonAloituskeskusteluService.findOneByIdAndLahikouluttajaId(id, kayttaja.id!!)

        if (!aloituskeskusteluDTO.isPresent) {
            aloituskeskusteluDTO =
                koejaksonAloituskeskusteluService.findOneByIdAndLahiesimiesId(id, kayttaja.id!!)
        }

        return ResponseUtil.wrapOrNotFound(aloituskeskusteluDTO)
    }

    @PutMapping("/koejakso/aloituskeskustelu")
    fun updateAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        validateId(aloituskeskusteluDTO.id, ENTITY_KOEJAKSON_ALOITUSKESKUSTELU)
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var aloituskeskustelu =
            koejaksonAloituskeskusteluService.findOneByIdAndLahikouluttajaId(
                aloituskeskusteluDTO.id!!,
                kayttaja.id!!
            )

        if (!aloituskeskustelu.isPresent) {
            aloituskeskustelu =
                koejaksonAloituskeskusteluService.findOneByIdAndLahiesimiesId(
                    aloituskeskusteluDTO.id!!,
                    kayttaja.id!!
                )

            if (!aloituskeskustelu.isPresent) {
                throw BadRequestAlertException(
                    "Koejakson aloituskeskustelua ei löydy.",
                    ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                    "dataillegal"
                )
            }

            if (aloituskeskustelu.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata aloituskeskustelua, " +
                        "jos kouluttaja ei ole allekirjoittanut sitä",
                    ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                    "dataillegal"
                )
            }
        }

        if (aloituskeskustelu.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Arviointia ei saa muokata, jos erikoistuva ei ole lähettänyt pyyntöä.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }

        validateArviointi(
            aloituskeskustelu.get().lahiesimies?.sopimusHyvaksytty,
            ENTITY_KOEJAKSON_ALOITUSKESKUSTELU
        )

        val result =
            koejaksonAloituskeskusteluService.update(aloituskeskusteluDTO, kayttaja.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                    aloituskeskusteluDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/koejakso/valiarviointi/{id}")
    fun getValiarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var valiarviointiDTO =
            koejaksonValiarviointiService.findOneByIdAndLahikouluttajaId(id, kayttaja.id!!)

        if (!valiarviointiDTO.isPresent) {
            valiarviointiDTO =
                koejaksonValiarviointiService.findOneByIdAndLahiesimiesId(id, kayttaja.id!!)
        }

        return ResponseUtil.wrapOrNotFound(valiarviointiDTO)
    }

    @PutMapping("/koejakso/valiarviointi")
    fun updateValiarviointi(
        @Valid @RequestBody valiarviointiDTO: KoejaksonValiarviointiDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        validateId(valiarviointiDTO.id, ENTITY_KOEJAKSON_LOPPUKESKUSTELU)
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var valiarviointi =
            koejaksonValiarviointiService.findOneByIdAndLahikouluttajaId(
                valiarviointiDTO.id!!,
                kayttaja.id!!
            )

        if (!valiarviointi.isPresent) {
            valiarviointi =
                koejaksonValiarviointiService.findOneByIdAndLahiesimiesId(
                    valiarviointiDTO.id!!,
                    kayttaja.id!!
                )

            if (!valiarviointi.isPresent) {
                throw BadRequestAlertException(
                    "Koejakson väliarviointia ei löydy.",
                    ENTITY_KOEJAKSON_VALIARVIOINTI,
                    "dataillegal"
                )
            }

            if (valiarviointi.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata väliarviointia, " +
                        "jos kouluttaja ei ole allekirjoittanut sitä",
                    ENTITY_KOEJAKSON_VALIARVIOINTI,
                    "dataillegal"
                )
            }
        }

        validateArviointi(
            valiarviointi.get().erikoistuvaAllekirjoittanut,
            ENTITY_KOEJAKSON_VALIARVIOINTI
        )

        val result =
            koejaksonValiarviointiService.update(valiarviointiDTO, kayttaja.id!!)

        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_VALIARVIOINTI,
                    valiarviointiDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/koejakso/kehittamistoimenpiteet/{id}")
    fun getKehittamistoimenpiteet(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetService.findOneByIdAndLahikouluttajaId(id, kayttaja.id!!)

        if (!kehittamistoimenpiteetDTO.isPresent) {
            kehittamistoimenpiteetDTO =
                koejaksonKehittamistoimenpiteetService.findOneByIdAndLahiesimiesId(
                    id,
                    kayttaja.id!!
                )
        }

        return ResponseUtil.wrapOrNotFound(kehittamistoimenpiteetDTO)
    }

    @PutMapping("/koejakso/kehittamistoimenpiteet")
    fun updateKehittamistoimenpiteet(
        @Valid @RequestBody kehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        validateId(kehittamistoimenpiteetDTO.id, ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET)
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findOneByIdAndLahikouluttajaId(
                kehittamistoimenpiteetDTO.id!!,
                kayttaja.id!!
            )

        if (!kehittamistoimenpiteet.isPresent) {
            kehittamistoimenpiteet =
                koejaksonKehittamistoimenpiteetService.findOneByIdAndLahiesimiesId(
                    kehittamistoimenpiteetDTO.id!!,
                    kayttaja.id!!
                )

            if (!kehittamistoimenpiteet.isPresent) {
                throw BadRequestAlertException(
                    "Koejakson kehittämistoimenpiteitä ei löydy.",
                    ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                    "dataillegal"
                )
            }

            if (kehittamistoimenpiteet.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata kehittämistoimenpiteitä, " +
                        "jos kouluttaja ei ole allekirjoittanut niitä",
                    ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                    "dataillegal"
                )
            }
        }

        validateArviointi(
            kehittamistoimenpiteet.get().erikoistuvaAllekirjoittanut,
            ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET
        )

        val result =
            koejaksonKehittamistoimenpiteetService.update(kehittamistoimenpiteetDTO, kayttaja.id!!)

        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                    kehittamistoimenpiteetDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/koejakso/loppukeskustelu/{id}")
    fun getLoppukeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var loppukeskusteluDTO =
            koejaksonLoppukeskusteluService.findOneByIdAndLahikouluttajaId(id, kayttaja.id!!)

        if (!loppukeskusteluDTO.isPresent) {
            loppukeskusteluDTO =
                koejaksonLoppukeskusteluService.findOneByIdAndLahiesimiesId(
                    id,
                    kayttaja.id!!
                )
        }

        return ResponseUtil.wrapOrNotFound(loppukeskusteluDTO)
    }

    @PutMapping("/koejakso/loppukeskustelu")
    fun updateLoppukeskustelu(
        @Valid @RequestBody loppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        validateId(loppukeskusteluDTO.id, ENTITY_KOEJAKSON_LOPPUKESKUSTELU)
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        var loppukeskustelu =
            koejaksonLoppukeskusteluService.findOneByIdAndLahikouluttajaId(
                loppukeskusteluDTO.id!!,
                kayttaja.id!!
            )

        if (!loppukeskustelu.isPresent) {
            loppukeskustelu =
                koejaksonLoppukeskusteluService.findOneByIdAndLahiesimiesId(
                    loppukeskusteluDTO.id!!,
                    kayttaja.id!!
                )

            if (!loppukeskustelu.isPresent) {
                throw BadRequestAlertException(
                    "Koejakson loppukeskustelua ei löydy.",
                    ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                    "dataillegal"
                )
            }

            if (loppukeskustelu.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata loppukeskustelua, " +
                        "jos kouluttaja ei ole allekirjoittanut niitä",
                    ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                    "dataillegal"
                )
            }
        }

        validateArviointi(
            loppukeskustelu.get().erikoistuvaAllekirjoittanut,
            ENTITY_KOEJAKSON_LOPPUKESKUSTELU
        )

        val result =
            koejaksonLoppukeskusteluService.update(loppukeskusteluDTO, kayttaja.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                    loppukeskusteluDTO.id.toString()
                )
            )
            .body(result)
    }

    private fun validateArviointi(
        hyvaksytty: Boolean?,
        entity: String
    ) {
        if (hyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä arviointia ei saa muokata",
                entity,
                "dataillegal"
            )
        }
    }

    private fun validateId(id: Long?, entity: String) {
        if (id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                entity,
                "idnull"
            )
        }
    }
}
