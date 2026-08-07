package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.kayttaja.UserService
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
import fi.elsapalvelu.elsa.web.rest.SeurantakeskustelutResource
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/vastuuhenkilo/seurantakeskustelut")
class VastuuhenkiloSeurantakeskustelutResource(
    userService: UserService,
    seurantajaksoService: SeurantajaksoService
) : SeurantakeskustelutResource(userService, seurantajaksoService)
