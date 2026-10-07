package fi.elsapalvelu.elsa.service.integration.peppi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.security.currentUserIdLogField
import fi.elsapalvelu.elsa.service.integration.LocalizedString
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusOsakokonaisuusDTO
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale

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
                    log.error(
                        "$JSON_FETCHING_ERROR: $endpointUrl${currentUserIdLogField()} " +
                            response.body?.string()
                    )
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, object : TypeReference<List<StudyAccomplishment>>() {})
                        ?.let { accomplishments ->
                            OpintosuorituksetPersistenceDTO(
                                yliopisto = yliopistoEnum,
                                items = toOpintosuoritusDtos(accomplishments, yliopistoEnum),
                                replaceOsakokonaisuudet = true
                            )
                        }
                }
            }
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl${currentUserIdLogField()} ${e.message}",
                e
            )
            throw e
        } catch (e: IOException) {
            log.error("$JSON_FETCHING_ERROR: $endpointUrl${currentUserIdLogField()} ${e.message}", e)
            throw e
        }
    }

    private fun toOpintosuoritusDtos(
        accomplishments: List<StudyAccomplishment>,
        yliopisto: YliopistoEnum
    ): List<OpintosuoritusDTO> {
        val examCodes = when (yliopisto) {
            YliopistoEnum.TURUN_YLIOPISTO -> setOf("ELOP0001", "EHLO0001")
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> setOf("4415200", "4415100")
            else -> emptySet()
        }
        // Ordinary courses keep the original mapping and response order, including repeat attempts.
        val ordinary = accomplishments.filter { it.kurssiKoodi !in examCodes }
            .map { it.toFlatOpintosuoritusDto() }
        val exams = accomplishments.filter { it.kurssiKoodi in examCodes }
            .groupBy { it.studyEntitlementKey to it.kurssiKoodi }
            .values.flatMap { group ->
                val (valid, invalid) = group.partition { it.isValid() }
                val invalidDtos = invalid.map { it.toFlatOpintosuoritusDto() }
                val primary = selectAccomplishment(valid.filter { it.isMainExam() })
                val incompleteSubparts = invalid.any { !it.isMainExam() && it.hyvaksytty != false }
                if (primary == null || incompleteSubparts) {
                    // A subpart alone cannot certify the whole exam. Preserve legacy validation logging
                    // for invalid records, but never persist a valid subpart as the parent exam.
                    log.warn(
                        "Peppi: valid main exam missing or incomplete subpart data for course {} " +
                            "and study entitlement {}. Exam import skipped.",
                        group.first().kurssiKoodi, group.first().studyEntitlementKey
                    )
                    invalidDtos
                } else {
                    invalidDtos + primary.toFlatOpintosuoritusDto().apply {
                        // Retakes of the main exam are not components of the main exam.
                        osakokonaisuudet = valid.filterNot { it.isMainExam() }
                            .groupBy { it.osakokonaisuusKurssikoodi() }
                            .values.mapNotNull { selectAccomplishment(it) }
                            .sortedBy { it.osakokonaisuusKurssikoodi() }
                            .map { it.toOsakokonaisuusDto() }
                        // An empty list is an authoritative main-only result, unlike null.
                    }
                }
            }
        return ordinary + exams
    }

    private fun selectAccomplishment(group: List<StudyAccomplishment>): StudyAccomplishment? =
        group.maxWithOrNull(
            compareBy<StudyAccomplishment> { it.hyvaksytty == true }
                .thenBy { it.suoritusPvm?.tryParseToLocalDate() }
                .thenBy { it.id.orEmpty() }
                // Missing or duplicate IDs must not restore dependence on response order.
                .thenBy { objectMapper.writeValueAsString(it) }
        )

    private fun StudyAccomplishment.isMainExam(): Boolean =
        normalizedName() in setOf(
            "valtakunnallinen erikoislääkärikuulustelu",
            "valtakunnallinen erikoishammaslääkärikuulustelu",
            "erikoislääkärikuulustelu",
            "erikoishammaslääkärikuulustelu"
        )

    private fun StudyAccomplishment.normalizedName(): String =
        nimi?.fi.orEmpty().trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun StudyAccomplishment.toFlatOpintosuoritusDto() = OpintosuoritusDTO(
        suorituspaiva = suoritusPvm?.tryParseToLocalDate(),
        opintopisteet = opintopisteet,
        nimi_fi = nimi?.fi,
        nimi_sv = nimi?.sv,
        kurssikoodi = kurssiKoodi,
        hyvaksytty = hyvaksytty,
        arvio_fi = arvio?.fi,
        arvio_sv = arvio?.sv,
        yliopistoOpintooikeusId = studyEntitlementKey
    )

    private fun StudyAccomplishment.osakokonaisuusKurssikoodi(): String {
        val sourceId = id?.takeIf { it.isNotBlank() }
        if (sourceId != null && sourceId.length <= 50) return sourceId
        // Keep the key within varchar(50) without truncating names into identical keys.
        val identity = sourceId?.let { "id:$it" } ?: "name:$kurssiKoodi:${normalizedName()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(StandardCharsets.UTF_8))
        return "hash:" + HexFormat.of().formatHex(hash).take(40)
    }

    private fun StudyAccomplishment.toOsakokonaisuusDto(): OpintosuoritusOsakokonaisuusDTO {
        return OpintosuoritusOsakokonaisuusDTO(
            suorituspaiva = suoritusPvm?.tryParseToLocalDate(),
            opintopisteet = opintopisteet,
            nimi_fi = nimi?.fi,
            nimi_sv = nimi?.sv,
            kurssikoodi = osakokonaisuusKurssikoodi(),
            hyvaksytty = hyvaksytty,
            arvio_fi = arvio?.fi,
            arvio_sv = arvio?.sv
        )
    }

    private fun StudyAccomplishment.isValid(): Boolean =
        kurssiKoodi != null && studyEntitlementKey != null && nimi?.fi != null && hyvaksytty != null &&
            suoritusPvm?.tryParseToLocalDate() != null
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class StudyAccomplishment(
    val id: String? = null,
    val studyEntitlementId: String?,
    val studyEntitlementKey: String?,
    val kurssiKoodi: String?,
    val suoritusPvm: String?,
    val opintopisteet: Double?,
    val nimi: LocalizedString?,
    val hyvaksytty: Boolean?,
    val arvio: LocalizedString?
)
