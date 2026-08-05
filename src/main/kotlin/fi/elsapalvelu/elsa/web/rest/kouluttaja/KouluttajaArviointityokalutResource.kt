package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.ArviointityokaluKategoriaService
import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.web.rest.common.ArviointityokalutResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaArviointityokalutResource(
    arviointityokaluService: ArviointityokaluService,
    arviointityokaluKategoriaService: ArviointityokaluKategoriaService,
) : ArviointityokalutResource(arviointityokaluService, arviointityokaluKategoriaService)
