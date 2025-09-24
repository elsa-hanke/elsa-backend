package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiMetadata
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.Record
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.ResourceBundle
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class ArkistointiServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val tampereLouhiService: TampereLouhiService
) : ArkistointiService {
    private val log = LoggerFactory.getLogger(ArkistointiServiceImpl::class.java)

    override fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<RecordProperties>,
        case: CaseProperties,
        tarkastaja: String?,
        tarkastusPaiva: LocalDate?,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?
    ): String {
        val name = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        val title = "${case.type} $name"
        var metadata = ArkistointiMetadata()

        val resourceBundle = ResourceBundle.getBundle("i18n/messages")

        var transferInfomation = metadata.transferInformation
        var year = LocalDate.now().year
        transferInfomation.nativeId = "urn:oid:1.2.246.582.200.${opintooikeus?.id}$year.$year.0001"
        transferInfomation.transferContractId = opintooikeus?.id?.toString()

        var contactInformation = metadata.contactInformation
        contactInformation.organisation = "TAU"

        var arkistointiProperties = applicationProperties.getArkistointi().getTre()

        var contactPerson = contactInformation.contactPerson
        contactPerson.name = arkistointiProperties.contactPerson
        contactPerson.address = arkistointiProperties.contactAddress
        contactPerson.phoneNumber = arkistointiProperties.contactPhone
        contactPerson.email = arkistointiProperties.contactEmail

        var caseFile = metadata.caseFile
        caseFile.created = LocalDate.now()
        caseFile.nativeId = case.nativeId
        caseFile.title = title
        caseFile.function = case.function

        caseFile.restriction.person.name = name
        caseFile.restriction.person.ssn = opintooikeus?.erikoistuvaLaakari?.syntymaaika

        var action = caseFile.action
        action.created = LocalDate.now()
        action.title = title
        action.type = case.type

        asiakirjat.forEach {
            val asiakirja = it.asiakirja
            val record = Record()
            record.created = LocalDate.now()
            record.nativeId = asiakirja.id?.toString()
            record.title = asiakirja.nimi
            record.type = it.type
            record.retentionPeriod = it.retentionPeriod
            record.function = case.function

            record.restriction.publicityClass = it.publicityClass.displayName
            record.restriction.securityReason = it.publicityClass.securityReason
            record.restriction.person.name = name
            record.restriction.person.ssn = opintooikeus?.erikoistuvaLaakari?.syntymaaika

            val custom = record.custom
            custom.erikoistujanNimi = name
            custom.erikoisala = opintooikeus?.erikoisala?.nimi
            custom.opiskelijanumero = opintooikeus?.opiskelijatunnus
            custom.syntymaaika = opintooikeus?.erikoistuvaLaakari?.syntymaaika
            custom.yliopisto = resourceBundle.getString(opintooikeus?.yliopisto?.nimi?.toString()!!)
            custom.tarkastaja = tarkastaja
            custom.tarkastuspaiva = tarkastusPaiva
            custom.hyvaksyja = hyvaksyja
            custom.hyvaksymispaiva = hyvaksymisPaiva

            val document = record.document
            document.nativeId = asiakirja.nimi
            document.file.name = asiakirja.nimi
            document.file.path = "pdf/${asiakirja.nimi}"

            document.format.name = asiakirja.tyyppi
            document.hashValue = DigestUtils.sha256Hex(asiakirja.asiakirjaData?.data)
            action.record.add(record)
        }

        var mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.dateFormat = SimpleDateFormat("dd.MM.yyyy")
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)

        val filePath = "/tmp/$title.zip"
        var fos = FileOutputStream(filePath)
        var zipOut = ZipOutputStream(fos)

        zipOut.putNextEntry(ZipEntry("sahke.xml"))
        zipOut.write(mapper.writeValueAsBytes(metadata))

        asiakirjat.forEach {
            val asiakirja = it.asiakirja
            var zipEntry = ZipEntry("pdf/${asiakirja.nimi}")
            zipOut.putNextEntry(zipEntry)
            zipOut.write(asiakirja.asiakirjaData?.data)
        }

        zipOut.close()
        fos.close()

        return filePath
    }

    override fun laheta(yliopisto: YliopistoEnum, filePath: String, yek: Boolean) {
        when (yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                tampereLouhiService.laheta(filePath, yek)
            }

            else -> log.info("Integraatiota arkistointiin ei ole tuettu yliopistossa ${yliopisto.name}")
        }
    }

    override fun onKaytossa(yliopisto: YliopistoEnum): Boolean {
        return when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> {
                applicationProperties.getArkistointi().getOulu().kaytossa
            }

            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                applicationProperties.getArkistointi().getHki().kaytossa
            }

            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                applicationProperties.getArkistointi().getTre().kaytossa
            }

            YliopistoEnum.TURUN_YLIOPISTO -> {
                applicationProperties.getArkistointi().getTurku().kaytossa
            }

            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> {
                applicationProperties.getArkistointi().getUef().kaytossa
            }
        }
    }
}
