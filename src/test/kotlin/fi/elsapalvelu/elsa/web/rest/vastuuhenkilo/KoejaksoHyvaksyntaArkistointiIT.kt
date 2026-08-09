package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

private const val ARVIO_ENDPOINT = "/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio"
private const val SOPIMUS_ENDPOINT = "/api/vastuuhenkilo/koejakso/koulutussopimus"
private const val TEST_ZIP_PATH = "/tmp/koejakso-arkistointi-test.zip"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KoejaksoHyvaksyntaArkistointiIT : ResourceIntegrationTestBase() {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var asiakirjaRepository: AsiakirjaRepository

    @Autowired
    private lateinit var koulutussopimusRepository: KoejaksonKoulutussopimusRepository

    @Autowired
    private lateinit var vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var koulutussopimusMapper: KoejaksonKoulutussopimusMapper

    @Autowired
    private lateinit var vastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper

    @MockitoBean
    private lateinit var arkistointiService: ArkistointiService

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var pdfService: PdfService

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var mailService: MailService

    private var fixture: KoejaksoFixture? = null

    @AfterEach
    fun cleanupCommittedData() {
        val current = fixture ?: return
        transactionTemplate.execute { _ ->
            cleanupDocuments(current)
            cleanupKoejakso(current)
            cleanupRelationships(current)
            em.clear()
        }
        fixture = null
    }

    private fun cleanupDocuments(current: KoejaksoFixture) {
        val asiakirjaDataIds = em.createNativeQuery(
            "SELECT asiakirja_data_id FROM asiakirja WHERE opintooikeus_id = ${current.opintooikeusId}"
        ).resultList.mapNotNull { value -> (value as? Number)?.toLong() }

        em.createNativeQuery(
            "DELETE FROM asiakirja WHERE opintooikeus_id = ${current.opintooikeusId}"
        ).executeUpdate()
        if (asiakirjaDataIds.isNotEmpty()) {
            em.createNativeQuery(
                "DELETE FROM asiakirja_data WHERE id IN (${asiakirjaDataIds.joinToString()})"
            ).executeUpdate()
        }
    }

    private fun cleanupKoejakso(current: KoejaksoFixture) {
        em.createNativeQuery(
            "DELETE FROM koulutussopimuksen_kouluttaja WHERE koulutussopimus_id = ${current.koulutussopimusId}"
        ).executeUpdate()
        em.createNativeQuery(
            "DELETE FROM koulutussopimuksen_koulutuspaikka WHERE koulutussopimus_id = ${current.koulutussopimusId}"
        ).executeUpdate()
        listOf(
            "koejakson_vastuuhenkilon_arvio",
            "koejakson_loppukeskustelu",
            "koejakson_kehittamistoimenpiteet",
            "koejakson_valiarviointi",
            "koejakson_aloituskeskustelu"
        ).forEach { table ->
            em.createNativeQuery(
                "DELETE FROM $table WHERE opintooikeus_id = ${current.opintooikeusId}"
            ).executeUpdate()
        }
        em.createNativeQuery(
            "DELETE FROM koejakson_koulutussopimus WHERE id = ${current.koulutussopimusId}"
        ).executeUpdate()
    }

    private fun cleanupRelationships(current: KoejaksoFixture) {
        em.createNativeQuery(
            "DELETE FROM rel_kayttaja__yliopisto WHERE yliopisto_id = ${current.yliopistoId}"
        ).executeUpdate()
        em.createNativeQuery(
            "DELETE FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi " +
                "WHERE kayttaja_yliopisto_erikoisala_id IN " +
                "(SELECT id FROM kayttaja_yliopisto_erikoisala WHERE yliopisto_id = ${current.yliopistoId})"
        ).executeUpdate()
        em.createNativeQuery(
            "DELETE FROM kayttaja_yliopisto_erikoisala WHERE yliopisto_id = ${current.yliopistoId}"
        ).executeUpdate()
        em.createNativeQuery("DELETE FROM opintooikeus WHERE id = ${current.opintooikeusId}").executeUpdate()
        em.createNativeQuery(
            "DELETE FROM erikoistuva_laakari WHERE id = ${current.erikoistuvaLaakariId}"
        ).executeUpdate()
        em.createNativeQuery("DELETE FROM yliopisto WHERE id = ${current.yliopistoId}").executeUpdate()
    }

    @Test
    fun `vastuuhenkilon arvio approval persists and archives the generated PDF`() {
        val current = createFixture()
        stubSuccessfulArchiving()

        approveVastuuhenkilonArvio(current).andExpect(status().isOk)

        assertVastuuhenkilonArvioApproved(current.vastuuhenkilonArvioId)
        assertGeneratedDocumentWasArchived(
            current,
            current.vastuuhenkilonArvioId,
            CaseType.KOEJAKSO,
            RecordType.ARVIOINTI,
            "koejakson_vastuuhenkilon_arvio_"
        )
    }

    @Test
    fun `vastuuhenkilon arvio approval persists PDF without delivery when archiving is disabled`() {
        val current = createFixture()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(false)

        approveVastuuhenkilonArvio(current).andExpect(status().isOk)

        assertVastuuhenkilonArvioApproved(current.vastuuhenkilonArvioId)
        assertSinglePersistedDocument(current.opintooikeusId, "koejakson_vastuuhenkilon_arvio_")
        verifyNoPackageOrDelivery()
    }

    @Test
    fun `vastuuhenkilon arvio package failure rolls back approval and generated PDF`() {
        val current = createFixture()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(
            arkistointiService.muodostaSahke(
                anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), any()
            )
        ).thenThrow(RuntimeException("SÄHKE-paketin muodostaminen epäonnistui"))

        approveVastuuhenkilonArvio(current).andExpect(status().is5xxServerError)

        assertVastuuhenkilonArvioRolledBack(current)
        verify(arkistointiService, never()).laheta(
            any(), any(), any(), any(), anyOrNull(), anyOrNull()
        )
    }

    @Test
    fun `vastuuhenkilon arvio delivery failure rolls back approval and generated PDF`() {
        val current = createFixture()
        stubSuccessfulPackageBuilding()
        whenever(
            arkistointiService.laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        ).thenThrow(RuntimeException("Arkistointipalvelu ei vastaa"))

        approveVastuuhenkilonArvio(current).andExpect(status().is5xxServerError)

        assertVastuuhenkilonArvioRolledBack(current)
        verify(arkistointiService).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(),
            anyOrNull(), anyOrNull(), anyOrNull(), eq(CaseType.KOEJAKSO)
        )
    }

    @Test
    fun `koulutussopimus approval persists and archives the generated PDF`() {
        val current = createFixture()
        stubSuccessfulArchiving()

        approveKoulutussopimus(current).andExpect(status().isOk)

        assertKoulutussopimusApproved(current.koulutussopimusId)
        assertGeneratedDocumentWasArchived(
            current,
            current.koulutussopimusId,
            CaseType.SOPIMUS,
            RecordType.SOPIMUS,
            "koejakson_koulutussopimus_"
        )
    }

    @Test
    fun `koulutussopimus approval persists PDF without delivery when archiving is disabled`() {
        val current = createFixture()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(false)

        approveKoulutussopimus(current).andExpect(status().isOk)

        assertKoulutussopimusApproved(current.koulutussopimusId)
        assertSinglePersistedDocument(current.opintooikeusId, "koejakson_koulutussopimus_")
        verifyNoPackageOrDelivery()
    }

    @Test
    fun `koulutussopimus package failure rolls back approval and generated PDF`() {
        val current = createFixture()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(
            arkistointiService.muodostaSahke(
                anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), any()
            )
        ).thenThrow(RuntimeException("SÄHKE-paketin muodostaminen epäonnistui"))

        approveKoulutussopimus(current).andExpect(status().is5xxServerError)

        assertKoulutussopimusRolledBack(current)
        verify(arkistointiService, never()).laheta(
            any(), any(), any(), any(), anyOrNull(), anyOrNull()
        )
    }

    @Test
    fun `koulutussopimus delivery failure rolls back approval and generated PDF`() {
        val current = createFixture()
        stubSuccessfulPackageBuilding()
        whenever(
            arkistointiService.laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        ).thenThrow(RuntimeException("Arkistointipalvelu ei vastaa"))

        approveKoulutussopimus(current).andExpect(status().is5xxServerError)

        assertKoulutussopimusRolledBack(current)
        verify(arkistointiService).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(),
            anyOrNull(), anyOrNull(), anyOrNull(), eq(CaseType.SOPIMUS)
        )
    }

    private fun createFixture(): KoejaksoFixture = transactionTemplate.execute { _ ->
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(VASTUUHENKILO)
        )
        em.persist(vastuuhenkiloUser)
        em.flush()
        setSecurityContext(vastuuhenkiloUser.id!!, VASTUUHENKILO)

        val yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO)
        em.persist(yliopisto)

        val erikoistuvaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(ERIKOISTUVA_LAAKARI)
        )
        em.persist(erikoistuvaUser)
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(
            em,
            user = erikoistuvaUser,
            yliopisto = yliopisto
        )
        val opintooikeus = requireNotNull(erikoistuvaLaakari.getOpintooikeusKaytossa())
        val vastuuhenkilo = createVastuuhenkiloForKoejakso(
            erikoistuvaLaakari,
            user = vastuuhenkiloUser
        )

        val kouluttaja = KayttajaHelper.createEntity(em).also(em::persist)
        val esimies = KayttajaHelper.createEntity(em).also(em::persist)
        val virkailija = KayttajaHelper.createEntity(em).also(em::persist)
        val vaiheet = KoejaksonVaiheetHelper.persistKoejaksoVaiheet(
            em,
            erikoistuvaLaakari,
            kouluttaja,
            esimies,
            vastuuhenkilo,
            yliopisto
        )
        vaiheet.koulutussopimus.erikoistuvanAllekirjoitusaika = LocalDate.now()

        val vastuuhenkilonArvio = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(
            erikoistuvaLaakari,
            vastuuhenkilo
        ).apply {
            erikoistuvanKuittausaika = LocalDate.now()
            this.virkailija = virkailija
            virkailijaHyvaksynyt = true
            virkailijanKuittausaika = LocalDate.now()
        }
        em.persist(vastuuhenkilonArvio)
        em.flush()

        KoejaksoFixture(
            opintooikeusId = requireNotNull(opintooikeus.id),
            erikoistuvaLaakariId = requireNotNull(erikoistuvaLaakari.id),
            yliopistoId = requireNotNull(yliopisto.id),
            koulutussopimusId = requireNotNull(vaiheet.koulutussopimus.id),
            vastuuhenkilonArvioId = requireNotNull(vastuuhenkilonArvio.id)
        )
    }!!.also { fixture = it }

    private fun approveVastuuhenkilonArvio(current: KoejaksoFixture) = testMockMvc.perform(
        put(ARVIO_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(vastuuhenkilonArvioDto(current.vastuuhenkilonArvioId)))
            .with(csrf())
    )

    private fun approveKoulutussopimus(current: KoejaksoFixture) = testMockMvc.perform(
        put(SOPIMUS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(koulutussopimusDto(current.koulutussopimusId)))
            .with(csrf())
    )

    private fun vastuuhenkilonArvioDto(id: Long): KoejaksonVastuuhenkilonArvioDTO =
        transactionTemplate.execute { _ ->
            vastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvioRepository.findById(id).orElseThrow())
                .apply {
                    koejaksoHyvaksytty = true
                    vastuuhenkilonKorjausehdotus = null
                }
        }!!

    private fun koulutussopimusDto(id: Long): KoejaksonKoulutussopimusDTO =
        transactionTemplate.execute { _ ->
            koulutussopimusMapper.toDto(koulutussopimusRepository.findById(id).orElseThrow())
                .apply {
                    vastuuhenkilo?.sopimusHyvaksytty = true
                    vastuuhenkilo?.kuittausaika = LocalDate.now()
                    korjausehdotus = null
                }
        }!!

    private fun stubSuccessfulArchiving() {
        stubSuccessfulPackageBuilding()
    }

    private fun stubSuccessfulPackageBuilding() {
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(
            arkistointiService.muodostaSahke(
                anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), any()
            )
        ).thenReturn(ArkistointiResult(TEST_ZIP_PATH, null))
    }

    private fun assertGeneratedDocumentWasArchived(
        current: KoejaksoFixture,
        caseId: Long,
        caseType: CaseType,
        recordType: RecordType,
        filenamePrefix: String
    ) {
        val documentsCaptor = argumentCaptor<List<RecordProperties>>()
        verify(arkistointiService).muodostaSahke(
            anyOrNull(),
            documentsCaptor.capture(),
            eq(caseId.toString()),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(YliopistoEnum.TAMPEREEN_YLIOPISTO),
            eq(caseType)
        )

        val persisted = assertSinglePersistedDocument(current.opintooikeusId, filenamePrefix)
        assertThat(documentsCaptor.firstValue).hasSize(1)
        assertThat(documentsCaptor.firstValue.single().type).isEqualTo(recordType)
        assertThat(documentsCaptor.firstValue.single().asiakirja.id).isEqualTo(persisted.id)
        assertThat(documentsCaptor.firstValue.single().asiakirja.nimi).isEqualTo(persisted.nimi)

        verify(arkistointiService).laheta(
            eq(YliopistoEnum.TAMPEREEN_YLIOPISTO),
            eq(TEST_ZIP_PATH),
            eq(caseType),
            any(),
            eq(caseId.toString()),
            anyOrNull()
        )
    }

    private fun assertSinglePersistedDocument(opintooikeusId: Long, filenamePrefix: String): PersistedDocument {
        val documents = transactionTemplate.execute { _ ->
            asiakirjaRepository.findAllByOpintooikeusId(opintooikeusId).map { document ->
                document.toPersistedDocument()
            }
        }!!
        assertThat(documents).hasSize(1)
        return documents.single().also { document ->
            assertThat(document.id).isNotNull()
            assertThat(document.nimi).startsWith(filenamePrefix).endsWith(".pdf")
            assertThat(document.tyyppi).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        }
    }

    private fun assertVastuuhenkilonArvioApproved(id: Long) {
        val state = transactionTemplate.execute { _ ->
            vastuuhenkilonArvioRepository.findById(id).orElseThrow().let { arvio ->
                ApprovalState(arvio.vastuuhenkiloHyvaksynyt, arvio.vastuuhenkilonKuittausaika, arvio.koejaksoHyvaksytty)
            }
        }!!
        assertThat(state.hyvaksytty).isTrue()
        assertThat(state.kuittausaika).isNotNull()
        assertThat(state.koejaksoHyvaksytty).isTrue()
    }

    private fun assertKoulutussopimusApproved(id: Long) {
        val state = transactionTemplate.execute { _ ->
            koulutussopimusRepository.findById(id).orElseThrow().let { sopimus ->
                ApprovalState(sopimus.vastuuhenkiloHyvaksynyt, sopimus.vastuuhenkilonKuittausaika)
            }
        }!!
        assertThat(state.hyvaksytty).isTrue()
        assertThat(state.kuittausaika).isNotNull()
    }

    private fun assertVastuuhenkilonArvioRolledBack(current: KoejaksoFixture) {
        val state = transactionTemplate.execute { _ ->
            vastuuhenkilonArvioRepository.findById(current.vastuuhenkilonArvioId).orElseThrow().let { arvio ->
                ApprovalState(arvio.vastuuhenkiloHyvaksynyt, arvio.vastuuhenkilonKuittausaika, arvio.koejaksoHyvaksytty)
            }
        }!!
        assertThat(state.hyvaksytty).isFalse()
        assertThat(state.kuittausaika).isNull()
        assertThat(state.koejaksoHyvaksytty).isNull()
        assertNoPersistedDocuments(current.opintooikeusId)
    }

    private fun assertKoulutussopimusRolledBack(current: KoejaksoFixture) {
        val state = transactionTemplate.execute { _ ->
            koulutussopimusRepository.findById(current.koulutussopimusId).orElseThrow().let { sopimus ->
                ApprovalState(sopimus.vastuuhenkiloHyvaksynyt, sopimus.vastuuhenkilonKuittausaika)
            }
        }!!
        assertThat(state.hyvaksytty).isFalse()
        assertThat(state.kuittausaika).isNull()
        assertNoPersistedDocuments(current.opintooikeusId)
    }

    private fun assertNoPersistedDocuments(opintooikeusId: Long) {
        val documents = transactionTemplate.execute { _ ->
            asiakirjaRepository.findAllByOpintooikeusId(opintooikeusId)
        }!!
        assertThat(documents).isEmpty()
    }

    private fun verifyNoPackageOrDelivery() {
        verify(arkistointiService, never()).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(),
            anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        verify(arkistointiService, never()).laheta(
            any(), any(), any(), any(), anyOrNull(), anyOrNull()
        )
    }

    private fun Asiakirja.toPersistedDocument() = PersistedDocument(id, nimi, tyyppi)

    private data class KoejaksoFixture(
        val opintooikeusId: Long,
        val erikoistuvaLaakariId: Long,
        val yliopistoId: Long,
        val koulutussopimusId: Long,
        val vastuuhenkilonArvioId: Long
    )

    private data class PersistedDocument(
        val id: Long?,
        val nimi: String?,
        val tyyppi: String?
    )

    private data class ApprovalState(
        val hyvaksytty: Boolean,
        val kuittausaika: LocalDate?,
        val koejaksoHyvaksytty: Boolean? = null
    )
}
