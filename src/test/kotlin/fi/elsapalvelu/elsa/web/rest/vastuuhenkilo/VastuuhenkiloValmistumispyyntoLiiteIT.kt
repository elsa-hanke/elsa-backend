package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentException
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.apache.pdfbox.Loader
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import jakarta.persistence.EntityManager

/**
 * Integration tests for the valmistumispyynto approval flow focusing on
 * attachment (liite) edge cases discovered in ELSA-1127.
 *
 * These tests exercise the full HTTP → service → PDF generation stack so that
 * failures reproduce the real transaction rollback seen in production.
 *
 * Legacy attachments whose stored PDF metadata does not match their content must
 * block approval with an actionable validation response. Valid PDFs are merged normally.
 *
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VastuuhenkiloValmistumispyyntoLiiteIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restMockMvc: MockMvc

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    private lateinit var opintooikeus: Opintooikeus
    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari
    private lateinit var vastuuhenkilo: Kayttaja
    private lateinit var anotherVastuuhenkilo: Kayttaja
    private lateinit var virkailija: Kayttaja

    private val validPdf: ByteArray by lazy {
        javaClass.getResourceAsStream("/fixtures/valid.pdf")!!.readBytes()
    }

    private val emptyPdf: ByteArray = ByteArray(0)

    private val docxContent: ByteArray by lazy {
        ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("[Content_Types].xml"))
                zip.write("<Types/>".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("word/document.xml"))
                zip.write("<document/>".toByteArray())
                zip.closeEntry()
            }
            output.toByteArray()
        }
    }

    /**
     * A real JPEG (1×1 px) representing an attachment that must not be merged as PDF.
     */
    private val validJpeg: ByteArray by lazy {
        javaClass.getResourceAsStream("/fixtures/valid.jpg")!!.readBytes()
    }

    @BeforeEach
    fun initTest() {
        initTestWithTehtavatyypit(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))
    }

    private fun initTestWithTehtavatyypit(vastuuhenkilonTehtavatyypit: List<VastuuhenkilonTehtavatyyppiEnum>) {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(vastuuhenkiloUser)

        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(vastuuhenkiloUser.id, emptyMap()),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        erikoistuvaLaakari = initErikoistuvaLaakari()
        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)
        vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        val tehtavat = tehtavatyypit.filter { it.nimi in vastuuhenkilonTehtavatyypit }.toMutableSet()
        initVastuuhenkiloErikoisalat(vastuuhenkilo, opintooikeus.yliopisto!!, opintooikeus.erikoisala!!, tehtavat)
        em.persist(vastuuhenkilo)

        if (!vastuuhenkilonTehtavatyypit.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI) ||
            !vastuuhenkilonTehtavatyypit.contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA)
        ) {
            val anotherUser = KayttajaResourceWithMockUserIT.createEntity(
                authority = Authority(VASTUUHENKILO)
            )
            em.persist(anotherUser)

            anotherVastuuhenkilo = KayttajaHelper.createEntity(em, anotherUser)
            val anotherTehtavat = tehtavatyypit.filter { it.nimi !in vastuuhenkilonTehtavatyypit }.toMutableSet()
            initVastuuhenkiloErikoisalat(
                anotherVastuuhenkilo,
                opintooikeus.yliopisto!!,
                opintooikeus.erikoisala!!,
                anotherTehtavat
            )
            em.persist(anotherVastuuhenkilo)
        }

        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(OPINTOHALLINNON_VIRKAILIJA)
        )
        em.persist(virkailijaUser)
        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        em.persist(virkailija)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
    }

    private fun initVastuuhenkiloErikoisalat(
        kayttaja: Kayttaja,
        yliopisto: Yliopisto,
        erikoisala: Erikoisala,
        tehtavat: MutableSet<VastuuhenkilonTehtavatyyppi>
    ) {
        val newErikoisala = ErikoisalaHelper.createEntity()
        val anotherNewErikoisala = ErikoisalaHelper.createEntity()
        em.persist(newErikoisala)
        em.persist(anotherNewErikoisala)

        listOf(newErikoisala, erikoisala, anotherNewErikoisala).forEach { e ->
            kayttaja.yliopistotAndErikoisalat.add(
                KayttajaYliopistoErikoisala(
                    kayttaja = kayttaja,
                    yliopisto = yliopisto,
                    erikoisala = e,
                    vastuuhenkilonTehtavat = tehtavat
                )
            )
        }
    }

    private fun initErikoistuvaLaakari(): ErikoistuvaLaakari {
        val user = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(ERIKOISTUVA_LAAKARI)
        )
        em.persist(user)
        val el = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(el)
        return el
    }

    private fun performApproval(valmistumispyyntoId: Long?) =
        restMockMvc.perform(
            put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/{id}", valmistumispyyntoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                .with(csrf())
        )

    /**
     * Persist a suoritusarviointi with a single attachment of [tyyppi] and [data].
     * When [itsearviointi] is true the file is stored as an *itsearviointiAsiakirja*,
     * otherwise as an *arviointiAsiakirja*.
     */
    private fun persistSuoritusarviointiWithAsiakirja(
        tyyppi: String,
        data: ByteArray,
        itsearviointi: Boolean = false
    ) {
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em)
        tyoskentelyjakso.opintooikeus = opintooikeus
        em.persist(tyoskentelyjakso)
        em.flush()

        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em)
        em.persist(suoritusarviointi)
        em.flush()

        val asiakirja = Asiakirja(
            opintooikeus = opintooikeus,
            arviointi = if (!itsearviointi) suoritusarviointi else null,
            itsearviointi = if (itsearviointi) suoritusarviointi else null,
            nimi = "liite.${tyyppi.substringAfter("/")}",
            tyyppi = tyyppi,
            lisattypvm = LocalDateTime.now(),
            asiakirjaData = AsiakirjaData(data = data)
        )
        em.persist(asiakirja)
        em.flush()
    }

    private fun persistTyoskentelyjaksoWithAsiakirja(data: ByteArray) {
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em)
        tyoskentelyjakso.opintooikeus = opintooikeus
        em.persist(tyoskentelyjakso)
        em.flush()

        em.persist(
            Asiakirja(
                opintooikeus = opintooikeus,
                tyoskentelyjakso = tyoskentelyjakso,
                nimi = "tyotodistus.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = data)
            )
        )
        em.flush()
    }

    private fun persistValmistumispyyntoOdottaaHyvaksyntaa(): Valmistumispyynto {
        val freshOpintooikeus = em.find(Opintooikeus::class.java, opintooikeus.id!!)
        val freshAnotherVastuuhenkilo = em.find(Kayttaja::class.java, anotherVastuuhenkilo.id!!)
        val freshVirkailija = em.find(Kayttaja::class.java, virkailija.id!!)

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            freshOpintooikeus, freshAnotherVastuuhenkilo, freshVirkailija
        )
        em.persist(valmistumispyynto)

        val tarkistus = ValmistumispyynnonTarkistusHelper
            .createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)
        valmistumispyynto.valmistumispyynnonTarkistus = tarkistus
        em.flush()
        return valmistumispyynto
    }

    private fun persistKoulutussuunnitelma() {
        val koulutussuunnitelma = Koulutussuunnitelma(
            opintooikeus = opintooikeus,
            motivaatiokirjeYksityinen = false,
            opiskeluJaTyohistoriaYksityinen = false,
            vahvuudetYksityinen = false,
            tulevaisuudenVisiointiYksityinen = false,
            osaamisenKartuttaminenYksityinen = false,
            elamankenttaYksityinen = false
        )
        em.persist(koulutussuunnitelma)
        em.flush()
    }

    private fun persistKoulutussuunnitelmaWithMotivaatiokirje(data: ByteArray) {
        val asiakirja = Asiakirja(
            opintooikeus = opintooikeus,
            nimi = "motivaatiokirje.pdf",
            tyyppi = MediaType.APPLICATION_PDF_VALUE,
            lisattypvm = LocalDateTime.now(),
            asiakirjaData = AsiakirjaData(data = data)
        )
        em.persist(asiakirja)

        val koulutussuunnitelma = Koulutussuunnitelma(
            opintooikeus = opintooikeus,
            motivaatiokirjeYksityinen = false,
            opiskeluJaTyohistoriaYksityinen = false,
            vahvuudetYksityinen = false,
            tulevaisuudenVisiointiYksityinen = false,
            osaamisenKartuttaminenYksityinen = false,
            elamankenttaYksityinen = false,
            motivaatiokirjeAsiakirja = asiakirja
        )
        em.persist(koulutussuunnitelma)
        em.flush()
    }

    // ── tests ────────────────────────────────────────────────────────────────

    /**
     * ELSA-1127 – PRIMARY BUG fixed: JPEG in arviointiAsiakirjat.
     *
     * [lisaaArvioinnit] skips JPEG attachments instead of passing them to PdfReader.
     */
    @Test
    @Transactional
    fun approvalSucceedsBySkippingJpegArviointiAttachment() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.IMAGE_JPEG_VALUE, validJpeg)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * ELSA-1127 – PRIMARY BUG fixed: JPEG in itsearviointiAsiakirjat.
     */
    @Test
    @Transactional
    fun approvalSucceedsBySkippingJpegItsearviointiAttachment() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.IMAGE_JPEG_VALUE, validJpeg, itsearviointi = true)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * A legacy zero-byte assessment PDF must block approval instead of being omitted.
     */
    @Test
    @Transactional
    fun approvalIsBlockedWhenArviointiPdfIsEmpty() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, emptyPdf)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.message").value(
                    "error.${InvalidPdfAttachmentException.ERROR_KEY}"
                )
            )
            .andExpect(jsonPath("$.attachmentName").value("liite.pdf"))
            .andExpect(jsonPath("$.attachmentSource").value("arviointi"))
            .andExpect(jsonPath("$.attachmentDate").value("1970-01-01"))
    }

    /**
     * Legacy data can contain a DOCX whose stored name and MIME type claim it is a PDF.
     * Approval must identify the affected assessment and require the file to be corrected.
     */
    @Test
    @Transactional
    fun approvalIsBlockedWhenDocxContentIsLabelledAsPdf() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, docxContent)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.message").value(
                    "error.${InvalidPdfAttachmentException.ERROR_KEY}"
                )
            )
            .andExpect(jsonPath("$.attachmentName").value("liite.pdf"))
            .andExpect(jsonPath("$.attachmentSource").value("arviointi"))
            .andExpect(jsonPath("$.attachmentDate").value("1970-01-01"))
    }

    /**
     * A legacy invalid PDF attached to a work period must also block approval.
     */
    @Test
    @Transactional
    fun approvalIsBlockedWhenTyoskentelyjaksoPdfIsInvalid() {

        persistKoulutussuunnitelma()
        persistTyoskentelyjaksoWithAsiakirja(docxContent)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.attachmentName").value("tyotodistus.pdf"))
            .andExpect(jsonPath("$.attachmentSource").value("tyoskentelyjakso"))
            .andExpect(jsonPath("$.attachmentDate").value("1970-01-01"))
    }

    /**
     * A legacy zero-byte self-assessment PDF must block approval instead of being omitted.
     */
    @Test
    @Transactional
    fun approvalIsBlockedWhenItsearviointiPdfIsEmpty() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, emptyPdf, itsearviointi = true)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.attachmentName").value("liite.pdf"))
            .andExpect(jsonPath("$.attachmentSource").value("itsearviointi"))
            .andExpect(jsonPath("$.attachmentDate").value("1970-01-01"))
    }

    /**
     * Happy path: a valid PDF attachment on suoritusarviointi must NOT crash approval.
     *
     * If this test fails it means a regression was introduced in the PDF merge path.
     */
    @Test
    @Transactional
    fun approvalSucceedsWhenArviointiAttachmentIsValidPdf() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, validPdf)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)

        assertGeneratedTraineeDataDocumentIsValid(valmistumispyynto.id!!)
    }

    /**
     * Happy path: no attachments at all – approval must succeed.
     */
    @Test
    @Transactional
    fun approvalSucceedsWithNoAttachments() {

        persistKoulutussuunnitelma()

        // No suoritusarviointi / asiakirja persisted on purpose.
        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * A legacy empty motivation-letter PDF must block approval instead of being omitted.
     */
    @Test
    @Transactional
    fun approvalIsBlockedWhenMotivaatiokirjeAsiakirjaIsEmpty() {

        persistKoulutussuunnitelmaWithMotivaatiokirje(emptyPdf)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.attachmentName").value("motivaatiokirje.pdf"))
            .andExpect(jsonPath("$.attachmentSource").value("motivaatiokirje"))
            .andExpect(jsonPath("$.attachmentDate").doesNotExist())
    }

    /**
     * Happy path: a valid PDF motivaatiokirje is merged into the final document.
     */
    @Test
    @Transactional
    fun approvalSucceedsWhenMotivaatiokirjeAsiakirjaIsValidPdf() {

        persistKoulutussuunnitelmaWithMotivaatiokirje(validPdf)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * Mixed attachments: one valid PDF + one JPEG on the same suoritusarviointi.
     *
     * The PDF is merged and the unsupported JPEG is skipped. No crash.
     */
    @Test
    @Transactional
    fun approvalSucceedsWithMixedAttachmentsByMergingOnlyPdf() {

        persistKoulutussuunnitelma()

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em)
        tyoskentelyjakso.opintooikeus = opintooikeus
        em.persist(tyoskentelyjakso)
        em.flush()

        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em)
        em.persist(suoritusarviointi)
        em.flush()

        em.persist(
            Asiakirja(
                opintooikeus = opintooikeus,
                arviointi = suoritusarviointi,
                nimi = "valid.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = validPdf)
            )
        )
        em.persist(
            Asiakirja(
                opintooikeus = opintooikeus,
                arviointi = suoritusarviointi,
                nimi = "photo.jpg",
                tyyppi = MediaType.IMAGE_JPEG_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = validJpeg)
            )
        )
        em.flush()
        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    private fun assertGeneratedTraineeDataDocumentIsValid(valmistumispyyntoId: Long) {
        em.flush()
        em.clear()
        val updated = valmistumispyyntoRepository.findById(valmistumispyyntoId).orElseThrow()
        val data = requireNotNull(updated.erikoistujanTiedotAsiakirja?.asiakirjaData?.data)
        assertThat(data).isNotEmpty
        Loader.loadPDF(data).use { pdf ->
            assertThat(pdf.numberOfPages).isGreaterThan(3)
        }
    }
}
