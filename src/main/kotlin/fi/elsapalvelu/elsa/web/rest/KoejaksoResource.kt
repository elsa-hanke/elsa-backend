package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.kouluttaja.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

private const val ENTITY_KOEJAKSON_ALOITUSKESKUSTELU = "koejakson_aloituskeskustelu"
private const val ENTITY_KOEJAKSON_VALIARVIOINTI = "koejakson_valiarviointi"
private const val ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET = "koejakson_kehittamistoimenpiteet"
private const val ENTITY_KOEJAKSON_LOPPUKESKUSTELU = "koejakson_loppukeskustelu"

open class KoejaksoResource(
    private val userService: UserService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService
) {

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
                    "dataillegal.koejakson-aloituskeskustelua-ei-loydy"
                )
            }

            if (aloituskeskustelu.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esihenkilö ei saa muokata aloituskeskustelua, " +
                        "jos kouluttaja ei ole hyväksynyt sitä",
                    ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                    "dataillegal.esimies-ei-saa-muokata-aloituskeskustelua-jos-kouluttaja-ei-ole-hyvaksynyt-sita"
                )
            }
        }

        if (aloituskeskustelu.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Arviointia ei saa muokata, jos erikoistuva ei ole lähettänyt pyyntöä.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal.arviointia-ei-saa-muokata-jos-erikoistuva-ei-ole-lahettanyt-pyyntoa"
            )
        }

        validateArviointi(
            aloituskeskustelu.get().lahiesimies?.sopimusHyvaksytty,
            ENTITY_KOEJAKSON_ALOITUSKESKUSTELU
        )

        val result = koejaksonAloituskeskusteluService.update(aloituskeskusteluDTO, user.id!!)
        return ResponseEntity.ok(result)
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
                    "dataillegal.koejakson-valiarviointia-ei-loydy"
                )
            }

            if (valiarviointi.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata väliarviointia, " +
                        "jos kouluttaja ei ole hyväksynyt sitä",
                    ENTITY_KOEJAKSON_VALIARVIOINTI,
                    "dataillegal.esimies-ei-saa-muoktata-valiarviointia-jos-kouluttaja-ei-ole-hyvaksynyt-sita"
                )
            }
        }

        validateArviointi(
            valiarviointi.get().lahiesimies?.sopimusHyvaksytty,
            ENTITY_KOEJAKSON_VALIARVIOINTI
        )

        val result = koejaksonValiarviointiService.update(valiarviointiDTO, user.id!!)
        return ResponseEntity.ok(result)
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
                    "dataillegal.koejakson-kehittamistoimenpiteita-ei-loydy"
                )
            }

            if (kehittamistoimenpiteet.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata kehittämistoimenpiteitä, " +
                        "jos kouluttaja ei ole hyväksynyt niitä",
                    ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                    "dataillegal.esimies-ei-saa-muokata-kehittamistoimenpiteita-jos-kouluttaja-ei-ole-hyvaksynyt-niita"
                )
            }
        }

        validateArviointi(
            kehittamistoimenpiteet.get().lahiesimies?.sopimusHyvaksytty,
            ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET
        )

        val result =
            koejaksonKehittamistoimenpiteetService.update(kehittamistoimenpiteetDTO, user.id!!)
        return ResponseEntity.ok(result)
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
                    "dataillegal.koejakson-loppukeskustelua-ei-loydy"
                )
            }

            if (loppukeskustelu.get().lahikouluttaja?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Esimies ei saa muokata loppukeskustelua, " +
                        "jos kouluttaja ei ole hyväksynyt niitä",
                    ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                    "dataillegal.esimies-ei-saa-muokata-loppukeskustelua-jos-kouluttaja-ei-ole-hyvaksynyt-niita"
                )
            }
        }

        validateArviointi(
            loppukeskustelu.get().lahiesimies?.sopimusHyvaksytty,
            ENTITY_KOEJAKSON_LOPPUKESKUSTELU
        )

        val result = koejaksonLoppukeskusteluService.update(loppukeskusteluDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    private fun validateArviointi(
        hyvaksytty: Boolean?,
        entity: String
    ) {
        if (hyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä arviointia ei saa muokata",
                entity,
                "dataillegal.hyvaksyttya-arviointia-ei-saa-muokata"
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
