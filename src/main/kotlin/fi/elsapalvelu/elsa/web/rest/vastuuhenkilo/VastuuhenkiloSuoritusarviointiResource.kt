package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import com.fasterxml.jackson.databind.ObjectMapper
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
import fi.elsapalvelu.elsa.web.rest.SuoritusarviointiResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloSuoritusarviointiResource(
    suoritusarviointiService: SuoritusarviointiService,
    suoritusarviointiQueryService: SuoritusarviointiQueryService,
    userService: UserService,
    objectMapper: ObjectMapper,
    fileValidationService: FileValidationService,
    kayttajaService: KayttajaService,
    arvioitavaKokonaisuusService: ArvioitavaKokonaisuusService
) : SuoritusarviointiResource(
    suoritusarviointiService,
    suoritusarviointiQueryService,
    userService,
    objectMapper,
    fileValidationService,
    kayttajaService,
    arvioitavaKokonaisuusService
)
