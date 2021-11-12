package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.service.SuoritusarviointiQueryService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.UserService
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
    fileValidationService: FileValidationService
) : SuoritusarviointiResource(
    suoritusarviointiService,
    suoritusarviointiQueryService,
    userService,
    objectMapper,
    fileValidationService
)
