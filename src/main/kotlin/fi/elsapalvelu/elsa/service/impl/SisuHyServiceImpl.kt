package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.PrivatePersonByPersonalIdentityCodeQuery
import fi.elsapalvelu.elsa.service.SisuHyClientBuilder
import fi.elsapalvelu.elsa.service.SisuHyService
import org.springframework.stereotype.Service

@Service
class SisuHyServiceImpl(
    private val sisuHyClientBuilder: SisuHyClientBuilder
) : SisuHyService {
    override suspend fun getPrivatePersonIdByPersonalIdentityCode(id: String):
        PrivatePersonByPersonalIdentityCodeQuery.Private_person_by_personal_identity_code? {
        val response =
            sisuHyClientBuilder.apolloClient().query(PrivatePersonByPersonalIdentityCodeQuery(id = id))
                .execute()
        return response.data?.private_person_by_personal_identity_code
    }
}


