package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import javax.crypto.Cipher
import javax.crypto.SecretKey

interface OpintotietodataPersistenceService {

    fun create(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        etunimi: String,
        sukunimi: String,
        opintotietodataDTO: OpintotietodataDTO
    )

    fun createWithoutOpintotietodata(
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
        opintotietodataDTO: OpintotietodataDTO
    )
}
