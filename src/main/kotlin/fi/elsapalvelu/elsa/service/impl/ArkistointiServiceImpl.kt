package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.*
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class ArkistointiServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val tampereLouhiService: TampereLouhiService,
    private val helsinkiSiiloService: HelsinkiSiiloService
) : ArkistointiService {
    private val log = LoggerFactory.getLogger(ArkistointiServiceImpl::class.java)

    override fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<RecordProperties>,
        caseId: String?,
        tarkastaja: String?,
        tarkastusPaiva: LocalDate?,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?,
        yliopisto: YliopistoEnum?,
        caseType: CaseType
    ): ArkistointiResult {
        val name = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        val syntymaaika = opintooikeus?.erikoistuvaLaakari?.syntymaaika
        //val title = "${case.type} $name"
        val metadata = ArkistointiMetadata()

        val metadataProperties = getMetadataForYliopisto(yliopisto!!)
        val case = metadataProperties?.getCaseMetadata(caseType) ?: throw IllegalArgumentException(
            "Arkistointia ${caseType.value} ei ole määritelty yliopistolle ${yliopisto.name}"
        )

        buildTransferInformation(metadata.transferInformation, metadataProperties, opintooikeus?.id)
        buildContactInformation(metadata.contactInformation, metadataProperties)
        buildCaseFile(metadata.caseFile, caseId, name, syntymaaika, metadataProperties, case)

        val configuredAsiakirjat = asiakirjat.filter { recordProperties ->
            metadataProperties.getDocumentMetadata(recordProperties.type, case) != null
        }

        buildRecords(
            configuredAsiakirjat,
            metadata,
            metadataProperties,
            opintooikeus,
            tarkastaja,
            tarkastusPaiva,
            hyvaksyja,
            hyvaksymisPaiva,
            case
        )

        val mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.dateFormat = SimpleDateFormat("dd.MM.yyyy")
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)

        val user = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
        val title = "${metadata.caseFile.action.type?.lowercase()}_${user?.firstName?.lowercase()}_${user?.lastName?.lowercase()}"
        val filePath = "/tmp/$title.zip"
        val fos = FileOutputStream(filePath)
        val zipOut = ZipOutputStream(fos)

        if (metadataProperties.zipMetadata) {
            zipOut.putNextEntry(ZipEntry("sahke.xml"))
            zipOut.write(mapper.writeValueAsBytes(metadata))
        }

        configuredAsiakirjat.forEach {
            val asiakirja = it.asiakirja
            val zipEntry = ZipEntry("pdf/${asiakirja.nimi}")
            zipOut.putNextEntry(zipEntry)
            zipOut.write(asiakirja.asiakirjaData?.data)
        }

        zipOut.close()
        fos.close()

        val metadataBytes = if (metadataProperties.zipMetadata) null else mapper.writeValueAsBytes(metadata)
        return ArkistointiResult(filePath, metadataBytes)
    }

    private fun getMetadataForYliopisto(yliopisto: YliopistoEnum): ApplicationProperties.Arkistointi.Metadata? {
        val arkistointi = applicationProperties.getArkistointi()
        return when (yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> arkistointi.getTre().metadata
            YliopistoEnum.HELSINGIN_YLIOPISTO -> arkistointi.getHki().metadata
            YliopistoEnum.OULUN_YLIOPISTO -> arkistointi.getOulu().metadata
            YliopistoEnum.TURUN_YLIOPISTO -> arkistointi.getTurku().metadata
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> arkistointi.getUef().metadata
        }
    }

    private fun buildTransferInformation(transferInformation: TransferInformation, metadata: ApplicationProperties.Arkistointi.Metadata, opintooikeusId: Long?) {
        val year = LocalDate.now().year
        transferInformation.nativeId = "urn:oid:1.2.246.582.200.${opintooikeusId}$year.$year.0001"
        transferInformation.transferContractId = opintooikeusId?.toString()
        metadata.useType?.let { transferInformation.useType = it }
    }

    private fun buildContactInformation(
        contactInformation: ContactInformation,
        metadata: ApplicationProperties.Arkistointi.Metadata
    ) {
        contactInformation.organisation.name = metadata.organisation

        val contactPerson = contactInformation.contactPerson
        val contact = metadata.contact
        contactPerson.name = contact?.person
        contactPerson.address = contact?.address
        contactPerson.phoneNumber = contact?.phone
        contactPerson.email = contact?.email
    }

    private fun buildCaseFile(
        caseFile: CaseFile,
        nativeId: String?,
        name: String?,
        syntymaaika: LocalDate?,
        metadata: ApplicationProperties.Arkistointi.Metadata,
        caseMetadata: ApplicationProperties.Arkistointi.Case?,
    ) {
        caseFile.created = LocalDate.now()
        caseFile.nativeId = nativeId
        caseFile.title = caseMetadata?.title
        caseFile.type = caseMetadata?.type
        caseFile.function = caseMetadata?.function

        caseFile.restriction.person.name = name
        caseFile.restriction.person.ssn = syntymaaika
        caseFile.restriction.owner = metadata.organisation
        caseFile.retentionReason = metadata.retentionReason
        caseFile.retentionPeriod = metadata.retentionPeriod

        val action = caseFile.action
        action.created = LocalDate.now()
        action.title = caseMetadata?.title
        action.type = caseMetadata?.type
    }

    private fun buildRecords(
        asiakirjat: List<RecordProperties>,
        metadata: ArkistointiMetadata,
        arkistointiProperties: ApplicationProperties.Arkistointi.Metadata,
        opintooikeus: Opintooikeus?,
        tarkastaja: String?,
        tarkastusPaiva: LocalDate?,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?,
        case: ApplicationProperties.Arkistointi.Case?
    ) {
        val name = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        val resourceBundle = ResourceBundle.getBundle("i18n/messages")

        asiakirjat.forEach { recordProperties ->
            val documentMetadata = arkistointiProperties.getDocumentMetadata(recordProperties.type, case)!!

            val asiakirja = recordProperties.asiakirja
            val record = Record()
            record.created = LocalDate.now()
            record.nativeId = asiakirja.id?.toString()

            val user = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
            record.title = "${user?.lastName}, ${user?.firstName}, ${opintooikeus?.erikoisala?.nimi}"
            record.type = case?.type
            record.retentionPeriod = documentMetadata.retentionPeriod
            record.function = metadata.caseFile.function

            val publicityClass = when (recordProperties.type) {
                RecordType.YHTEENVETO -> PublicityClass.PUBLIC
                RecordType.LIITE -> PublicityClass.PARTIALLY_RESTRICTED
                RecordType.ARVIOINTI -> PublicityClass.PUBLIC
                RecordType.SOPIMUS -> PublicityClass.PUBLIC
            }

            record.restriction.publicityClass = publicityClass.displayName
            record.restriction.securityReason = publicityClass.securityReason
            record.restriction.person.name = name
            record.restriction.person.ssn = opintooikeus?.erikoistuvaLaakari?.syntymaaika
            record.restriction.owner = arkistointiProperties.organisation

            val custom = record.custom
            custom.erikoistujanNimi = name
            custom.erikoisala = opintooikeus?.erikoisala?.nimi
            custom.opiskelijanumero = opintooikeus?.opiskelijatunnus
            custom.syntymaaika = opintooikeus?.erikoistuvaLaakari?.syntymaaika
            opintooikeus?.yliopisto?.nimi?.toString()?.let { custom.yliopisto = resourceBundle.getString(it) }
            custom.tarkastaja = tarkastaja
            custom.tarkastuspaiva = tarkastusPaiva
            custom.hyvaksyja = hyvaksyja
            custom.hyvaksymispaiva = hyvaksymisPaiva
            custom.asiakirjatyyppi = case?.title

            val document = record.document
            document.nativeId = asiakirja.nimi
            document.file.name = asiakirja.nimi
            document.file.path = "pdf/${asiakirja.nimi}"

            document.format.name = asiakirja.tyyppi
            document.hashValue = DigestUtils.sha256Hex(asiakirja.asiakirjaData?.data)
            metadata.caseFile.action.record.add(record)
        }
    }

    override fun laheta(
        yliopisto: YliopistoEnum,
        filePath: String,
        caseType: CaseType,
        yek: Boolean
    ) {
        when (yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> {
                tampereLouhiService.laheta(filePath, yek)
            }

            YliopistoEnum.HELSINGIN_YLIOPISTO -> {
                helsinkiSiiloService.laheta(filePath, caseType)
            }

            else -> log.info("Integraatiota arkistointiin ei ole tuettu yliopistossa ${yliopisto.name}")
        }
    }

    override fun onKaytossa(yliopisto: YliopistoEnum, caseType: CaseType): Boolean {
        val kaytossa = when (yliopisto) {
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

        if (!kaytossa) {
            return false
        }

        val metadataProperties = getMetadataForYliopisto(yliopisto)
        return metadataProperties?.getCaseMetadata(caseType) != null
    }
}
