package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO

interface KayttajienYhdistaminenService {

    fun yhdistaKayttajatilit(
        kayttajienYhdistaminenDTo: KayttajienYhdistaminenDTO
    )


}
