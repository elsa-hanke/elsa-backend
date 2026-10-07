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
                                items = toOpintosuoritusDtos(accomplishments)
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

    // Lähdejärjestelmä voi palauttaa useamman suorituksen samalla opinto-oikeudella ja kurssikoodilla,
    // esim. kun yksi tutkinnon osa koostuu useasta arvioitavasta kokonaisuudesta (esseet, preparaatit,
    // kuulustelu). Tallennuspalvelu tunnistaa suoritukset opinto-oikeuden ja kurssikoodin perusteella, joten
    // tällaiset suoritukset yhdistetään yhdeksi pääsuoritukseksi, jonka osakokonaisuuksina muut säilyvät.
    private fun toOpintosuoritusDtos(accomplishments: List<StudyAccomplishment>): List<OpintosuoritusDTO> {
        val (valid, invalid) = accomplishments.partition { it.isValid() }

        // Puutteellisia suorituksia ei ryhmitellä: tallennuspalvelu ohittaa ne joka tapauksessa
        // puuttuvan tiedon (esim. suorituspäivämäärän) vuoksi yksitellen, aivan kuten ennenkin.
        val invalidDtos = invalid.map { it.toFlatOpintosuoritusDto() }

        val validDtos = valid
            .groupBy { it.studyEntitlementKey to it.kurssiKoodi }
            .values
            .map { group ->
                if (group.size == 1) {
                    group.first().toFlatOpintosuoritusDto()
                } else {
                    mergeIntoOpintosuoritusDto(group)
                }
            }

        return invalidDtos + validDtos
    }

    private fun mergeIntoOpintosuoritusDto(group: List<StudyAccomplishment>): OpintosuoritusDTO {
        val primary = selectPrimaryAccomplishment(group)

        return primary.toFlatOpintosuoritusDto().apply {
            osakokonaisuudet = group
                .filter { it !== primary }
                .sortedWith(compareBy<StudyAccomplishment> { it.suoritusPvm }.thenBy { it.id })
                .mapNotNull { it.toOsakokonaisuusDtoOrLogError() }
                .ifEmpty { null }
        }
    }

    private fun selectPrimaryAccomplishment(group: List<StudyAccomplishment>): StudyAccomplishment {
        return group.maxWithOrNull(
            compareBy<StudyAccomplishment> { it.hyvaksytty == true }
                .thenBy { isMainCourse(it) }
                .thenBy { it.suoritusPvm?.tryParseToLocalDate() }
                .thenBy { it.opintopisteet ?: 0.0 }
                .thenBy { !it.nimi?.sv.isNullOrBlank() }
        ) ?: group.first()
    }

    private fun isMainCourse(accomplishment: StudyAccomplishment): Boolean {
        val nimiFi = accomplishment.nimi?.fi.orEmpty()
        // Valtakunnallinen erikoislääkärikuulustelu (ELOP0001) / erikoishammaslääkärikuulustelu (EHLO0001)
        if (nimiFi.contains("VALTAKUNNALLINEN", ignoreCase = true)) {
            return true
        }
        val nimiSv = accomplishment.nimi?.sv.orEmpty()
        // Jos suorituksella on virallinen ruotsinkielinen nimi eikä suomenkielinen nimi sisällä
        // pilkkueroteltua osasuorituksen tarkenninta (kuten "Patologia, esseet"), kyseessä on päätason kurssi.
        return nimiSv.isNotBlank() && !nimiFi.contains(", ")
    }

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

    private fun StudyAccomplishment.toOsakokonaisuusDtoOrLogError(): OpintosuoritusOsakokonaisuusDTO? {
        val osakokonaisuusKurssikoodi = id?.takeIf { it.isNotBlank() } ?: "${kurssiKoodi}_${nimi?.fi}".take(50)
        return OpintosuoritusOsakokonaisuusDTO(
            suorituspaiva = suoritusPvm?.tryParseToLocalDate(),
            opintopisteet = opintopisteet,
            nimi_fi = nimi?.fi,
            nimi_sv = nimi?.sv,
            kurssikoodi = osakokonaisuusKurssikoodi,
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
