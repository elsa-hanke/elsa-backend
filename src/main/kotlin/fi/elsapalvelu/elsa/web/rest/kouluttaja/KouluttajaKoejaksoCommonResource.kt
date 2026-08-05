package fi.elsapalvelu.elsa.web.rest.kouluttaja

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
