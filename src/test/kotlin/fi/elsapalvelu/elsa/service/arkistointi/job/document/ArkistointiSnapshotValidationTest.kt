package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreatorImpl
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArkistointiSnapshotValidationTest {
    private val documents = mock<AsiakirjaRepository>()
    private val jobs = mock<ArkistointiJobRepository>()
    private val scheduler = mock<ArkistointiJobScheduler>()
    private val snapshots = ArkistointiAsiakirjaSnapshotService(documents)
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
        whenever(jobs.saveAndFlush(any<ArkistointiJob>())).thenAnswer { it.getArgument<ArkistointiJob>(0).apply { id = UUID.randomUUID() } }
        whenever(scheduler.schedule(any(), any())).thenReturn(true)
    }

    @Test
    fun `copied reference is resolved against persisted immutable bytes`() {
        val reference = snapshot()
        val job = creator.create(request(reference.copy()))
        val attached = job.asiakirjat.single()
        assertThat(attached.asiakirja).isSameAs(reference.asiakirja)
        assertThat(attached.filename).isEqualTo("document.jpg")
        assertThat(attached.contentType).isEqualTo("image/jpeg")
        assertThat(attached.sha256).isEqualTo(reference.sha256)
    }

    @ParameterizedTest
    @ValueSource(strings = ["bytes", "id", "checksum", "filename", "contentType"])
    fun `changing contents identity or metadata cannot bypass persisted integrity checks`(change: String) {
        val reference = snapshot()
        val altered = when (change) {
            "bytes" -> reference.also { it.asiakirja.asiakirjaData!!.data!![0] = 0 }
            "id" -> reference.copy(asiakirja = reference.asiakirja.copy(id = 999L))
            "checksum" -> reference.copy(sha256 = "a".repeat(64))
            "filename" -> reference.copy(filename = "changed.pdf")
            else -> reference.copy(contentType = "application/pdf")
        }
        assertThrows<ArkistointiAsiakirjaException> { creator.create(request(altered)) }
        verify(jobs, never()).saveAndFlush(any())
        verify(scheduler, never()).schedule(any(), any())
    }

    @Test
    fun `mutable source is copied and job keeps its original metadata and checksum`() {
        val bytes = ArchiveImageTestData.image()
        val source = Asiakirja(id = 8L, nimi = "original.jpg", tyyppi = "image/jpeg", opintooikeus = Opintooikeus(id = 1L),
            asiakirjaData = AsiakirjaData(data = bytes), archiveSnapshot = true)
        stored[8L] = source
        persistedFlags[8L] = false
        val reference = ArkistointiAsiakirjaReference(source, RecordType.LIITE, 0, source.nimi!!, source.tyyppi!!,
            ArkistointiChecksum.sha256(bytes))

        val attached = creator.create(request(reference)).asiakirjat.single()

        assertThat(attached.asiakirja!!.id).isNotEqualTo(8L)
        assertThat(attached.filename).isEqualTo("original.jpg")
        assertThat(attached.contentType).isEqualTo("image/jpeg")
        assertThat(attached.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes))
        assertThat(attached.asiakirja!!.asiakirjaData!!.data).containsExactly(*bytes).isNotSameAs(bytes)
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
