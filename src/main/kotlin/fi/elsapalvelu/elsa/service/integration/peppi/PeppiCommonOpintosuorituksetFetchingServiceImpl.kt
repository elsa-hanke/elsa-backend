package fi.elsapalvelu.elsa.service.integration.peppi

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
    // tällaiset suoritukset yhdistetään tässä yhdeksi suoritukseksi, jonka osakokonaisuuksina muut säilyvät -
    // muuten vain viimeisenä käsitelty suoritus jäisi talteen ja muut ylikirjoittuisivat huomaamatta.
    private fun toOpintosuoritusDtos(accomplishments: List<StudyAccomplishment>): List<OpintosuoritusDTO> {
        val (valid, invalid) = accomplishments.partition { it.isValid() }

        // Puutteellisia suorituksia ei ryhmitellä: tallennuspalvelu ohittaa (ja lokittaa) ne joka
        // tapauksessa yksitellen, aivan kuten ennenkin.
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

    // Ryhmän "pääsuoritus" valitaan uusimman suorituspäivän perusteella. Jos suorituspäivät ovat samat
    // (tyypillistä, kun kaikki osat on arvioitu samana päivänä), valinta on silti deterministinen: ensimmäisenä
    // lähdejärjestelmän listassa ollut suoritus säilyy pääsuorituksena. Loput ryhmän suorituksista tallennetaan
    // osakokonaisuuksina, avaimenaan lähdejärjestelmän oma tunniste, jotta ne pysyvät samoina myöhemmillä
    // tuontikerroilla eivätkä esim. törmää keskenään.
    private fun mergeIntoOpintosuoritusDto(group: List<StudyAccomplishment>): OpintosuoritusDTO {
        val primary = group.reduce { best, candidate ->
            val bestPvm = best.suoritusPvm?.tryParseToLocalDate()
            val candidatePvm = candidate.suoritusPvm?.tryParseToLocalDate()
            if (candidatePvm != null && (bestPvm == null || candidatePvm.isAfter(bestPvm))) candidate else best
        }

        return primary.toFlatOpintosuoritusDto().apply {
            osakokonaisuudet = group.filter { it !== primary }.mapNotNull { it.toOsakokonaisuusDtoOrLogError() }
                .ifEmpty { null }
        }
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
        val osakokonaisuusKurssikoodi = id ?: run {
            log.warn(
                "$JSON_DATA_PROSESSING_ERROR:${currentUserIdLogField()} lähdejärjestelmän suorituksella " +
                    "(kurssikoodi=$kurssiKoodi, opinto-oikeus=$studyEntitlementKey) ei ole tunnistetta, " +
                    "joten sitä ei voida tallentaa osakokonaisuutena."
            )
            return null
        }
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


data class StudyAccomplishment(
    val id: String?,
    val studyEntitlementId: String?,
    val studyEntitlementKey: String?,
    val kurssiKoodi: String?,
    val suoritusPvm: String?,
    val opintopisteet: Double?,
    val nimi: LocalizedString?,
    val hyvaksytty: Boolean?,
    val arvio: LocalizedString?
)
