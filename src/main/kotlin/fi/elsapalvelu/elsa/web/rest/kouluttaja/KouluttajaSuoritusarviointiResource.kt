package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.SuoritusarviointiQueryService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.web.rest.SuoritusarviointiResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaSuoritusarviointiResource(
    suoritusarviointiService: SuoritusarviointiService,
    suoritusarviointiQueryService: SuoritusarviointiQueryService,
    kayttajaService: KayttajaService
) : SuoritusarviointiResource(
    suoritusarviointiService,
    suoritusarviointiQueryService,
    kayttajaService
)
