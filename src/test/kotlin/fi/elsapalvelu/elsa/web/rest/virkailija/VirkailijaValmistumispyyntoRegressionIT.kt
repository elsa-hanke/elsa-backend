package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyyntoHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private const val ENDPOINT_BASE_URL = "/api/virkailija"
private const val TARKISTUS_ENDPOINT = "/valmistumispyynnon-tarkistus"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class VirkailijaValmistumispyyntoRegressionIT : ResourceIntegrationTestBase() {

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    private lateinit var opintooikeus: Opintooikeus
    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    @Test
    fun updateValmistumispyynnonTarkistusUpdatesLicensingInformation() {
        initOfficer()
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)
        val licensingDate = LocalDate.of(2020, 2, 3)
        val certificateData = javaClass.getResourceAsStream("/fixtures/valid.pdf")!!.readBytes()
        val certificate = MockMultipartFile(
            "laillistamistodistus",
            "laillistamistodistus.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            certificateData
        )

        testMockMvc.perform(
            multipart(
                "$ENDPOINT_BASE_URL$TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .file(certificate)
                .param("laillistamispaiva", licensingDate.toString())
                .param("keskenerainen", "true")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        em.flush()
        em.clear()
        val updated = em.find(ErikoistuvaLaakari::class.java, erikoistuvaLaakari.id)
        assertThat(updated.laillistamispaiva).isEqualTo(licensingDate)
        assertThat(updated.laillistamispaivanLiitetiedostonNimi)
            .isEqualTo("laillistamistodistus.pdf")
        assertThat(updated.laillistamispaivanLiitetiedostonTyyppi)
            .isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(updated.laillistamistodistus?.data).isEqualTo(certificateData)
    }

    @Test
    fun ackYekValmistumispyynnonTarkistusSendsRequestToFinalApproval() {
        initOfficer(yek = true)
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        testMockMvc.perform(
            multipart(
                "$ENDPOINT_BASE_URL$TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .param("keskenerainen", "false")
                .param("virkailijanYhteenveto", "YEK-tarkistus valmis")
                .with { it.method = "PUT"; it }
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.valmistumispyynto.virkailijanKuittausaika")
                    .value(LocalDate.now().toString())
            )

        em.flush()
        em.clear()
        val updated = valmistumispyyntoRepository.findById(valmistumispyynto.id!!).orElseThrow()
        assertThat(updated.virkailija?.id).isEqualTo(virkailija.id)
        assertThat(updated.virkailijanKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updated.opintooikeus?.erikoisala?.id).isEqualTo(YEK_ERIKOISALA_ID)
        assertThat(updated.valmistumispyynnonTarkistus?.virkailijanYhteenveto)
            .isEqualTo("YEK-tarkistus valmis")
    }

    @Test
    fun getValmistumispyynnonAsiakirjaReturnsDocumentForOfficerFromSameUniversity() {
        initOfficer()
        val asiakirja = persistAsiakirja()
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
            .apply { yhteenvetoAsiakirja = asiakirja }
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
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF_VALUE))
            .andExpect(content().bytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA))
    }

    @Test
    fun getValmistumispyynnonAsiakirjaReturnsNotFoundForOfficerFromDifferentUniversity() {
        initOfficer(Yliopisto(nimi = YliopistoEnum.TURUN_YLIOPISTO))
        val asiakirja = persistAsiakirja()
        val valmistumispyynto = ValmistumispyyntoHelper
            .createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
            .apply { yhteenvetoAsiakirja = asiakirja }
        em.persist(valmistumispyynto)
        em.flush()

        testMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL/valmistumispyynto/{valmistumispyyntoId}/asiakirja/{asiakirjaId}",
                valmistumispyynto.id,
                asiakirja.id
            )
        ).andExpect(status().isNotFound)
    }

    private fun initOfficer(officerUniversity: Yliopisto? = null, yek: Boolean = false) {
        val officerUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(OPINTOHALLINNON_VIRKAILIJA)
        )
        em.persist(officerUser)
        setSecurityContext(requireNotNull(officerUser.id), OPINTOHALLINNON_VIRKAILIJA)

        erikoistuvaLaakari = initErikoistuvaLaakari()
        opintooikeus = if (yek) {
            OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuvaLaakari)
                .also {
                    OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, it)
                }
        } else {
            erikoistuvaLaakari.getOpintooikeusKaytossa()!!
        }

        val reviewerRole = if (yek) {
            VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN
        } else {
            VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
        }
        vastuuhenkilo = createReviewer(reviewerRole)

        virkailija = KayttajaHelper.createEntity(em, officerUser)
        officerUniversity?.let { em.persist(it) }
        virkailija.yliopistot.add(officerUniversity ?: opintooikeus.yliopisto!!)
        em.persist(virkailija)
        em.flush()
    }

    private fun createReviewer(role: VastuuhenkilonTehtavatyyppiEnum) =
        KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
            .also { em.persist(it) }
            .let { user ->
                KayttajaHelper.createEntity(em, user).apply {
                    yliopistotAndErikoisalat.add(
                        KayttajaYliopistoErikoisala(
                            kayttaja = this,
                            yliopisto = opintooikeus.yliopisto,
                            erikoisala = opintooikeus.erikoisala,
                            vastuuhenkilonTehtavat = em.findAll(
                                VastuuhenkilonTehtavatyyppi::class
                            ).filter { it.nimi == role }.toMutableSet()
                        )
                    )
                    em.persist(this)
                }
            }

    private fun persistAsiakirja(): Asiakirja {
        return Asiakirja(
            opintooikeus = opintooikeus,
            nimi = AsiakirjaHelper.ASIAKIRJA_PDF_NIMI,
            tyyppi = MediaType.APPLICATION_PDF_VALUE,
            lisattypvm = LocalDateTime.now(),
            asiakirjaData = AsiakirjaData(data = AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        ).also { em.persist(it) }
    }
}
