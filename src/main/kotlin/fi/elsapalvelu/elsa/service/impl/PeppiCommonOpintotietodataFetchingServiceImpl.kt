package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS
import fi.elsapalvelu.elsa.config.YEK_KOULUTETTAVA_PEPPI_VIRTAKOODI
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila.Companion.fromPeppiOpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.service.PeppiCommonOpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.PeppiOpintooikeudenTila
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class PeppiCommonOpintotietodataFetchingServiceImpl(
    private val objectMapper: ObjectMapper
): PeppiCommonOpintotietodataFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(
        endpointUrl: String,
        client: OkHttpClient,
        hetu: String,
        yliopistoEnum: YliopistoEnum
    ): OpintotietodataDTO? {
        val postBody = "{\"hetu\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()

        try {
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body?.string()}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, Student::class.java)?.let { student ->
                        OpintotietodataDTO(
                            syntymaaika = student.birthDate?.tryParseToLocalDate(),
                            opintooikeudet = student.entitlements?.filter { e ->
                                e.koulutusKoodi == ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS ||
                                    e.koulutusKoodi == ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS ||
                                    e.erikoisalat?.any { ea -> ea.avain == YEK_KOULUTETTAVA_PEPPI_VIRTAKOODI } == true
                            }?.map {
                                OpintotietoOpintooikeusDataDTO(
                                    id = it.opiskeluoikeusNumero,
                                    opintooikeudenAlkamispaiva = it.alkamispaiva?.tryParseToLocalDate(),
                                    opintooikeudenPaattymispaiva = it.paattymispaiva?.tryParseToLocalDate(),
                                    erikoisalaTunnisteList = it.erikoisalat?.map { e -> e.avain }?.filter { avain ->
                                        it.koulutusKoodi == ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS ||
                                        it.koulutusKoodi == ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS ||
                                        avain == YEK_KOULUTETTAVA_PEPPI_VIRTAKOODI },
                                    asetus = convertPeppiAsetusString(it.asetus, endpointUrl),
                                    tila = fromPeppiOpintooikeudenTila(it.tila),
                                    yliopisto = yliopistoEnum
                                )
                            }
                        )
                    }
                }
            }
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonMappingException) {
            log.error(
                "$JSON_MAPPING_ERROR: $endpointUrl ${e.message} "
            )
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
        }
        return null
    }

    private fun convertPeppiAsetusString(peppiAsetus: String?, endpointUrl: String): String? {
        val splittedAsetus = peppiAsetus?.split("-")
        if (splittedAsetus?.size != 2) {
            log.warn("Rajapinnasta $endpointUrl ei saatu kelvollista asetustietoa. Asetus: $peppiAsetus")
            return null
        }
        return "${splittedAsetus[1]}/${splittedAsetus[0]}"
    }
}

data class Student(val birthDate: String?, val entitlements: List<Entitlement>?)

data class Entitlement(
    val opiskeluoikeusNumero: String?,

    @JsonProperty("alkamispäivä")
    val alkamispaiva: String?,

    @JsonProperty("päättymispäivä")
    val paattymispaiva: String?,

    val tila: PeppiOpintooikeudenTila?,

    val koulutusKoodi: String?,

    val asetus: String?,

    val erikoisalat: List<ErikoisalaPeppiTurku>?
)

data class ErikoisalaPeppiTurku(
    val avain: String
)

