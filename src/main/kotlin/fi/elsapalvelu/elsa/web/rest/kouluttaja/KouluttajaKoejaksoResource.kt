package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
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
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejaksot")
    fun getKoejaksot(principal: Principal?): ResponseEntity<List<KoejaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)

        val resultMap = HashMap<KayttajaDTO, KoejaksoDTO>()

        val koulutussopimukset =
            koejaksonKoulutussopimusService.findAllByKouluttajaKayttajaUserId(user.id!!)

        koulutussopimukset.forEach { (kayttaja, sopimus) ->
            resultMap[kayttaja] = KoejaksoDTO()
            resultMap[kayttaja]?.addKoulutussopimus(sopimus)
        }

        val aloituskeskustelut =
            koejaksonAloituskeskusteluService.findAllByKouluttajaUserId(user.id!!)
        aloituskeskustelut.forEach { (kayttaja, aloituskeskustelu) ->
            resultMap.putIfAbsent(kayttaja, KoejaksoDTO())
            resultMap[kayttaja]?.addAloitusKeskustelu(aloituskeskustelu)
        }

        val valiarvioinnit =
            koejaksonValiarviointiService.findAllByKouluttajaUserId(user.id!!)
        valiarvioinnit.forEach { (kayttaja, valiarviointi) ->
            resultMap.putIfAbsent(kayttaja, KoejaksoDTO())
            resultMap[kayttaja]?.addValiarviointi(valiarviointi)
            addAloitusKeskustelu(resultMap[kayttaja]!!, valiarviointi.id!!)
        }

        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findAllByKouluttajaUserId(user.id!!)
        kehittamistoimenpiteet.forEach { (kayttaja, toimenpiteet) ->
            resultMap.putIfAbsent(kayttaja, KoejaksoDTO())
            resultMap[kayttaja]?.addKehittamistoimenpiteet(toimenpiteet)
            addValiarviointi(resultMap[kayttaja]!!, toimenpiteet.id!!)
            addAloitusKeskustelu(resultMap[kayttaja]!!, resultMap[kayttaja]!!.valiarviointi?.id!!)
        }

        val loppukeskustelut =
            koejaksonLoppukeskusteluService.findAllByKouluttajaUserId(user.id!!)
        loppukeskustelut.forEach { (kayttaja, loppukeskustelu) ->
            resultMap.putIfAbsent(kayttaja, KoejaksoDTO())
            resultMap[kayttaja]?.addLoppukeskustelu(loppukeskustelu)
            addValiarviointi(resultMap[kayttaja]!!, loppukeskustelu.id!!)
            addKehittamistoimenpiteet(resultMap[kayttaja]!!, loppukeskustelu.id!!)
            addAloitusKeskustelu(resultMap[kayttaja]!!, resultMap[kayttaja]!!.valiarviointi?.id!!)
        }
        return ResponseEntity.ok(resultMap.values.toList())
    }

    @GetMapping("/koejakso/koulutussopimus/{id}")
    fun getKoulutussopimus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get koulutussopimus $id for user: $user.id")
        val koulutussopimusDTO =
            koejaksonKoulutussopimusService.findOneByIdAndKouluttajaKayttajaUserId(id, user.id!!)
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

        val user = userService.getAuthenticatedUser(principal)

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
            koejaksonKoulutussopimusService.update(koulutussopimusDTO, user.id!!)
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
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get aloituskeskustelu $id for user: $user.id")
        var aloituskeskusteluDTO =
            koejaksonAloituskeskusteluService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!aloituskeskusteluDTO.isPresent) {
            aloituskeskusteluDTO =
                koejaksonAloituskeskusteluService.findOneByIdAndLahiesimiesUserId(id, user.id!!)
        }
        return ResponseUtil.wrapOrNotFound(aloituskeskusteluDTO)
    }

    @PutMapping("/koejakso/aloituskeskustelu")
    fun updateAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        validateId(aloituskeskusteluDTO.id, ENTITY_KOEJAKSON_ALOITUSKESKUSTELU)
        val user = userService.getAuthenticatedUser(principal)

        var aloituskeskustelu =
            koejaksonAloituskeskusteluService.findOneByIdAndLahikouluttajaUserId(
                aloituskeskusteluDTO.id!!,
                user.id!!
            )

        if (!aloituskeskustelu.isPresent) {
            aloituskeskustelu =
                koejaksonAloituskeskusteluService.findOneByIdAndLahiesimiesUserId(
                    aloituskeskusteluDTO.id!!,
                    user.id!!
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
            koejaksonAloituskeskusteluService.update(aloituskeskusteluDTO, user.id!!)
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
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get valiarviointi $id for user: $user.id")
        var valiarviointiDTO =
            koejaksonValiarviointiService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!valiarviointiDTO.isPresent) {
            valiarviointiDTO =
                koejaksonValiarviointiService.findOneByIdAndLahiesimiesUserId(id, user.id!!)
        }
        return ResponseUtil.wrapOrNotFound(valiarviointiDTO)
    }

    @PutMapping("/koejakso/valiarviointi")
    fun updateValiarviointi(
        @Valid @RequestBody valiarviointiDTO: KoejaksonValiarviointiDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        validateId(valiarviointiDTO.id, ENTITY_KOEJAKSON_LOPPUKESKUSTELU)
        val user = userService.getAuthenticatedUser(principal)

        var valiarviointi =
            koejaksonValiarviointiService.findOneByIdAndLahikouluttajaUserId(
                valiarviointiDTO.id!!,
                user.id!!
            )

        if (!valiarviointi.isPresent) {
            valiarviointi =
                koejaksonValiarviointiService.findOneByIdAndLahiesimiesUserId(
                    valiarviointiDTO.id!!,
                    user.id!!
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
            koejaksonValiarviointiService.update(valiarviointiDTO, user.id!!)
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
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get kehittamistoimenpiteet $id for user: $user.id")
        var kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!kehittamistoimenpiteetDTO.isPresent) {
            kehittamistoimenpiteetDTO =
                koejaksonKehittamistoimenpiteetService.findOneByIdAndLahiesimiesUserId(
                    id,
                    user.id!!
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
        val user = userService.getAuthenticatedUser(principal)

        var kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findOneByIdAndLahikouluttajaUserId(
                kehittamistoimenpiteetDTO.id!!,
                user.id!!
            )

        if (!kehittamistoimenpiteet.isPresent) {
            kehittamistoimenpiteet =
                koejaksonKehittamistoimenpiteetService.findOneByIdAndLahiesimiesUserId(
                    kehittamistoimenpiteetDTO.id!!,
                    user.id!!
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
            koejaksonKehittamistoimenpiteetService.update(kehittamistoimenpiteetDTO, user.id!!)
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
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get loppukeskustelu $id for user: $user.id")
        var loppukeskusteluDTO =
            koejaksonLoppukeskusteluService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!loppukeskusteluDTO.isPresent) {
            loppukeskusteluDTO =
                koejaksonLoppukeskusteluService.findOneByIdAndLahiesimiesUserId(
                    id,
                    user.id!!
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
        val user = userService.getAuthenticatedUser(principal)

        var loppukeskustelu =
            koejaksonLoppukeskusteluService.findOneByIdAndLahikouluttajaUserId(
                loppukeskusteluDTO.id!!,
                user.id!!
            )

        if (!loppukeskustelu.isPresent) {
            loppukeskustelu =
                koejaksonLoppukeskusteluService.findOneByIdAndLahiesimiesUserId(
                    loppukeskusteluDTO.id!!,
                    user.id!!
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
            koejaksonLoppukeskusteluService.update(loppukeskusteluDTO, user.id!!)
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

    private fun addAloitusKeskustelu(
        koejaksoDTO: KoejaksoDTO,
        valiarviointiId: Long
    ) {
        if (koejaksoDTO.aloituskeskustelu == null) {
            koejaksoDTO.addAloitusKeskustelu(
                koejaksonAloituskeskusteluService.findByValiarviointiId(valiarviointiId)
                    .get()
            )
        }
    }

    private fun addValiarviointi(
        koejaksoDTO: KoejaksoDTO,
        kehittamistoimenpiteetId: Long
    ) {
        if (koejaksoDTO.valiarviointi == null) {
            koejaksoDTO.addValiarviointi(
                koejaksonValiarviointiService.findByKehittamistoimenpiteetId(
                    kehittamistoimenpiteetId
                )
                    .get()
            )
        }
    }

    private fun addKehittamistoimenpiteet(
        koejaksoDTO: KoejaksoDTO,
        loppukeskusteluId: Long
    ) {
        if (koejaksoDTO.kehittamistoimenpiteet == null && koejaksoDTO.valiarviointi?.kehittamistoimenpiteet != null) {
            koejaksoDTO.addKehittamistoimenpiteet(
                koejaksonKehittamistoimenpiteetService.findByLoppukeskusteluId(
                    loppukeskusteluId
                ).get()
            )
        }
    }
}