package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.*
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
    opintoopasService: OpintoopasService
) : KayttajahallintaResource(
    erikoistuvaLaakariService,
    userService,
    kayttajaService,
    yliopistoService,
    erikoisalaService,
    asetusService,
    opintoopasService
)
