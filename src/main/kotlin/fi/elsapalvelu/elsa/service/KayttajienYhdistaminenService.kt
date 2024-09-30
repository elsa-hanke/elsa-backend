package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenResult

interface KayttajienYhdistaminenService {

    fun yhdistaKayttajatilit(
        kayttajienYhdistaminenDTo: KayttajienYhdistaminenDTO
    ): List<KayttajienYhdistaminenResult>

}
