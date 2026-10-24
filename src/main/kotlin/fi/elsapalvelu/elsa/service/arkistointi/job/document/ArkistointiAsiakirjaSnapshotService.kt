package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArkistointiAsiakirjaSnapshotService(
    private val asiakirjaRepository: AsiakirjaRepository
) {

    @Transactional
    fun createReference(
        asiakirja: Asiakirja,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        documentData(asiakirja, jarjestysnumero)
        val stored = loadDocument(asiakirja.id)
        return prepareReference(stored, asiakirjatyyppi, jarjestysnumero)
    }

    /** Resolve repository state and the persisted snapshot flag; the entity may already be managed in this transaction. */
    @Transactional
    fun resolveReference(reference: ArkistointiAsiakirjaReference): ArkistointiAsiakirjaReference {
        val stored = loadDocument(reference.asiakirja.id)
        val data = documentData(stored, reference.jarjestysnumero)
        val checksum = ArkistointiChecksum.sha256(data)
        if (!checksum.equals(reference.sha256, ignoreCase = true)) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch())
        }
        if (reference.filename != stored.nimi || reference.contentType != stored.tyyppi) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid())
        }
        return prepareReference(stored, reference.asiakirjatyyppi, reference.jarjestysnumero, checksum)
    }

    private fun loadDocument(id: Long?): Asiakirja =
        id?.let(asiakirjaRepository::findOneWithDataById)
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing())

    private fun documentData(asiakirja: Asiakirja, jarjestysnumero: Int): ByteArray {
        if (jarjestysnumero < 0 || asiakirja.nimi.isNullOrBlank() || asiakirja.tyyppi.isNullOrBlank()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid())
        }
        val data = asiakirja.asiakirjaData?.data
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing())
        if (data.isEmpty()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.empty())
        }
        if (asiakirja.id == null) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing())
        }
        return data
    }

    private fun prepareReference(
        asiakirja: Asiakirja,
        asiakirjatyyppi: RecordType,
        jarjestysnumero: Int,
        verifiedChecksum: String? = null
    ): ArkistointiAsiakirjaReference {
        val data = documentData(asiakirja, jarjestysnumero)
        // Snapshot the existing output without introducing another conversion or format policy.
        if (asiakirjaRepository.findArchiveSnapshotFlagById(requireNotNull(asiakirja.id)) != true) {
            return createSnapshot(
                requireNotNull(asiakirja.opintooikeus), asiakirja.nimi, asiakirja.tyyppi,
                data, asiakirjatyyppi, jarjestysnumero
            )
        }

        // Only resolveReference supplies this private argument, after hashing and validating the loaded bytes.
        return verifiedReference(asiakirja, asiakirjatyyppi, jarjestysnumero, verifiedChecksum ?: ArkistointiChecksum.sha256(data))
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
        if (jarjestysnumero < 0) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid())
        }
        if (data == null) throw inputError(ArkistointiAsiakirjaErrors.missing())
        if (data.isEmpty()) throw inputError(ArkistointiAsiakirjaErrors.empty())
        if (filename.isNullOrBlank() || filename.length > MAX_METADATA_LENGTH ||
            contentType.isNullOrBlank() || contentType.length > MAX_METADATA_LENGTH) {
            throw inputError(ArkistointiAsiakirjaErrors.invalid())
        }
        // Own the snapshot bytes: subsequent changes to the caller's buffer must not change this document.
        val snapshotData = data.copyOf()
        val snapshot = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = opintooikeus,
                nimi = filename,
                tyyppi = contentType,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = snapshotData),
                archiveSnapshot = true
            )
        )
        return verifiedReference(snapshot, asiakirjatyyppi, jarjestysnumero, ArkistointiChecksum.sha256(snapshotData))
    }

    private fun verifiedReference(
        snapshot: Asiakirja,
        type: RecordType,
        order: Int,
        checksum: String
    ): ArkistointiAsiakirjaReference = ArkistointiAsiakirjaReference(
        snapshot, type, order, requireNotNull(snapshot.nimi), requireNotNull(snapshot.tyyppi), checksum
    )

    @Transactional
    fun createLaillistamistodistusReference(
        opintooikeus: Opintooikeus,
        jarjestysnumero: Int
    ): ArkistointiAsiakirjaReference {
        val erikoistuvaLaakari = opintooikeus.erikoistuvaLaakari
            ?: throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.missing()
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

    private fun inputError(error: ArkistointiError) =
        ArkistointiAsiakirjaException(error, ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT)

    private companion object {
        const val MAX_METADATA_LENGTH = 255
    }
}
