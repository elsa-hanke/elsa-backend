package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

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
import fi.elsapalvelu.elsa.web.rest.KayttajahallintaResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaKayttajahallintaResource(
    erikoistuvaLaakariService: ErikoistuvaLaakariService,
    userService: UserService,
    kayttajaService: KayttajaService,
    yliopistoService: YliopistoService,
    erikoisalaService: ErikoisalaService,
    asetusService: AsetusService,
    opintoopasService: OpintoopasService,
    kayttajahallintaValidationService: KayttajahallintaValidationService,
    mailService: MailService,
    opintooikeusService: OpintooikeusService,
    kayttajienYhdistaminenService: KayttajienYhdistaminenService
) : KayttajahallintaResource(
    erikoistuvaLaakariService,
    userService,
    kayttajaService,
    yliopistoService,
    erikoisalaService,
    asetusService,
    opintoopasService,
    kayttajahallintaValidationService,
    mailService,
    opintooikeusService,
    kayttajienYhdistaminenService
)
