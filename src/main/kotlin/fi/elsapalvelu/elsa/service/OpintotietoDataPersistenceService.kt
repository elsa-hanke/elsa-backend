package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintotietoDataDTO
import javax.crypto.Cipher
import javax.crypto.SecretKey

interface OpintotietoDataPersistenceService {

    fun create(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        etunimi: String,
        sukunimi: String,
        opintotietoDataDTO: OpintotietoDataDTO
    )

    fun createWithoutOpintotietoData(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        etunimi: String,
        sukunimi: String
    )

    fun createOrUpdateIfChanged(
        userId: String,
        etunimi: String,
        sukunimi: String,
        opintotietoDataDTO: OpintotietoDataDTO
    )
}
