package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.seuranta.SeurantajaksoService
import fi.elsapalvelu.elsa.web.rest.SeurantakeskustelutResource
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/kouluttaja/seurantakeskustelut")
class KouluttajaSeurantakeskustelutResource(
    userService: UserService,
    seurantajaksoService: SeurantajaksoService
) : SeurantakeskustelutResource(userService, seurantajaksoService)
