package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class KoejaksoArkistointiServiceTest {
    private val configuration = mock<ArkistointiConfigurationProvider>()
    private val synchronous = mock<SynchronousArkistointiService>()
    private val snapshots = mock<ArkistointiAsiakirjaSnapshotService>()
    private val creator = mock<ArkistointiJobCreator>()
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val service = KoejaksoArkistointiService(configuration, synchronous, snapshots, creator, mapper)
    private val university = YliopistoEnum.HELSINGIN_YLIOPISTO
    private val studyRight = Opintooikeus(id = 12, yliopisto = Yliopisto(nimi = university))
    private val document = Asiakirja(id = 22, nimi = "approval.pdf", tyyppi = "application/pdf")

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `disabled integration creates no snapshot or job`(type: CaseType) {
        archive(type)
        verifyNoInteractions(configuration, snapshots, creator)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `async preserves event metadata and creates one correct document reference`(type: CaseType) {
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(configuration.isAsyncEnabled(university, type)).thenReturn(true)
        val recordType = if (type == CaseType.SOPIMUS) RecordType.SOPIMUS else RecordType.ARVIOINTI
        val reference = ArkistointiAsiakirjaReference(document, recordType, 0, "approval.pdf", "application/pdf", "abc")
        whenever(snapshots.createReference(document, recordType, 0)).thenReturn(reference)

        archive(type)

        val request = argumentCaptor<CreateArkistointiJobRequest>()
        verify(creator).create(request.capture())
        assertThat(request.firstValue.caseType).isEqualTo(type)
        assertThat(request.firstValue.asiakirjat).containsExactly(reference)
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        assertThat(request.firstValue.key).isEqualTo("$prefix:${university.name}:7")
        val payload = mapper.readValue(request.firstValue.payload, KoejaksoArkistointiPayload::class.java)
        assertThat(payload.caseId).isEqualTo(7)
        assertThat(payload.opintooikeusId).isEqualTo(12)
        assertThat(payload.approvalDate).isEqualTo(LocalDate.of(2026, 10, 24))
        assertThat(payload.koejaksoHyvaksytty).isEqualTo(if (type == CaseType.KOEJAKSO) false else null)
        assertThat(payload.reviewer).isEqualTo(if (type == CaseType.SOPIMUS) "" else null)
        assertThat(payload.toString()).isEqualTo("KoejaksoArkistointiPayload(caseId=7)")
        verify(synchronous, never()).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        verify(synchronous, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `sync keeps existing package and delivery path`(type: CaseType) {
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(synchronous.muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )).thenReturn(ArkistointiPackage("/tmp/test.zip", null))

        archive(type)

        verify(synchronous).laheta(eq(university), eq("/tmp/test.zip"), eq(type), eq(false), eq("7"), anyOrNull())
        verifyNoInteractions(snapshots, creator)
    }

    private fun archive(type: CaseType) {
        val date = LocalDate.of(2026, 10, 24)
        if (type == CaseType.SOPIMUS) {
            service.archive(KoejaksonKoulutussopimus(id = 7, opintooikeus = studyRight, vastuuhenkilonKuittausaika = date), document)
        } else {
            service.archive(KoejaksonVastuuhenkilonArvio(
                id = 7, opintooikeus = studyRight, vastuuhenkilonKuittausaika = date, koejaksoHyvaksytty = false
            ), document)
        }
    }
}
