package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.sarakesign.SarakeSignResponseRequestDTO

interface SarakesignService {

    fun lahetaAllekirjoitettavaksi(
        title: String,
        recipients: List<User>,
        asiakirjaId: Long,
        yliopisto: YliopistoEnum
    ): String

    fun tarkistaAllekirjoitus(requestId: String?, yliopisto: YliopistoEnum): SarakeSignResponseRequestDTO?

    fun getApiUrl(yliopisto: YliopistoEnum): String?
}
