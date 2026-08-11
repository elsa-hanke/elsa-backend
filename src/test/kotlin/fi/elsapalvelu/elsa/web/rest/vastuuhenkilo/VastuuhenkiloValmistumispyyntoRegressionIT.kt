package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyynnonTarkistusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyyntoHelper
import org.apache.pdfbox.Loader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private const val ENDPOINT_BASE_URL = "/api/vastuuhenkilo"
private const val HYVAKSYNTA_ENDPOINT = "/valmistumispyynnon-hyvaksynta"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class VastuuhenkiloValmistumispyyntoRegressionIT : ResourceIntegrationTestBase() {

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    private lateinit var opintooikeus: Opintooikeus

    @Test
    fun ackYekValmistumispyyntoCreatesValidSummaryAndCompletesRequest() {
        initYekReviewer()
        val valmistumispyynto = persistYekRequestAwaitingApproval()

        testMockMvc.perform(
            put("$ENDPOINT_BASE_URL$HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.valmistumispyynto.tila")
                    .value(ValmistumispyynnonTila.HYVAKSYTTY.name)
            )

        em.flush()
        em.clear()
        val updated = valmistumispyyntoRepository.findById(valmistumispyynto.id!!).orElseThrow()
        assertThat(updated.vastuuhenkiloHyvaksyja?.id).isEqualTo(vastuuhenkilo.id)
        assertThat(updated.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updated.opintooikeus?.erikoisala?.id).isEqualTo(YEK_ERIKOISALA_ID)
        assertThat(updated.yhteenvetoAsiakirja?.nimi).startsWith("valmistumisen_yhteenveto_yek_")
        assertThat(updated.liitteetAsiakirja).isNotNull

        val summaryData = requireNotNull(updated.yhteenvetoAsiakirja?.asiakirjaData?.data)
        assertThat(summaryData).isNotEmpty
        Loader.loadPDF(summaryData).use { pdf ->
            assertThat(pdf.numberOfPages).isGreaterThan(0)
        }
    }

    @Test
    fun declineYekValmistumispyyntoReturnsRequestToTrainee() {
        initYekReviewer()
        val valmistumispyynto = persistYekRequestAwaitingApproval()

        testMockMvc.perform(
            put("$ENDPOINT_BASE_URL$HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON)
                .content(
                    convertObjectToJsonBytes(
                        ValmistumispyyntoHyvaksyntaFormDTO("Täydennä YEK-pyyntö")
                    )
                )
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.valmistumispyynto.tila")
                    .value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.name)
            )

        em.flush()
        em.clear()
        val updated = valmistumispyyntoRepository.findById(valmistumispyynto.id!!).orElseThrow()
        assertThat(updated.erikoistujanKuittausaika).isNull()
        assertThat(updated.virkailijanKuittausaika).isNull()
        assertThat(updated.vastuuhenkiloHyvaksyjaPalautusaika).isEqualTo(LocalDate.now())
        assertThat(updated.vastuuhenkiloHyvaksyjaKorjausehdotus).isEqualTo("Täydennä YEK-pyyntö")
    }

    @Test
    fun getValmistumispyynnonAsiakirjaReturnsLinkedDocumentForAssignedReviewer() {
        initReviewer(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI)
        val asiakirja = persistAsiakirja(opintooikeus)
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaArviointia(opintooikeus)
            .apply {
                vastuuhenkiloOsaamisenArvioija = vastuuhenkilo
                yhteenvetoAsiakirja = asiakirja
            }
        em.persist(valmistumispyynto)
        em.flush()

        testMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL/valmistumispyynto/{valmistumispyyntoId}/asiakirja/{asiakirjaId}",
                valmistumispyynto.id,
                asiakirja.id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PDF_VALUE))
            .andExpect(content().bytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA))
    }

    @Test
    fun getValmistumispyynnonAsiakirjaReturnsNotFoundForDocumentNotLinkedToRequest() {
        initReviewer(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI)
        val linkedAsiakirja = persistAsiakirja(opintooikeus)
        val unrelatedAsiakirja = persistAsiakirja(opintooikeus, "unrelated.pdf")
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaArviointia(opintooikeus)
            .apply {
                vastuuhenkiloOsaamisenArvioija = vastuuhenkilo
                yhteenvetoAsiakirja = linkedAsiakirja
            }
        em.persist(valmistumispyynto)
        em.flush()

        testMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL/valmistumispyynto/{valmistumispyyntoId}/asiakirja/{asiakirjaId}",
                valmistumispyynto.id,
                unrelatedAsiakirja.id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    fun getValmistumispyynnonTyoskentelyjaksoAsiakirjaEnforcesStudyRightBoundary() {
        initReviewer(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA)
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val asiakirja = persistTyoskentelyjaksoAsiakirja(opintooikeus)

        val anotherErikoistuva = initErikoistuvaLaakari(
            opintooikeus.yliopisto,
            opintooikeus.erikoisala
        )
        val anotherAsiakirja = persistTyoskentelyjaksoAsiakirja(
            anotherErikoistuva.getOpintooikeusKaytossa()!!
        )
        em.flush()

        testMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL/valmistumispyynto/{valmistumispyyntoId}/tyoskentelyjakso-liite/{asiakirjaId}",
                valmistumispyynto.id,
                asiakirja.id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().bytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA))

        testMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL/valmistumispyynto/{valmistumispyyntoId}/tyoskentelyjakso-liite/{asiakirjaId}",
                valmistumispyynto.id,
                anotherAsiakirja.id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    fun getValmistumispyynnonTyoskentelyjaksoAsiakirjaRequiresGraduationApprovalRole() {
        initReviewer()
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val asiakirja = persistTyoskentelyjaksoAsiakirja(opintooikeus)
        em.flush()

        testMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL/valmistumispyynto/{valmistumispyyntoId}/tyoskentelyjakso-liite/{asiakirjaId}",
                valmistumispyynto.id,
                asiakirja.id
            )
        ).andExpect(status().isNotFound)
    }

    private fun persistYekRequestAwaitingApproval(): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            virkailija = virkailija,
            virkailijanKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)
        val tarkistus = ValmistumispyynnonTarkistusHelper
            .createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)
        valmistumispyynto.valmistumispyynnonTarkistus = tarkistus
        em.flush()
        return valmistumispyynto
    }

    private fun initReviewer(vararg roles: VastuuhenkilonTehtavatyyppiEnum) {
        val reviewerUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(VASTUUHENKILO)
        )
        em.persist(reviewerUser)
        setSecurityContext(requireNotNull(reviewerUser.id), VASTUUHENKILO)

        val erikoistuvaLaakari = initErikoistuvaLaakari()
        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!
        vastuuhenkilo = KayttajaHelper.createEntity(em, reviewerUser)
        addReviewerRoles(roles.toSet())
        em.persist(vastuuhenkilo)
    }

    private fun initYekReviewer() {
        val reviewerUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(VASTUUHENKILO)
        )
        em.persist(reviewerUser)
        setSecurityContext(requireNotNull(reviewerUser.id), VASTUUHENKILO)

        val erikoistuvaLaakari = initErikoistuvaLaakari()
        opintooikeus = OpintooikeusHelper.addOpintooikeusForYekKoulutettava(
            em,
            erikoistuvaLaakari
        )
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, opintooikeus)

        vastuuhenkilo = KayttajaHelper.createEntity(em, reviewerUser)
        addReviewerRoles(setOf(VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN))
        em.persist(vastuuhenkilo)

        val officerUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(OPINTOHALLINNON_VIRKAILIJA)
        )
        em.persist(officerUser)
        virkailija = KayttajaHelper.createEntity(em, officerUser)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
        em.persist(virkailija)
        em.flush()
    }

    private fun addReviewerRoles(roles: Set<VastuuhenkilonTehtavatyyppiEnum>) {
        val tasks = em.findAll(VastuuhenkilonTehtavatyyppi::class)
            .filter { it.nimi in roles }
            .toMutableSet()
        vastuuhenkilo.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkilo,
                yliopisto = opintooikeus.yliopisto,
                erikoisala = opintooikeus.erikoisala,
                vastuuhenkilonTehtavat = tasks
            )
        )
    }

    private fun persistAsiakirja(
        opintooikeus: Opintooikeus,
        name: String = AsiakirjaHelper.ASIAKIRJA_PDF_NIMI
    ): Asiakirja {
        return Asiakirja(
            opintooikeus = opintooikeus,
            nimi = name,
            tyyppi = APPLICATION_PDF_VALUE,
            lisattypvm = LocalDateTime.now(),
            asiakirjaData = AsiakirjaData(data = AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        ).also { em.persist(it) }
    }

    private fun persistTyoskentelyjaksoAsiakirja(opintooikeus: Opintooikeus): Asiakirja {
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(
            em,
            user = opintooikeus.erikoistuvaLaakari?.kayttaja?.user
        ).apply {
            this.opintooikeus = opintooikeus
        }
        em.persist(tyoskentelyjakso)
        return Asiakirja(
            opintooikeus = opintooikeus,
            tyoskentelyjakso = tyoskentelyjakso,
            nimi = AsiakirjaHelper.ASIAKIRJA_PDF_NIMI,
            tyyppi = APPLICATION_PDF_VALUE,
            lisattypvm = LocalDateTime.now(),
            asiakirjaData = AsiakirjaData(data = AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        ).also { em.persist(it) }
    }
}
