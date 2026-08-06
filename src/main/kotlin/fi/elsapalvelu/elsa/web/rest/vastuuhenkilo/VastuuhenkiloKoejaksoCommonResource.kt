package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

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
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import fi.elsapalvelu.elsa.web.rest.KoejaksoResource
import fi.elsapalvelu.elsa.web.rest.common.KoejaksoResourceSupport
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloKoejaksoCommonResource(
    userService: UserService,
    koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    koejaksonValiarviointiService: KoejaksonValiarviointiService,
    koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    koejaksoResourceSupport: KoejaksoResourceSupport
) : KoejaksoResource(
    userService,
    koejaksonAloituskeskusteluService,
    koejaksonValiarviointiService,
    koejaksonKehittamistoimenpiteetService,
    koejaksonLoppukeskusteluService,
    koejaksoResourceSupport
)
