package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDateTime
import fi.elsapalvelu.elsa.service.PeppiCommonOpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class PeppiCommonOpintosuorituksetFetchingServiceImpl(
    private val objectMapper: ObjectMapper
) : PeppiCommonOpintosuorituksetFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(
        endpointUrl: String,
        client: OkHttpClient,
        hetu: String,
        yliopistoEnum: YliopistoEnum
    ): OpintosuorituksetPersistenceDTO? {
        val postBody = "{\"hetu\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()

        try {
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, object : TypeReference<List<StudyAccomplishment>>() {})
                        ?.let { accomplishments ->
                            OpintosuorituksetPersistenceDTO(
                                yliopisto = yliopistoEnum,
                                items = accomplishments.map {
                                    OpintosuoritusDTO(
                                        suorituspaiva = it.suoritusPvm?.tryParseToLocalDateTime()?.toLocalDate(),
                                        opintopisteet = it.opintopisteet,
                                        nimi_fi = it.nimi?.fi,
                                        nimi_sv = it.nimi?.sv,
                                        kurssikoodi = it.kurssiKoodi,
                                        hyvaksytty = it.hyvaksytty,
                                        arvio_fi = it.arvio?.fi,
                                        arvio_sv = it.arvio?.sv,
                                        yliopistoOpintooikeusId = it.studyEntitlementKey
                                    )
                                }
                            )
                        }
                }
            }
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonMappingException) {
            log.error(
                "$JSON_MAPPING_ERROR: $endpointUrl ${e.message} "
            )
        }
        return null
    }
}


data class StudyAccomplishment(
    val studyEntitlementId: String?,
    val studyEntitlementKey: String?,
    val kurssiKoodi: String?,
    val suoritusPvm: String?,
    val opintopisteet: Double?,
    val nimi: LocalizedString?,
    val hyvaksytty: Boolean?,
    val arvio: LocalizedString?
)

data class LocalizedString(
    val fi: String?,
    val sv: String?
)
