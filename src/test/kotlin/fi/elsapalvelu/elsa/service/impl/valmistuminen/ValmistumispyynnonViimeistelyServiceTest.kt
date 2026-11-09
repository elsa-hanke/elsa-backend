package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobEnqueuer
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusMapper
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.*

class ValmistumispyynnonViimeistelyServiceTest {
    private val documents = mock<ValmistumispyynnonAsiakirjojenLuontiService>()
    private val reviewService = mock<ValmistumispyynnonTarkistusService>()
    private val reviewMapper = mock<ValmistumispyynnonTarkistusMapper>()
    private val archive = mock<SynchronousArkistointiService>()
    private val creator = mock<ArkistointiJobCreator>()
    private val snapshots = mock<ArkistointiAsiakirjaSnapshotService>()
    private val configuration = mock<ArkistointiConfigurationProvider>()
    private val arkistointiAsiakirjat = mock<ValmistuminenArkistointiAsiakirjat>()
    private val jobs = mock<ArkistointiJobRepository>()
    private val notifications = mock<ValmistumispyynnonIlmoitusService>()
    private val service = ValmistumispyynnonViimeistelyService(
        documents, reviewService, reviewMapper, notifications, archive,
        ArkistointiJobEnqueuer(jobs, snapshots, creator, jacksonObjectMapper()), configuration, arkistointiAsiakirjat
    )
    private val university = YliopistoEnum.HELSINGIN_YLIOPISTO
    private val review = ValmistumispyynnonTarkistus()

    @ParameterizedTest
    @EnumSource(ArkistointiJobStatus::class)
    fun `existing job skips PDF notifications completion and snapshots regardless of status`(status: ArkistointiJobStatus) {
        whenever(configuration.isAsyncEnabled(university, CaseType.VALMISTUMINEN)).thenReturn(true)
        whenever(jobs.findByKey("valmistuminen:${university.name}:7")).thenReturn(ArkistointiJob(status = status))

        service.viimeistele(7, review, Valmistumispyynto(id = 7), university)

        verifyNoInteractions(documents, reviewService, reviewMapper, notifications, arkistointiAsiakirjat, snapshots, creator)
    }

    @BeforeEach
    fun prepareApproval() {
        val dto = ValmistumispyynnonTarkistusDTO()
        whenever(reviewMapper.toDto(review)).thenReturn(dto)
        whenever(reviewService.taydenna(dto)).thenReturn(dto)
        whenever(documents.luo(eq(dto), any())).thenReturn(emptyList())
        whenever(archive.onKaytossa(university, CaseType.VALMISTUMINEN)).thenReturn(true)
    }

    @ParameterizedTest
    @CsvSource("false, false", "false, true", "true, false", "true, true")
    fun `missing study right or specialty prevents both delivery routes`(async: Boolean, missingStudyRight: Boolean) {
        whenever(configuration.isAsyncEnabled(university, CaseType.VALMISTUMINEN)).thenReturn(async)
        val request = Valmistumispyynto(id = 7, opintooikeus = if (missingStudyRight) null else Opintooikeus(id = 12))

        assertThatThrownBy { service.viimeistele(7, review, request, university) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(archive, never()).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        verify(archive, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        verifyNoInteractions(creator, snapshots, arkistointiAsiakirjat)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `synchronous graduation preserves the YEK delivery flag`(yek: Boolean) {
        val request = Valmistumispyynto(id = 7, opintooikeus = Opintooikeus(
            id = 12, erikoisala = Erikoisala(id = if (yek) YEK_ERIKOISALA_ID else 1)
        ))
        whenever(archive.muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )).thenReturn(ArkistointiPackage("/tmp/test.zip", null))

        service.viimeistele(7, review, request, university)

        verify(archive).laheta(eq(university), eq("/tmp/test.zip"), eq(CaseType.VALMISTUMINEN), eq(yek), eq("7"), isNull())
        verifyNoInteractions(creator, snapshots, arkistointiAsiakirjat)
    }
}
