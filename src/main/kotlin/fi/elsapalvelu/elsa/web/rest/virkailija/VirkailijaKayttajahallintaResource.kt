package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.web.rest.KayttajahallintaResource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaKayttajahallintaResource(
    erikoistuvaLaakariService: ErikoistuvaLaakariService,
    userService: UserService,
    kayttajaService: KayttajaService,
    yliopistoService: YliopistoService,
    erikoisalaService: ErikoisalaService,
    asetusService: AsetusService,
    opintoopasService: OpintoopasService,
    kayttajahallintaValidationService: KayttajahallintaValidationService,
    mailService: MailService,
    opintooikeusService: OpintooikeusService
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
    opintooikeusService
)
