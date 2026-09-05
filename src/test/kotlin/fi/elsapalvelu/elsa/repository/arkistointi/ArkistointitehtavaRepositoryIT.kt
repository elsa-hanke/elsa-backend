package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.Arkistointitehtava
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointitehtavaAsiakirja
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointitehtavanTila
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ArkistointitehtavaRepositoryIT {

    @Autowired
    private lateinit var tehtavaRepository: ArkistointitehtavaRepository

    @Autowired
    private lateinit var asiakirjaRepository: ArkistointitehtavaAsiakirjaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `tehtava ja asiakirjaviite tallennetaan ja haetaan`() {
        val asiakirja = AsiakirjaHelper.createEntity(entityManager)
        entityManager.persist(asiakirja)
        val tehtava = uusiTehtava("tallennus-1")
        tehtava.createdDate = null
        tehtava.lastModifiedDate = null
        tehtava.lisaaAsiakirja(
            ArkistointitehtavaAsiakirja(
                asiakirja = asiakirja,
                asiakirjatyyppi = RecordType.YHTEENVETO,
                jarjestys = 0,
                tiedostonimi = "yhteenveto.pdf",
                sisaltotyyppi = "application/pdf",
                sha256 = "a".repeat(64)
            )
        )

        val tallennettu = tehtavaRepository.saveAndFlush(tehtava)
        entityManager.clear()

        val loydetty = tehtavaRepository.findByIdempotenssiavain("tallennus-1")
        val asiakirjat = asiakirjaRepository
            .findAllByArkistointitehtavaIdOrderByJarjestysAsc(tallennettu.id!!)

        assertThat(loydetty).isNotNull
        assertThat(loydetty?.createdDate).isNotNull
        assertThat(loydetty?.lastModifiedDate).isNotNull
        assertThat(asiakirjat).hasSize(1)
        assertThat(asiakirjat.single().asiakirja?.id).isEqualTo(asiakirja.id)
        assertThat(asiakirjat.single().arkistointitehtava?.id).isEqualTo(tallennettu.id)
    }

    @Test
    fun `samaa idempotenssiavainta ei voi tallentaa kahdesti`() {
        tehtavaRepository.saveAndFlush(uusiTehtava("sama-avain"))

        assertThrows<DataIntegrityViolationException> {
            tehtavaRepository.saveAndFlush(uusiTehtava("sama-avain"))
        }
    }

    @Test
    fun `paivitysaika paivittyy tehtavaa muutettaessa`() {
        val tehtava = tehtavaRepository.saveAndFlush(uusiTehtava("paivitysaika"))
        tehtava.lastModifiedDate = Instant.EPOCH
        tehtava.yrityskerrat = 1

        tehtavaRepository.saveAndFlush(tehtava)

        assertThat(tehtava.lastModifiedDate).isAfter(Instant.EPOCH)
    }

    @Test
    fun `kasittelykelpoisten tehtavien haku huomioi ajan ja vanhentuneen varauksen`() {
        val nyt = Instant.parse("2026-09-04T12:00:00Z")
        val odottava = uusiTehtava("odottava", seuraavaKasittelyaika = nyt.minusSeconds(1))
        val tuleva = uusiTehtava("tuleva", seuraavaKasittelyaika = nyt.plusSeconds(1))
        val vanhentunutVaraus = uusiTehtava(
            "vanhentunut-varaus",
            tila = ArkistointitehtavanTila.KASITTELYSSA,
            seuraavaKasittelyaika = nyt.minusSeconds(2),
            kasittelyvarausPaattyy = nyt.minusSeconds(1)
        )
        val voimassaOlevaVaraus = uusiTehtava(
            "voimassa-oleva-varaus",
            tila = ArkistointitehtavanTila.KASITTELYSSA,
            kasittelyvarausPaattyy = nyt.plusSeconds(1)
        )
        val lahetetty = uusiTehtava(
            "lahetetty",
            tila = ArkistointitehtavanTila.LAHETETTY,
            seuraavaKasittelyaika = nyt.minusSeconds(1)
        )
        tehtavaRepository.saveAllAndFlush(
            listOf(odottava, tuleva, vanhentunutVaraus, voimassaOlevaVaraus, lahetetty)
        )

        val loydetyt = tehtavaRepository.findKasittelykelpoiset(
            nyt = nyt,
            odottaa = ArkistointitehtavanTila.ODOTTAA,
            kasittelyssa = ArkistointitehtavanTila.KASITTELYSSA,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(loydetyt.map { it.idempotenssiavain })
            .containsExactly("vanhentunut-varaus", "odottava")
    }

    private fun uusiTehtava(
        idempotenssiavain: String,
        tila: ArkistointitehtavanTila = ArkistointitehtavanTila.ODOTTAA,
        seuraavaKasittelyaika: Instant = Instant.parse("2026-09-04T12:00:00Z"),
        kasittelyvarausPaattyy: Instant? = null
    ) = Arkistointitehtava(
        yliopisto = YliopistoEnum.TURUN_YLIOPISTO,
        asiatyyppi = CaseType.VALMISTUMINEN,
        tila = tila,
        metatiedot = "{\"asia\":\"valmistuminen\"}",
        metatietoversio = "1",
        idempotenssiavain = idempotenssiavain,
        seuraavaKasittelyaika = seuraavaKasittelyaika,
        kasittelyvarausPaattyy = kasittelyvarausPaattyy
    )
}
