package fi.elsapalvelu.elsa.service.arkistointi.sahke

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Component
class SahkePakettiBuilder {
    fun build(
        metadataResult: SahkeMetadataResult,
        opintooikeus: Opintooikeus?,
        zipMetadata: Boolean
    ): ArkistointiPackage {
        val metadata = metadataResult.metadata
        val user = opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
        val filePath = "/tmp/${filename(metadata.caseFile.action.type, user?.firstName, user?.lastName)}"

        return build(metadataResult, Path.of(filePath), zipMetadata)
    }

    fun filename(type: String?, firstName: String?, lastName: String?): String =
        "${type?.lowercase()}_${firstName?.lowercase()}_${lastName?.lowercase()}.zip"

    /** The caller owns a unique temporary directory and removes it on both success and failure. */
    fun build(
        metadataResult: SahkeMetadataResult,
        destination: Path,
        zipMetadata: Boolean
    ): ArkistointiPackage {
        val mapper = createXmlMapper()
        val metadata = metadataResult.metadata
        val filePath = destination.toString()

        ZipOutputStream(FileOutputStream(filePath)).use { zipOut ->
            if (zipMetadata) {
                zipOut.putNextEntry(ZipEntry("sahke.xml"))
                zipOut.write(mapper.writeValueAsBytes(metadata))
            }

            metadataResult.asiakirjat.forEach {
                val asiakirja = it.asiakirja
                zipOut.putNextEntry(ZipEntry("pdf/${asiakirja.nimi}"))
                zipOut.write(asiakirja.asiakirjaData?.data)
            }
        }

        val metadataBytes = if (zipMetadata) null else mapper.writeValueAsBytes(metadata)
        return ArkistointiPackage(filePath, metadataBytes)
    }

    private fun createXmlMapper(): XmlMapper = XmlMapper().apply {
        registerModule(JavaTimeModule())
        dateFormat = SimpleDateFormat("dd.MM.yyyy")
        configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
    }
}
