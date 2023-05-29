package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.web.rest.SeurantakeskustelutResource
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/vastuuhenkilo/seurantakeskustelut")
class VastuuhenkiloSeurantakeskustelutResource(
    userService: UserService,
    seurantajaksoService: SeurantajaksoService
) : SeurantakeskustelutResource(userService, seurantajaksoService)
