package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum

interface SarakesignService {

    fun lahetaAllekirjoitettavaksi(
        title: String,
        recipients: List<User>,
        asiakirjaId: Long,
        yliopisto: YliopistoEnum
    ): String

    fun tarkistaAllekirjoitus(requestId: String?, yliopisto: YliopistoEnum): Int?

    fun getApiUrl(yliopisto: YliopistoEnum): String?
}
