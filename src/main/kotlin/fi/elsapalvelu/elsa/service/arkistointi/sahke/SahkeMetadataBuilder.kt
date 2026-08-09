package fi.elsapalvelu.elsa.service.arkistointi.sahke

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiMetadata
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseFile
import fi.elsapalvelu.elsa.service.dto.arkistointi.ContactInformation
import fi.elsapalvelu.elsa.service.dto.arkistointi.PublicityClass
import fi.elsapalvelu.elsa.service.dto.arkistointi.Record
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.arkistointi.TransferInformation
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.ResourceBundle

data class SahkeMetadataRequest(
    val opintooikeus: Opintooikeus?,
    val asiakirjat: List<RecordProperties>,
    val caseId: String?,
    val tarkastaja: String?,
    val tarkastusPaiva: LocalDate?,
    val hyvaksyja: String?,
    val hyvaksymisPaiva: LocalDate?
)

data class SahkeMetadataResult(
    val metadata: ArkistointiMetadata,
    val asiakirjat: List<RecordProperties>
)

@Component
class SahkeMetadataBuilder {
    fun build(
        request: SahkeMetadataRequest,
        configuration: ArkistointiConfiguration
    ): SahkeMetadataResult {
        val metadata = ArkistointiMetadata()
        val opintooikeus = request.opintooikeus
        val name = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        val syntymaaika = opintooikeus?.erikoistuvaLaakari?.syntymaaika

        buildTransferInformation(metadata.transferInformation, configuration, opintooikeus?.id)
        buildContactInformation(metadata.contactInformation, configuration)
        buildCaseFile(metadata.caseFile, request.caseId, name, syntymaaika, configuration)

        val configuredAsiakirjat = request.asiakirjat.filter { recordProperties ->
            configuration.metadata.getDocumentMetadata(recordProperties.type, configuration.case) != null
        }

        buildRecords(configuredAsiakirjat, metadata, configuration, request)
        return SahkeMetadataResult(metadata, configuredAsiakirjat)
    }

    private fun buildTransferInformation(
        transferInformation: TransferInformation,
        configuration: ArkistointiConfiguration,
        opintooikeusId: Long?
    ) {
        val year = LocalDate.now().year
        transferInformation.nativeId = "urn:oid:1.2.246.582.200.${opintooikeusId}$year.$year.0001"
        transferInformation.transferContractId = opintooikeusId?.toString()
        configuration.metadata.useType?.let { transferInformation.useType = it }
    }

    private fun buildContactInformation(
        contactInformation: ContactInformation,
        configuration: ArkistointiConfiguration
    ) {
        val metadata = configuration.metadata
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
        configuration: ArkistointiConfiguration
    ) {
        val metadata = configuration.metadata
        val case = configuration.case
        caseFile.created = LocalDate.now()
        caseFile.nativeId = nativeId
        caseFile.title = case.title
        caseFile.type = case.type
        caseFile.function = case.function

        caseFile.restriction.person.name = name
        caseFile.restriction.person.ssn = syntymaaika
        caseFile.restriction.owner = metadata.organisation
        caseFile.retentionReason = metadata.retentionReason
        caseFile.retentionPeriod = metadata.retentionPeriod

        val action = caseFile.action
        action.created = LocalDate.now()
        action.title = case.title
        action.type = case.type
    }

    private fun buildRecords(
        asiakirjat: List<RecordProperties>,
        metadata: ArkistointiMetadata,
        configuration: ArkistointiConfiguration,
        request: SahkeMetadataRequest
    ) {
        val opintooikeus = request.opintooikeus
        val name = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.getName()
        val resourceBundle = ResourceBundle.getBundle("i18n/messages")

        asiakirjat.forEach { recordProperties ->
            val documentMetadata = configuration.metadata
                .getDocumentMetadata(recordProperties.type, configuration.case)
                .required()
            val asiakirja = recordProperties.asiakirja
            val record = Record()
            record.created = LocalDate.now()
            record.nativeId = asiakirja.id?.toString()

            val user = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
            record.title = "${user?.lastName}, ${user?.firstName}, ${opintooikeus?.erikoisala?.nimi}"
            record.type = configuration.case.type
            record.retentionPeriod = documentMetadata.retentionPeriod
            record.function = metadata.caseFile.function

            val publicityClass = getPublicityClass(recordProperties.type)
            record.restriction.publicityClass = publicityClass.displayName
            record.restriction.securityReason = publicityClass.securityReason
            record.restriction.person.name = name
            record.restriction.person.ssn = opintooikeus?.erikoistuvaLaakari?.syntymaaika
            record.restriction.owner = configuration.metadata.organisation

            buildCustomMetadata(record, request, configuration, name, resourceBundle)

            val document = record.document
            document.nativeId = asiakirja.nimi
            document.file.name = asiakirja.nimi
            document.file.path = "pdf/${asiakirja.nimi}"
            document.format.name = asiakirja.tyyppi
            document.hashValue = DigestUtils.sha256Hex(asiakirja.asiakirjaData?.data)
            metadata.caseFile.action.record.add(record)
        }
    }

    private fun buildCustomMetadata(
        record: Record,
        request: SahkeMetadataRequest,
        configuration: ArkistointiConfiguration,
        name: String?,
        resourceBundle: ResourceBundle
    ) {
        val opintooikeus = request.opintooikeus
        val custom = record.custom
        custom.erikoistujanNimi = name
        custom.erikoisala = opintooikeus?.erikoisala?.nimi
        custom.opiskelijanumero = opintooikeus?.opiskelijatunnus
        custom.syntymaaika = opintooikeus?.erikoistuvaLaakari?.syntymaaika
        opintooikeus?.yliopisto?.nimi?.toString()?.let { custom.yliopisto = resourceBundle.getString(it) }
        custom.tarkastaja = request.tarkastaja
        custom.tarkastuspaiva = request.tarkastusPaiva
        custom.hyvaksyja = request.hyvaksyja
        custom.hyvaksymispaiva = request.hyvaksymisPaiva
        custom.asiakirjatyyppi = configuration.case.title
    }

    private fun getPublicityClass(recordType: RecordType): PublicityClass = when (recordType) {
        RecordType.YHTEENVETO -> PublicityClass.PUBLIC
        RecordType.LIITE -> PublicityClass.PARTIALLY_RESTRICTED
        RecordType.ARVIOINTI -> PublicityClass.PUBLIC
        RecordType.SOPIMUS -> PublicityClass.PUBLIC
    }
}
