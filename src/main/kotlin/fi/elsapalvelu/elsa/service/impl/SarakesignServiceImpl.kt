package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.SarakesignService
import fi.elsapalvelu.elsa.service.dto.sarakesign.*
import io.undertow.util.BadRequestException
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.File
import java.nio.file.Files
import javax.persistence.EntityNotFoundException

@Service
@Transactional
class SarakesignServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val asiakirjaRepository: AsiakirjaRepository
) : SarakesignService {

    private val restTemplate = RestTemplate()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lahetaAllekirjoitettavaksi(
        title: String,
        recipients: List<User>,
        asiakirjaId: Long,
        yliopisto: YliopistoEnum
    ): String {
        val token = login(yliopisto)
        val requestId =
            createRequest(title, recipients.map { lisaaVastaanottaja(it) }, token, yliopisto)
        uploadDocument(title, asiakirjaId, requestId, token, yliopisto)
        sendRequest(requestId, token, yliopisto)
        log.info("Lähetetty pyyntö $requestId asiakirjasta $asiakirjaId SarakeSigniin allekirjoitettavaksi")
        return requestId
    }

    override fun tarkistaAllekirjoitus(requestId: String?, yliopisto: YliopistoEnum): SarakeSignResponseRequestDTO {
        return try {
            val token = login(yliopisto)
            getRequestStatus(requestId, token, yliopisto)
        } catch (e: Exception) {
            log.error("Sarakesign tilan tarkistus epäonnistui: $e")
            SarakeSignResponseRequestDTO()
        }
    }

    private fun login(yliopisto: YliopistoEnum): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val body = JSONObject(mapOf("token" to getApiKey(yliopisto)))
        val loginRequest = HttpEntity(body, headers)
        val loginUrl = getApiUrl(yliopisto) + "login/token"

        val loginResponse: ResponseEntity<JSONObject> = restTemplate.postForEntity(
            loginUrl, loginRequest,
            JSONObject::class.java
        )

        val token = loginResponse.body?.get("token")
        if (token is String) {
            return token
        }
        throw BadRequestException("Virhe sarakesign kirjautumisessa")
    }

    private fun createRequest(
        title: String,
        recipients: List<SarakeSignRecipientDTO>,
        token: String?,
        yliopisto: YliopistoEnum
    ): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        token?.let { headers.setBearerAuth(it) }

        val sarakeSignRequestDTO = SarakeSignRequestDTO(
            title = title,
            sendMail = true,
            recipients = recipients
        )

        val request = HttpEntity(sarakeSignRequestDTO, headers)
        val requestUrl = getApiUrl(yliopisto) + "request/" + getRequestTemplateId(yliopisto)

        val requestResponse: ResponseEntity<SarakeSignResponseDTO> = restTemplate.postForEntity(
            requestUrl, request,
            SarakeSignResponseDTO::class.java
        )

        return requestResponse.body?.request?.id
            ?: throw BadRequestException("Virhe sarakesign pyynnön luonnissa")
    }

    private fun getRequestStatus(
        requestId: String?,
        token: String?,
        yliopisto: YliopistoEnum
    ): SarakeSignResponseRequestDTO {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        token?.let { headers.setBearerAuth(it) }

        val request = HttpEntity(null, headers)
        val requestUrl = getApiUrl(yliopisto) + "request/" + requestId

        val requestResponse = restTemplate.exchange(
            requestUrl,
            HttpMethod.GET,
            request,
            SarakeSignResponseDTO::class.java
        )

        return requestResponse.body?.request
            ?: throw BadRequestException("Virhe saragesign pyynnön tilan haussa")
    }

    private fun uploadDocument(
        title: String,
        asiakirjaId: Long,
        requestId: String?,
        token: String?,
        yliopisto: YliopistoEnum
    ) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        token?.let { headers.setBearerAuth(it) }

        val asiakirja = asiakirjaRepository.findById(asiakirjaId)
            .orElseThrow { EntityNotFoundException("Asiakirjaa ei löydy") }

        val map: MultiValueMap<String, Any> = LinkedMultiValueMap()

        val file = File("/tmp/" + asiakirja.id.toString() + "_" + asiakirja.nimi)
        val data = asiakirja.asiakirjaData?.data!!
        Files.write(file.toPath(), data.getBytes(1, data.length().toInt()))

        val filesystemResource = FileSystemResource(file)
        map.add("file", filesystemResource)
        map.add(
            "data", SarakeSignDocumentMetadataDTO(
                title = title,
                signatureMode = 2
            )
        )

        val documentRequest: HttpEntity<MultiValueMap<String, Any>> =
            HttpEntity<MultiValueMap<String, Any>>(map, headers)

        val documentUrl = getApiUrl(yliopisto) + "document/" + requestId
        restTemplate.postForEntity(
            String.format(documentUrl, requestId),
            documentRequest,
            String::class.java
        )

        file.delete()
    }

    private fun sendRequest(requestId: String?, token: String?, yliopisto: YliopistoEnum) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        token?.let { headers.setBearerAuth(it) }

        val sendUrl = getApiUrl(yliopisto) + "request/" + requestId + "/send"
        restTemplate.exchange(
            String.format(sendUrl, requestId),
            HttpMethod.PUT,
            HttpEntity(null, headers),
            String::class.java
        )
    }

    override fun getApiUrl(yliopisto: YliopistoEnum): String? {
        when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getOulu().apiUrl
            }
            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getHki().apiUrl
            }
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getTre().apiUrl
            }
            YliopistoEnum.TURUN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getTurku().apiUrl
            }
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getUef().apiUrl
            }
        }
    }

    private fun getApiKey(yliopisto: YliopistoEnum): String? {
        when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getOulu().apiKey
            }
            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getHki().apiKey
            }
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getTre().apiKey
            }
            YliopistoEnum.TURUN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getTurku().apiKey
            }
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getUef().apiKey
            }
        }
    }

    private fun getRequestTemplateId(yliopisto: YliopistoEnum): String? {
        when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getOulu().requestTemplateId
            }
            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getHki().requestTemplateId
            }
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getTre().requestTemplateId
            }
            YliopistoEnum.TURUN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getTurku().requestTemplateId
            }
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> {
                return applicationProperties.getSarakesign().getUef().requestTemplateId
            }
        }
    }

    private fun lisaaVastaanottaja(user: User): SarakeSignRecipientDTO {
        return SarakeSignRecipientDTO(
            phaseNumber = 0,
            recipient = user.email,
            fields = SarakeSignRecipientFieldsDTO(
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = getPhoneNumber(user.phoneNumber)
            )
        )
    }

    private fun getPhoneNumber(number: String?): String? {
        if (number?.startsWith("0") == true) {
            return number.replaceFirst("0", "+358")
        }
        return number
    }
}
