package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.KoejaksoResource
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaKoejaksoCommonResource(
    userService: UserService,
    koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    koejaksonValiarviointiService: KoejaksonValiarviointiService,
    koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService
) : KoejaksoResource(
    userService,
    koejaksonAloituskeskusteluService,
    koejaksonValiarviointiService,
    koejaksonKehittamistoimenpiteetService,
    koejaksonLoppukeskusteluService
)
