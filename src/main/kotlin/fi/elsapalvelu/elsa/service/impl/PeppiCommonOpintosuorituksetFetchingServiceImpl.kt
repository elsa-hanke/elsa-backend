package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
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
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body?.string()}")
                    return null
                }
                val body = response.body?.string()
                val accomplishments = parseAccomplishments(body, endpointUrl) ?: return null
                OpintosuorituksetPersistenceDTO(
                    yliopisto = yliopistoEnum,
                    items = accomplishments.map {
                        OpintosuoritusDTO(
                            suorituspaiva = it.suoritusPvm?.tryParseToLocalDate(),
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
        } catch (e: JsonProcessingException) {
            log.error("$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}")
        } catch (e: JsonMappingException) {
            log.error("$JSON_MAPPING_ERROR: $endpointUrl ${e.message}")
        } catch (e: IOException) {
            log.error("$JSON_FETCHING_ERROR: $endpointUrl ${e.message}")
        }
        return null
    }

    /**
     * Parses the response body into a list of [StudyAccomplishment].
     *
     * The Peppi REST endpoint may return either:
     * - a top-level JSON array:  `[{...}, ...]`
     * - a wrapper JSON object:   `{"hetu":"...", "<listField>": [{...}, ...]}` where the list
     *   field name varies by university (e.g. `items`, `suoritukset`, `accomplishments`).
     *   When the hetu has no accomplishments the object contains no array fields at all.
     *
     * Returns an empty list for "no data" responses, and `null` only when parsing genuinely fails.
     */
    private fun parseAccomplishments(body: String?, endpointUrl: String): List<StudyAccomplishment>? {
        if (body.isNullOrBlank()) return emptyList()
        val listType = object : TypeReference<List<StudyAccomplishment>>() {}
        return try {
            val node: JsonNode = objectMapper.readTree(body)
            when {
                node.isArray -> objectMapper.convertValue(node, listType)
                node.isObject -> {
                    // Find the first field whose value is a JSON array and treat it as the list
                    val arrayField = node.fields().asSequence().firstOrNull { it.value.isArray }
                    if (arrayField != null) {
                        log.debug(
                            "Peppi study_accomplishments at {} returned wrapper object; " +
                                "extracting list from field '{}'", endpointUrl, arrayField.key
                        )
                        objectMapper.convertValue(arrayField.value, listType)
                    } else {
                        // Object with no array field = this hetu has no accomplishments
                        log.debug(
                            "Peppi study_accomplishments at {} returned object with no array field " +
                                "(no accomplishments for this hetu): {}", endpointUrl, body
                        )
                        emptyList()
                    }
                }
                else -> {
                    log.error("$JSON_DATA_PROSESSING_ERROR: $endpointUrl – unexpected JSON root type")
                    null
                }
            }
        } catch (e: Exception) {
            log.error("$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}")
            null
        }
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
