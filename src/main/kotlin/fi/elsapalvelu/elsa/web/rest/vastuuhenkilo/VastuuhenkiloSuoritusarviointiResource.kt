package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.SuoritusarviointiQueryService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.UserService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloSuoritusarviointiResource(
    suoritusarviointiService: SuoritusarviointiService,
    suoritusarviointiQueryService: SuoritusarviointiQueryService,
    userService: UserService,
    arviointityokaluService: ArviointityokaluService
) : SuoritusarviointiResource(
    suoritusarviointiService,
    suoritusarviointiQueryService,
    userService,
    arviointityokaluService
)
