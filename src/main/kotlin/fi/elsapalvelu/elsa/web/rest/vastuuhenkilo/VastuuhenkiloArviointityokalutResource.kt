package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.arviointi.ArviointityokaluKategoriaService
import fi.elsapalvelu.elsa.service.arviointi.ArviointityokaluService
import fi.elsapalvelu.elsa.web.rest.common.ArviointityokalutResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloArviointityokalutResource(
    arviointityokaluService: ArviointityokaluService,
    arviointityokaluKategoriaService: ArviointityokaluKategoriaService,
) : ArviointityokalutResource(arviointityokaluService, arviointityokaluKategoriaService)
