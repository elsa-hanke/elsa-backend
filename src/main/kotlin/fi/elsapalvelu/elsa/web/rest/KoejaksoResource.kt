package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.koejakso.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.service.arviointi.*
import fi.elsapalvelu.elsa.service.suoritteet.*
import fi.elsapalvelu.elsa.service.koulutus.*
import fi.elsapalvelu.elsa.service.seuranta.*
import fi.elsapalvelu.elsa.service.valmistuminen.*
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.perustiedot.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import fi.elsapalvelu.elsa.web.rest.common.KoejaksoResourceSupport
import fi.elsapalvelu.elsa.web.rest.kouluttaja.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_KOEJAKSON_ALOITUSKESKUSTELU = "koejakson_aloituskeskustelu"
private const val ENTITY_KOEJAKSON_VALIARVIOINTI = "koejakson_valiarviointi"
private const val ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET = "koejakson_kehittamistoimenpiteet"
private const val ENTITY_KOEJAKSON_LOPPUKESKUSTELU = "koejakson_loppukeskustelu"

open class KoejaksoResource(
    private val userService: UserService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val koejaksoResourceSupport: KoejaksoResourceSupport
) {

    @PutMapping("/koejakso/aloituskeskustelu")
    fun updateAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        koejaksoResourceSupport.validateId(aloituskeskusteluDTO.id, ENTITY_KOEJAKSON_ALOITUSKESKUSTELU)
        val user = userService.getAuthenticatedUser(principal)

        val aloituskeskustelu = koejaksoResourceSupport.findForUpdateByLahikouluttajaOrLahiesimies(
            id = aloituskeskusteluDTO.id!!,
            userId = user.id!!,
            entity = ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
            notFoundMessage = "Koejakson aloituskeskustelua ei löydy.",
            notFoundErrorKey = "dataillegal.koejakson-aloituskeskustelua-ei-loydy",
            lahiesimiesBeforeLahikouluttajaMessage = "Esihenkilö ei saa muokata aloituskeskustelua, " +
                "jos kouluttaja ei ole hyväksynyt sitä",
            lahiesimiesBeforeLahikouluttajaErrorKey = "dataillegal.esimies-ei-saa-muokata-aloituskeskustelua-jos-kouluttaja-ei-ole-hyvaksynyt-sita",
            findByLahikouluttaja = koejaksonAloituskeskusteluService::findOneByIdAndLahikouluttajaUserId,
            findByLahiesimies = koejaksonAloituskeskusteluService::findOneByIdAndLahiesimiesUserId,
            lahikouluttajaSopimusHyvaksytty = { it.lahikouluttaja?.sopimusHyvaksytty }
        )

        koejaksoResourceSupport.validateLahetetty(
            aloituskeskustelu.lahetetty,
            ENTITY_KOEJAKSON_ALOITUSKESKUSTELU
        )

        koejaksoResourceSupport.validateArviointi(
            aloituskeskustelu.lahiesimies?.sopimusHyvaksytty,
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
        koejaksoResourceSupport.validateId(valiarviointiDTO.id, ENTITY_KOEJAKSON_LOPPUKESKUSTELU)
        val user = userService.getAuthenticatedUser(principal)

        val valiarviointi = koejaksoResourceSupport.findForUpdateByLahikouluttajaOrLahiesimies(
            id = valiarviointiDTO.id!!,
            userId = user.id!!,
            entity = ENTITY_KOEJAKSON_VALIARVIOINTI,
            notFoundMessage = "Koejakson väliarviointia ei löydy.",
            notFoundErrorKey = "dataillegal.koejakson-valiarviointia-ei-loydy",
            lahiesimiesBeforeLahikouluttajaMessage = "Esimies ei saa muokata väliarviointia, " +
                "jos kouluttaja ei ole hyväksynyt sitä",
            lahiesimiesBeforeLahikouluttajaErrorKey = "dataillegal.esimies-ei-saa-muoktata-valiarviointia-jos-kouluttaja-ei-ole-hyvaksynyt-sita",
            findByLahikouluttaja = koejaksonValiarviointiService::findOneByIdAndLahikouluttajaUserId,
            findByLahiesimies = koejaksonValiarviointiService::findOneByIdAndLahiesimiesUserId,
            lahikouluttajaSopimusHyvaksytty = { it.lahikouluttaja?.sopimusHyvaksytty }
        )

        koejaksoResourceSupport.validateArviointi(
            valiarviointi.lahiesimies?.sopimusHyvaksytty,
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
        koejaksoResourceSupport.validateId(kehittamistoimenpiteetDTO.id, ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET)
        val user = userService.getAuthenticatedUser(principal)

        val kehittamistoimenpiteet = koejaksoResourceSupport.findForUpdateByLahikouluttajaOrLahiesimies(
            id = kehittamistoimenpiteetDTO.id!!,
            userId = user.id!!,
            entity = ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
            notFoundMessage = "Koejakson kehittämistoimenpiteitä ei löydy.",
            notFoundErrorKey = "dataillegal.koejakson-kehittamistoimenpiteita-ei-loydy",
            lahiesimiesBeforeLahikouluttajaMessage = "Esimies ei saa muokata kehittämistoimenpiteitä, " +
                "jos kouluttaja ei ole hyväksynyt niitä",
            lahiesimiesBeforeLahikouluttajaErrorKey = "dataillegal.esimies-ei-saa-muokata-kehittamistoimenpiteita-jos-kouluttaja-ei-ole-hyvaksynyt-niita",
            findByLahikouluttaja = koejaksonKehittamistoimenpiteetService::findOneByIdAndLahikouluttajaUserId,
            findByLahiesimies = koejaksonKehittamistoimenpiteetService::findOneByIdAndLahiesimiesUserId,
            lahikouluttajaSopimusHyvaksytty = { it.lahikouluttaja?.sopimusHyvaksytty }
        )

        koejaksoResourceSupport.validateArviointi(
            kehittamistoimenpiteet.lahiesimies?.sopimusHyvaksytty,
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
        koejaksoResourceSupport.validateId(loppukeskusteluDTO.id, ENTITY_KOEJAKSON_LOPPUKESKUSTELU)
        val user = userService.getAuthenticatedUser(principal)

        val loppukeskustelu = koejaksoResourceSupport.findForUpdateByLahikouluttajaOrLahiesimies(
            id = loppukeskusteluDTO.id!!,
            userId = user.id!!,
            entity = ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
            notFoundMessage = "Koejakson loppukeskustelua ei löydy.",
            notFoundErrorKey = "dataillegal.koejakson-loppukeskustelua-ei-loydy",
            lahiesimiesBeforeLahikouluttajaMessage = "Esimies ei saa muokata loppukeskustelua, " +
                "jos kouluttaja ei ole hyväksynyt niitä",
            lahiesimiesBeforeLahikouluttajaErrorKey = "dataillegal.esimies-ei-saa-muokata-loppukeskustelua-jos-kouluttaja-ei-ole-hyvaksynyt-niita",
            findByLahikouluttaja = koejaksonLoppukeskusteluService::findOneByIdAndLahikouluttajaUserId,
            findByLahiesimies = koejaksonLoppukeskusteluService::findOneByIdAndLahiesimiesUserId,
            lahikouluttajaSopimusHyvaksytty = { it.lahikouluttaja?.sopimusHyvaksytty }
        )

        koejaksoResourceSupport.validateArviointi(
            loppukeskustelu.lahiesimies?.sopimusHyvaksytty,
            ENTITY_KOEJAKSON_LOPPUKESKUSTELU
        )

        val result = koejaksonLoppukeskusteluService.update(loppukeskusteluDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

}
