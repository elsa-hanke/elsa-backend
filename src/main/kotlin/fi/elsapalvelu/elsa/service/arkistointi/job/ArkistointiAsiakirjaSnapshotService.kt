package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfA2bSource
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ArkistointiAsiakirjaSnapshotService(
    private val asiakirjaRepository: AsiakirjaRepository,
    private val pdfA2bService: PdfA2bService
) {

    @Transactional
    fun createReference(
        asiakirja: Asiakirja,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        documentData(asiakirja, jarjestysnumero)
        val stored = loadDocument(asiakirja.id, asiakirja.nimi ?: "tuntematon")
        return prepareReference(stored, asiakirjatyyppi, jarjestysnumero)
    }

    /** Build a single archive snapshot from persisted originals, not the ordinary merged PDF. */
    @Transactional
    fun createMergedReference(
        asiakirja: Asiakirja,
        sources: List<Asiakirja>,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        documentData(asiakirja, jarjestysnumero)
        val stored = loadDocument(asiakirja.id, requireNotNull(asiakirja.nimi))
        val normalizedData = try {
            pdfA2bService.normalizeAndMerge(sources.asSequence().map { source ->
                val original = loadDocument(source.id, source.nimi ?: "tuntematon")
                require(original.opintooikeus?.id == stored.opintooikeus?.id) { "Archive attachment belongs to another study right" }
                PdfA2bSource(documentData(original, jarjestysnumero), original.tyyppi)
            })
        } catch (exception: ArkistointiAsiakirjaException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(requireNotNull(stored.nimi)), exception)
        }
        return saveSnapshot(requireNotNull(stored.opintooikeus), stored.nimi, normalizedData, asiakirjatyyppi, jarjestysnumero)
    }

    /** Resolve caller-supplied references against persisted contents; a DTO or flag alone is not proof. */
    @Transactional
    fun resolveReference(reference: ArkistointiAsiakirjaReference): ArkistointiAsiakirjaReference {
        val stored = loadDocument(reference.asiakirja.id, reference.filename)
        val data = documentData(stored, reference.jarjestysnumero)
        val checksum = ArkistointiChecksum.sha256(data)
        if (!checksum.equals(reference.sha256, ignoreCase = true)) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch(reference.filename))
        }
        if (reference.filename != stored.nimi || reference.contentType != stored.tyyppi) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(reference.filename))
        }
        val proof = reference.validation as? ValidatedSnapshot
        if (proof != null) {
            val id = requireNotNull(stored.id)
            if (proof != ValidatedSnapshot(id, checksum, reference.filename, reference.contentType) ||
                asiakirjaRepository.findArchiveSnapshotFlagById(id) != true) {
                throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(reference.filename))
            }
            return verifiedReference(stored, reference.asiakirjatyyppi, reference.jarjestysnumero, checksum)
        }
        return prepareReference(stored, reference.asiakirjatyyppi, reference.jarjestysnumero)
    }

    private fun loadDocument(id: Long?, identifier: String): Asiakirja =
        id?.let(asiakirjaRepository::findOneWithDataById)
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))

    private fun documentData(asiakirja: Asiakirja, jarjestysnumero: Int): ByteArray {
        val identifier = asiakirja.nimi ?: asiakirja.id?.toString() ?: "tuntematon"
        if (jarjestysnumero < 0 || asiakirja.nimi.isNullOrBlank()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(identifier))
        }
        val data = asiakirja.asiakirjaData?.data
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        if (data.isEmpty()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.empty(identifier))
        }
        if (asiakirja.id == null) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        }
        return data
    }

    private fun prepareReference(
        asiakirja: Asiakirja,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val data = documentData(asiakirja, jarjestysnumero)
        val identifier = requireNotNull(asiakirja.nimi)
        // Ordinary merged documents need not be PDF/A. Normalize only their archival copy.
        if (asiakirjaRepository.findArchiveSnapshotFlagById(requireNotNull(asiakirja.id)) != true) {
            return createSnapshot(
                requireNotNull(asiakirja.opintooikeus), asiakirja.nimi, asiakirja.tyyppi,
                data, asiakirjatyyppi, jarjestysnumero
            )
        }

        if (asiakirja.tyyppi != MediaType.APPLICATION_PDF_VALUE || !pdfA2bService.isPdfA2b(data)) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.notPdfA2b(identifier))
        }

        return verifiedReference(asiakirja, asiakirjatyyppi, jarjestysnumero, ArkistointiChecksum.sha256(data))
    }

    @Transactional
    fun createSnapshot(
        opintooikeus: Opintooikeus,
        filename: String?,
        contentType: String?,
        data: ByteArray?,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val identifier = filename ?: asiakirjatyyppi.name
        if (jarjestysnumero < 0) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(identifier))
        }
        val normalizedData = try {
            pdfA2bService.normalize(data, contentType)
        } catch (exception: RuntimeException) {
            throw ArkistointiAsiakirjaException(
                if (data == null) {
                    ArkistointiAsiakirjaErrors.missing(identifier)
                } else if (data.isEmpty()) {
                    ArkistointiAsiakirjaErrors.empty(identifier)
                } else {
                    ArkistointiAsiakirjaErrors.invalid(identifier)
                },
                exception
            )
        }
        return saveSnapshot(opintooikeus, filename, normalizedData, asiakirjatyyppi, jarjestysnumero)
    }

    /** Only bytes validated by normalization/assembly reach this private persistence boundary. */
    private fun saveSnapshot(
        opintooikeus: Opintooikeus,
        filename: String?,
        normalizedData: ByteArray,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val snapshot = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = opintooikeus,
                nimi = pdfFilename(filename, asiakirjatyyppi),
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = normalizedData),
                archiveSnapshot = true
            )
        )
        // normalize validates its final bytes (including fallback). Bind proof to those exact bytes.
        // No subsequent PDF/A parse is needed; consumers must still check the persisted checksum.
        return verifiedReference(snapshot, asiakirjatyyppi, jarjestysnumero, ArkistointiChecksum.sha256(normalizedData))
    }

    private fun verifiedReference(
        snapshot: Asiakirja,
        type: RecordType,
        order: Int,
        checksum: String
    ): ArkistointiAsiakirjaReference = ArkistointiAsiakirjaReference(
        snapshot, type, order, requireNotNull(snapshot.nimi), requireNotNull(snapshot.tyyppi), checksum
    ).also {
        it.validation = ValidatedSnapshot(requireNotNull(snapshot.id), checksum, it.filename, it.contentType)
    }

    /** Only this service can construct a proof. No bytes are retained, just their immutable digest. */
    private data class ValidatedSnapshot(
        val id: Long,
        val checksum: String,
        val filename: String,
        val contentType: String
    ) : ArkistointiSnapshotValidation

    @Transactional
    fun createLaillistamistodistusReference(
        opintooikeus: Opintooikeus,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val erikoistuvaLaakari = opintooikeus.erikoistuvaLaakari
            ?: throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.missing(RecordType.LAILLISTAMISTODISTUS.name)
            )
        return createSnapshot(
            opintooikeus = opintooikeus,
            filename = erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi,
            contentType = erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi,
            data = erikoistuvaLaakari.laillistamistodistus?.data,
            asiakirjatyyppi = RecordType.LAILLISTAMISTODISTUS,
            jarjestysnumero = jarjestysnumero
        )
    }

    private fun pdfFilename(filename: String?, asiakirjatyyppi: RecordType): String {
        val basename = filename?.substringBeforeLast('.', filename)?.takeIf { it.isNotBlank() }
            ?: asiakirjatyyppi.name.lowercase()
        return "${basename.take(MAX_FILENAME_LENGTH - PDF_EXTENSION.length)}$PDF_EXTENSION"
    }

    private companion object {
        const val MAX_FILENAME_LENGTH = 255
        const val PDF_EXTENSION = ".pdf"
    }
}
