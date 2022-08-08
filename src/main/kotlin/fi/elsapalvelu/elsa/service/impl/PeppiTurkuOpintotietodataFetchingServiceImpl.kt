package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDateTime
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.PeppiOpintooikeudenTila
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

private const val studentEndpoint = "student"

@Service
class PeppiTurkuOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiTurku") private val peppiTurkuClientBuilder: OkHttpClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/$studentEndpoint"
        val postBody = "{\"hetu\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()
        try {
            return peppiTurkuClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, Student::class.java)?.let { student ->
                        OpintotietodataDTO(
                            syntymaaika = student.birthDate?.tryParseToLocalDateTime()?.toLocalDate(),
                            opintooikeudet = student.entitlements?.filter { e ->
                                e.koulutusKoodi == ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS ||
                                    e.koulutusKoodi == ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS
                            }?.map {
                                OpintotietoOpintooikeusDataDTO(
                                    id = it.opiskeluoikeusNumero,
                                    opintooikeudenAlkamispaiva = it.alkamispaiva?.tryParseToLocalDateTime()
                                        ?.toLocalDate(),
                                    opintooikeudenPaattymispaiva = it.paattymispaiva?.tryParseToLocalDateTime()
                                        ?.toLocalDate(),
                                    erikoisalaTunnisteList = it.erikoisalat?.map { e -> e.avain },
                                    asetus = convertPeppiAsetusString(it.asetus, endpointUrl),
                                    tila = mapOpintooikeudenTila(it.tila),
                                    yliopisto = YliopistoEnum.TURUN_YLIOPISTO
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

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.TURUN_YLIOPISTO)?.haeOpintotietodata == true
    }

    private fun convertPeppiAsetusString(peppiAsetus: String?, endpointUrl: String): String? {
        val splittedAsetus = peppiAsetus?.split("-")
        if (splittedAsetus?.size != 2) {
            log.error("Rajapinnasta $endpointUrl ei saatu kelvollista asetustietoa. Asetus: $peppiAsetus")
            return null
        }
        return "${splittedAsetus[1]}/${splittedAsetus[0]}"
    }

    private fun mapOpintooikeudenTila(sisuOpintooikeudenTila: PeppiOpintooikeudenTila?): OpintooikeudenTila? {
        return when (sisuOpintooikeudenTila) {
            PeppiOpintooikeudenTila.ATTENDING -> OpintooikeudenTila.AKTIIVINEN
            PeppiOpintooikeudenTila.ABSENT,
            PeppiOpintooikeudenTila.ABSENT_EXPENDING,
            PeppiOpintooikeudenTila.ABSENT_NON_EXPENDING,
            PeppiOpintooikeudenTila.OTHER_NON_EXPENDING -> OpintooikeudenTila.PASSIIVINEN
            PeppiOpintooikeudenTila.GRADUATED -> OpintooikeudenTila.VALMISTUNUT
            PeppiOpintooikeudenTila.RESIGNED -> OpintooikeudenTila.PERUUTETTU
            else -> null
        }
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
