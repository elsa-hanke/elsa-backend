package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType

class ArkistointiSnapshotValidationTest {
    private val documents = mock<AsiakirjaRepository>()
    private val jobs = mock<ArkistointiJobRepository>()
    private val scheduler = mock<ArkistointiJobScheduler>()
    private val pdfA = spy(PdfA2bService(ClassPathResource("sRGB_CS_profile.icm")))
    private val snapshots = ArkistointiAsiakirjaSnapshotService(documents, pdfA)
    private val creator = ArkistointiJobCreatorImpl(jobs, scheduler, snapshots)
    private val stored = mutableMapOf<Long, Asiakirja>()
    private val persistedFlags = mutableMapOf<Long, Boolean>()

    @BeforeEach
    fun repository() {
        whenever(documents.save(any<Asiakirja>())).thenAnswer {
            it.getArgument<Asiakirja>(0).apply {
                id = (100 + stored.size).toLong()
                stored[id!!] = this
                persistedFlags[id!!] = archiveSnapshot
            }
        }
        whenever(documents.findOneWithDataById(any())).thenAnswer { stored[it.getArgument<Long>(0)] }
        whenever(documents.findArchiveSnapshotFlagById(any())).thenAnswer { persistedFlags[it.getArgument<Long>(0)] }
        whenever(jobs.saveAndFlush(any<ArkistointiJob>())).thenAnswer { it.getArgument<ArkistointiJob>(0).apply { id = 42L } }
        whenever(scheduler.schedule(any(), any())).thenReturn(true)
    }

    @ParameterizedTest
    @ValueSource(strings = ["pdf", "pdfa", "jpeg", "png"])
    fun `accepted final bytes are validated once across normalization snapshot and creator`(kind: String) {
        val input = when (kind) {
            "pdf" -> PdfTestData.unembeddedFontPdf(1)
            "pdfa" -> pdfA.normalize(ArchiveImageTestData.image(), "image/jpeg")
            else -> ArchiveImageTestData.image(kind)
        }
        val type = if (kind.startsWith("pdf")) "application/pdf" else "image/$kind"
        clearInvocations(pdfA)
        val reference = snapshots.createSnapshot(Opintooikeus(id = 1L), "document.$kind", type, input, RecordType.LIITE, 0)
        val finalBytes = reference.asiakirja.asiakirjaData!!.data!!.copyOf()
        val beforeCreator = org.mockito.Mockito.mockingDetails(pdfA).invocations.size

        val job = creator.create(request(reference))

        assertThat(org.mockito.Mockito.mockingDetails(pdfA).invocations).hasSize(beforeCreator)
        verify(pdfA, times(1)).isPdfA2b(argThat { contentEquals(finalBytes) })
        assertThat(job.asiakirjat.single().sha256).isEqualTo(ArkistointiChecksum.sha256(finalBytes))
        if (kind == "pdfa") assertThat(finalBytes).containsExactly(*input)
    }

    @Test
    fun `copy loses proof and existing snapshot is fully validated once`() {
        val reference = snapshot()
        val copy = reference.copy()
        assertThat(copy.validation).isNull()
        clearInvocations(pdfA)
        creator.create(request(copy))
        verify(pdfA, times(1)).isPdfA2b(any())
    }

    @ParameterizedTest
    @ValueSource(strings = ["bytes", "id", "checksum", "metadata", "borrowed-proof"])
    fun `changing verified contents or identity cannot bypass integrity checks`(change: String) {
        val reference = snapshot()
        val altered = when (change) {
            "bytes" -> reference.also { it.asiakirja.asiakirjaData!!.data!![0] = 0 }
            "id" -> reference.also { it.asiakirja.id = 999L }
            "checksum" -> reference.copy(sha256 = "a".repeat(64))
            "metadata" -> reference.copy(filename = "changed.pdf")
            else -> {
                val other = snapshot()
                other.copy().also { it.validation = reference.validation }
            }
        }
        assertThrows<ArkistointiAsiakirjaException> { creator.create(request(altered)) }
        verify(jobs, never()).saveAndFlush(any())
        verify(scheduler, never()).schedule(any(), any())
    }

    @Test
    fun `rewriting checksum after mutating validated bytes cannot renew the proof`() {
        val reference = snapshot()
        reference.asiakirja.asiakirjaData!!.data!![0] = 0
        val altered = reference.copy(sha256 = ArkistointiChecksum.sha256(reference.asiakirja.asiakirjaData!!.data!!))
            .also { it.validation = reference.validation }
        assertThrows<ArkistointiAsiakirjaException> { creator.create(request(altered)) }
        verify(jobs, never()).saveAndFlush(any())
    }

    @Test
    fun `mutable source is normalized and job uses the resulting snapshot metadata and hash`() {
        val bytes = ArchiveImageTestData.image()
        val source = Asiakirja(id = 8L, nimi = "original.jpg", tyyppi = "image/jpeg", opintooikeus = Opintooikeus(id = 1L),
            asiakirjaData = AsiakirjaData(data = bytes), archiveSnapshot = true)
        stored[8L] = source
        persistedFlags[8L] = false // Caller cannot turn a mutable persisted document into a snapshot by changing this flag.
        val reference = ArkistointiAsiakirjaReference(source, RecordType.LIITE, 0, source.nimi!!, source.tyyppi!!,
            ArkistointiChecksum.sha256(bytes))
        val job = creator.create(request(reference))
        val attached = job.asiakirjat.single()
        assertThat(attached.asiakirja!!.id).isNotEqualTo(8L)
        assertThat(attached.filename).isEqualTo("original.pdf")
        assertThat(attached.contentType).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(attached.sha256).isEqualTo(ArkistointiChecksum.sha256(attached.asiakirja!!.asiakirjaData!!.data!!))
        assertThat(source.asiakirjaData!!.data).containsExactly(*bytes)
    }

    @Test
    fun `fabricated entity contents cannot replace authoritative persisted bytes`() {
        val reference = snapshot()
        val fake = reference.asiakirja.copy(asiakirjaData = AsiakirjaData(data = byteArrayOf(1, 2, 3)))
        val supplied = reference.copy(asiakirja = fake, sha256 = ArkistointiChecksum.sha256(fake.asiakirjaData!!.data!!))
        assertThrows<ArkistointiAsiakirjaException> { creator.create(request(supplied)) }
        verify(jobs, never()).saveAndFlush(any())
    }

    private fun snapshot() = snapshots.createSnapshot(Opintooikeus(id = 1L), "document.jpg", "image/jpeg",
        ArchiveImageTestData.image(), RecordType.LIITE, 0)

    private fun request(reference: ArkistointiAsiakirjaReference) = CreateArkistointiJobRequest(
        YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", "validation", listOf(reference)
    )
}
