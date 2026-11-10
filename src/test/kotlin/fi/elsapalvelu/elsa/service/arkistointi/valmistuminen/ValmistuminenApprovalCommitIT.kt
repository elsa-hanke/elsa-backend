package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.document.toDescriptor
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistumispyynnonViimeistelyService
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

/** What a successful async approval commits, and that repeating it changes nothing. */
class ValmistuminenApprovalCommitIT : ValmistuminenArchiveItBase() {
    @Autowired private lateinit var finalization: ValmistumispyynnonViimeistelyService
    @Autowired private lateinit var requests: ValmistumispyyntoRepository

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `approval commits immutable documents metadata and scheduler task together`(kind: String) {
        val fixture = fixture(kind, certificate = null, attachment = PdfTestData.cmykJpeg(), contentType = MediaType.IMAGE_JPEG_VALUE)
        approve(fixture)
        val key = key(fixture)
        val jobId = requireNotNull(transactions.execute { jobs.findByKey(key)?.id })
        val expectedTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE)
        val archivedBytes = mutableMapOf<Long, ByteArray>()
        val payload = transactions.execute {
            val job = jobs.findByKey(key)!!
            assertThat(em.find(Valmistumispyynto::class.java, fixture.id).vastuuhenkiloHyvaksyjaKuittausaika)
                .isEqualTo(LocalDate.now())
            assertThat(job.asiakirjat.map { asiakirja -> asiakirja.asiakirjatyyppi }).containsExactlyElementsOf(expectedTypes)
            assertThat(job.asiakirjat.map { asiakirja -> asiakirja.jarjestysnumero }).containsExactlyElementsOf(expectedTypes.indices.toList())
            archivedBytes.putAll(assertSnapshots(job))
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val stored = job.payload!!
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            assertQueuedDocumentsMatchSources(job, request)
            assertZipContents(job, request, expectedTypes, properties)
            assertThat(job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LIITE }.asiakirja!!.id)
                .isNotEqualTo(request.liitteetAsiakirja!!.id)
            assertGeneratedSummary(request)
            val metadata = mapper.readTree(stored)
            assertThat(metadata["caseId"].asLong()).isEqualTo(fixture.id)
            assertThat(metadata["opintooikeusId"].asLong()).isEqualTo(fixture.studyRightId)
            assertThat(metadata["university"].asText()).isEqualTo("TAMPEREEN_YLIOPISTO")
            assertThat(metadata["yek"].asBoolean()).isEqualTo(kind == "YEK")
            assertThat(metadata["firstName"].asText()).isEqualTo("john")
            assertThat(metadata["lastName"].asText()).isEqualTo("doe")
            assertThat(metadata["traineeName"].asText()).isEqualTo("john doe")
            assertThat(metadata["studentNumber"].asText()).isEqualTo("AAAAAAAAAA")
            assertThat(metadata["birthDate"].asText()).isEqualTo(LocalDate.ofEpochDay(5).toString())
            assertThat(metadata["reviewer"].asText()).isEqualTo("john doe")
            assertThat(metadata["approver"].asText()).isEqualTo("john doe")
            assertThat(metadata["reviewDate"].asText()).isEqualTo(LocalDate.now().toString())
            assertThat(metadata["approvalDate"].asText()).isEqualTo(LocalDate.now().toString())
            assertThat(metadata["specialtyId"].asLong()).isEqualTo(request.opintooikeus!!.erikoisala!!.id)
            assertThat(metadata["specialtyName"].asText()).isEqualTo(request.opintooikeus!!.erikoisala!!.nimi)
            // Change live sources after approval; neither queued metadata nor bytes may follow them.
            request.opintooikeus!!.erikoistuvaLaakari!!.kayttaja!!.user!!.firstName = "Changed"
            request.opintooikeus!!.opiskelijatunnus = "changed-number"
            request.yhteenvetoAsiakirja!!.asiakirjaData!!.data = "changed-source".toByteArray()
            request.liitteetAsiakirja!!.asiakirjaData!!.data = "changed-attachments".toByteArray()
            em.find(Asiakirja::class.java, fixture.attachmentId).asiakirjaData!!.data = "changed-upload".toByteArray()
            stored
        }
        transactions.executeWithoutResult {
            val stored = jobs.findByKey(key)!!
            assertThat(stored.payload).isEqualTo(payload)
            stored.asiakirjat.forEach { asiakirja ->
                assertThat(loader.load(asiakirja.toDescriptor()).sisalto).containsExactly(*archivedBytes.getValue(asiakirja.id!!))
            }
        }
        assertRepeatedApprovalKeepsJob(fixture, jobId)
        assertThat(jobCount(fixture)).isEqualTo(1)
        verify(mail).sendEmailFromTemplate(any<User>(), any(), any(), any(), any(), any())
        verify(mail).sendEmailFromTemplate(anyOrNull<String>(), any(), any(), any(), any(), any())
        verify(legacyArchive, never()).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
        verifyNoLegacyDelivery()
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class, names = ["TAMPEREEN_YLIOPISTO", "HELSINGIN_YLIOPISTO"])
    fun `repeated internal finalization keeps the existing job without generating documents or snapshots`(university: YliopistoEnum) {
        val fixture = fixture("EL", certificate = null, universityName = university)
        approve(fixture)
        val before = transactions.execute {
            val job = jobs.findByKey(key(fixture))!!
            Triple(job.id!!, job.payload, job.asiakirjat.map { document -> document.id })
        }
        val sourcesBefore = transactions.execute {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            listOf(request.yhteenvetoAsiakirja!!.id, request.liitteetAsiakirja!!.id, request.erikoistujanTiedotAsiakirja!!.id)
        }
        val snapshotsBefore = snapshotCount(fixture)
        clearInvocations(snapshots, mail)

        repeat(2) { _ ->
            transactions.executeWithoutResult {
                val request = requests.findByIdForHyvaksynta(fixture.id)!!
                finalization.viimeistele(fixture.id, request.valmistumispyynnonTarkistus!!, request, university)
            }
        }

        transactions.executeWithoutResult {
            val job = jobs.findByKey(key(fixture))!!
            assertThat(Triple(job.id!!, job.payload, job.asiakirjat.map { document -> document.id })).isEqualTo(before)
            assertThat(job.expectedDocumentCount).isEqualTo(before.third.size)
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            assertThat(listOf(request.yhteenvetoAsiakirja!!.id, request.liitteetAsiakirja!!.id, request.erikoistujanTiedotAsiakirja!!.id))
                .isEqualTo(sourcesBefore)
        }
        assertThat(snapshotCount(fixture)).isEqualTo(snapshotsBefore)
        assertThat(taskCount(before.first)).isEqualTo(1)
        assertThat(jobCount(fixture)).isEqualTo(1)
        verify(snapshots, never()).createReference(any(), any(), any())
        verifyNoEmailSent()
        verifyNoLegacyDelivery()
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class, names = ["TAMPEREEN_YLIOPISTO", "HELSINGIN_YLIOPISTO"])
    fun `original document identities remain frozen after live documents are replaced`(university: YliopistoEnum) {
        val fixture = fixture("EL", certificate = null, universityName = university)
        approve(fixture)
        val frozen = transactions.execute {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            assertQueuedDocumentsMatchSources(job, request)
            Triple(job.payload!!, request.yhteenvetoAsiakirja!!.id!!,
                job.asiakirjat.associate { document -> document.asiakirja!!.id!! to loader.load(document.toDescriptor()).sisalto })
        }

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val replacement = Asiakirja(
                opintooikeus = request.opintooikeus, nimi = "replacement.pdf", tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = PdfTestData.unembeddedFontPdf(1))
            )
            em.persist(replacement)
            request.yhteenvetoAsiakirja = replacement
        }

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            assertThat(request.yhteenvetoAsiakirja!!.id).isNotEqualTo(frozen.second)
            assertThat(em.find(Asiakirja::class.java, frozen.second)).isNull()
            assertThat(job.payload).isEqualTo(frozen.first)
            val summaryIdentity = mapper.readTree(job.payload)["documents"].single { identity ->
                identity["recordType"].asText() == RecordType.YHTEENVETO.name
            }
            assertThat(summaryIdentity["sourceDocumentId"].longValue()).isEqualTo(frozen.second)
            job.asiakirjat.forEach { document ->
                assertThat(loader.load(document.toDescriptor()).sisalto).containsExactly(*frozen.third.getValue(document.asiakirja!!.id!!))
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
        "TAMPEREEN_YLIOPISTO,EL,MISSING",
        "TAMPEREEN_YLIOPISTO,EHL,MISSING",
        "TAMPEREEN_YLIOPISTO,YEK,MISSING",
        "HELSINGIN_YLIOPISTO,EL,MISSING",
        "HELSINGIN_YLIOPISTO,EHL,MISSING",
        "HELSINGIN_YLIOPISTO,YEK,MISSING",
        "TAMPEREEN_YLIOPISTO,EL,EMPTY",
        "TAMPEREEN_YLIOPISTO,EL,MISSING_FILENAME",
        "TAMPEREEN_YLIOPISTO,EL,MISSING_CONTENT_TYPE",
        "HELSINGIN_YLIOPISTO,EL,EMPTY",
        "HELSINGIN_YLIOPISTO,EL,MISSING_FILENAME",
        "HELSINGIN_YLIOPISTO,EL,MISSING_CONTENT_TYPE"
    )
    fun `async approval queues exactly the universitys sync document set without requiring a certificate`(
        university: YliopistoEnum, kind: String, certificateState: String
    ) {
        val fixture = fixture(kind,
            certificate = when (certificateState) {
                "MISSING" -> null
                "EMPTY" -> byteArrayOf()
                else -> byteArrayOf(1)
            },
            certificateFilename = if (certificateState == "MISSING_FILENAME") "" else "unused-certificate.pdf",
            certificateContentType = if (certificateState == "MISSING_CONTENT_TYPE") "" else MediaType.APPLICATION_PDF_VALUE,
            universityName = university)

        approve(fixture)

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            val expectedTypes = if (university == YliopistoEnum.TAMPEREEN_YLIOPISTO)
                listOf(RecordType.YHTEENVETO, RecordType.LIITE) else listOf(RecordType.YHTEENVETO)
            assertThat(job.asiakirjat.map { document -> document.asiakirjatyyppi }).containsExactlyElementsOf(expectedTypes)
            assertThat(snapshotCount(fixture)).isEqualTo(expectedTypes.size.toLong())
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
            assertQueuedDocumentsMatchSources(job, request)
            assertSnapshots(job)
            assertZipContents(job, request, expectedTypes, properties)
        }
        verifyNoLegacyDelivery()
    }

    @Test
    fun `YEK approval without a certificate queues only the required document set`() {
        val fixture = fixture("YEK", certificate = null)
        approve(fixture)
        transactions.executeWithoutResult {
            val job = jobs.findByKey(key(fixture))!!
            assertThat(job.asiakirjat.map { document -> document.asiakirjatyyppi })
                .containsExactly(RecordType.YHTEENVETO, RecordType.LIITE)
            assertSnapshots(job)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            assertThat(em.find(Valmistumispyynto::class.java, fixture.id).vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
        }
    }

    private fun assertRepeatedApprovalKeepsJob(fixture: ApprovalFixture, jobId: UUID) {
        fun jobState() = transactions.execute {
            val job = jobs.findById(jobId).orElseThrow()
            Triple(job.payload, job.expectedDocumentCount, job.asiakirjat.map { document -> document.id })
        }
        val before = jobState()
        val snapshotsBefore = snapshotCount(fixture)
        assertThrows<BadRequestAlertException> { approve(fixture) }
        assertThat(transactions.execute { jobs.findByKey(key(fixture))?.id }).isEqualTo(jobId)
        assertThat(jobState()).isEqualTo(before)
        assertThat(snapshotCount(fixture)).isEqualTo(snapshotsBefore)
        assertThat(taskCount(jobId)).isEqualTo(1)
    }
}
