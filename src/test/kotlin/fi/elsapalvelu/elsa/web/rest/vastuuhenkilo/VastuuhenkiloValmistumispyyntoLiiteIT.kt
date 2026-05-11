package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import jakarta.persistence.EntityManager

/**
 * Integration tests for the valmistumispyynto approval flow focusing on
 * attachment (liite) edge cases discovered in ELSA-1127.
 *
 * These tests exercise the full HTTP → service → PDF generation stack so that
 * failures reproduce the real transaction rollback seen in production.
 *
 * Bug summary:
 *  1. [lisaaArvioinnit] calls [yhdistaPdf] for every suoritusarviointi attachment
 *     WITHOUT checking the MIME type.  A JPEG attachment causes iText to throw
 *     "PDF header not found", rolling back the entire approval transaction.
 *  2. A zero-byte attachment stored in the DB produces the same crash.
 *  3. [yhdistaAsiakirjat] has a catch block, but it catches
 *     [com.itextpdf.io.exceptions.IOException] while PDFBox throws
 *     [java.io.IOException] – a different type – so it also bubbles up uncaught.
 *
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VastuuhenkiloValmistumispyyntoLiiteIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restMockMvc: MockMvc

    private lateinit var opintooikeus: Opintooikeus
    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari
    private lateinit var vastuuhenkilo: Kayttaja
    private lateinit var anotherVastuuhenkilo: Kayttaja
    private lateinit var virkailija: Kayttaja

    private val validPdf: ByteArray by lazy {
        javaClass.getResourceAsStream("/fixtures/valid.pdf")!!.readBytes()
    }

    private val emptyPdf: ByteArray = ByteArray(0)

    /**
     * A real JPEG (1×1 px) – the primary production bug trigger for opintooikeus.
     * Users uploaded JPEG photos as suoritusarviointi attachments; the code passes
     * them straight to PdfReader without a MIME check → "PDF header not found".
     */
    private val validJpeg: ByteArray by lazy {
        javaClass.getResourceAsStream("/fixtures/valid.jpg")!!.readBytes()
    }

    @BeforeEach
    fun initTest() {
        initTestWithTehtavatyypit(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))
    }

    private fun initTestWithTehtavatyypit(vastuuhenkilonTehtavatyypit: List<VastuuhenkilonTehtavatyyppiEnum>) {        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity()
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
     * [lisaaArvioinnit] now routes JPEG attachments through [yhdistaPdfTaiKuva] which
     * embeds the image as a PDF page instead of passing it to PdfReader.
     */
    @Test
    @Transactional
    fun approvalFailsWhenArviointiAttachmentIsJpeg() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.IMAGE_JPEG_VALUE, validJpeg)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        // FIXED: JPEG attachment no longer crashes the approval transaction
        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * ELSA-1127 – PRIMARY BUG fixed: JPEG in itsearviointiAsiakirjat.
     */
    @Test
    @Transactional
    fun approvalFailsWhenItsearviointiAttachmentIsJpeg() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.IMAGE_JPEG_VALUE, validJpeg, itsearviointi = true)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        // FIXED: JPEG attachment no longer crashes the approval transaction
        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * ELSA-1127 – Zero-byte (empty) PDF attachment fixed.
     *
     * Empty blobs in [asiakirja_data] (LENGTH(data) = 0) are now skipped with a
     * warning in [lisaaArvioinnit] instead of being passed to PdfReader.
     */
    @Test
    @Transactional
    fun approvalFailsWhenArviointiAttachmentIsEmptyPdf() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, emptyPdf)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        // FIXED: empty PDF attachment is skipped, approval succeeds
        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * Non-empty but invalid bytes labeled as PDF used to slip past the MIME check
     * and fail in PdfReader with "PDF header not found".
     */
    @Test
    @Transactional
    fun approvalSucceedsWhenArviointiAttachmentPayloadIsNotActuallyPdf() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, validJpeg)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    /**
     * ELSA-1127 – Zero-byte PDF in itsearviointi collection fixed.
     */
    @Test
    @Transactional
    fun approvalFailsWhenItsearviointiAttachmentIsEmptyPdf() {

        persistKoulutussuunnitelma()
        persistSuoritusarviointiWithAsiakirja(MediaType.APPLICATION_PDF_VALUE, emptyPdf, itsearviointi = true)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        // FIXED: empty PDF attachment is skipped, approval succeeds
        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
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
     * ELSA-1127 – Empty motivaatiokirjeAsiakirja in Koulutussuunnitelma.
     *
     * [lisaaKoulutussuunnitelma] calls [yhdistaPdf] for the motivaatiokirje attachment.
     * A zero-byte blob causes "PDF header not found" and rolls back the approval.
     * The same defensive empty-data guard applied to [lisaaArvioinnit] is now also
     * applied here – empty data is skipped with a warning.
     */
    @Test
    @Transactional
    fun approvalSucceedsWhenMotivaatiokirjeAsiakirjaIsEmpty() {

        persistKoulutussuunnitelmaWithMotivaatiokirje(emptyPdf)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        // FIXED: empty motivaatiokirje blob is skipped, approval succeeds
        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }

    @Test
    @Transactional
    fun approvalSucceedsWhenMotivaatiokirjeAsiakirjaPayloadIsNotActuallyPdf() {

        persistKoulutussuunnitelmaWithMotivaatiokirje(validJpeg)

        em.clear()

        val valmistumispyynto = persistValmistumispyyntoOdottaaHyvaksyntaa()

        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
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
     * FIXED: [yhdistaPdfTaiKuva] handles both types – the PDF is merged and the
     * JPEG is embedded as an image page. No crash.
     */
    @Test
    @Transactional
    fun approvalFailsWhenMixedAttachmentsContainJpeg() {

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

        // FIXED: mixed PDF + JPEG no longer crashes
        performApproval(valmistumispyynto.id)
            .andExpect(status().isOk)
    }
}
