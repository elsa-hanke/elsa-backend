package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.PrivatePersonByPersonalIdentityCodeQuery

interface SisuHyService {
    suspend fun getPrivatePersonIdByPersonalIdentityCode(id: String) :
        PrivatePersonByPersonalIdentityCodeQuery.Private_person_by_personal_identity_code?
}
