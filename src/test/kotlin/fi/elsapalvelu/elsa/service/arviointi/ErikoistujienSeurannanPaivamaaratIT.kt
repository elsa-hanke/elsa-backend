package fi.elsapalvelu.elsa.service.arviointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import jakarta.persistence.EntityManager
import java.time.Clock
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ErikoistujienSeurannanPaivamaaratIT {
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var palvelu: ErikoistujienSeurantaQueryService
    @MockitoBean private lateinit var clock: Clock

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `seuranta sisaltaa tanaan paattyvan oikeuden ja huomioi katseluajan`(aika: KellonTestitapaus) {
        asetaKello(aika)
        val tanaan = luoOikeus(aika.paiva)
        val eilinen = luoOikeus(aika.paiva.minusDays(1))
        val katseluPaattynyt = luoOikeus(aika.paiva.plusDays(1)).apply { viimeinenKatselupaiva = aika.paiva.minusDays(1) }
        em.flush()
        val criteria = ErikoistujanEteneminenCriteria()

        val voimassaolevat = palvelu.findByCriteriaAndYliopistoIdAndErikoisalaId(criteria, PageRequest.of(0, 20),
            tanaan.yliopisto!!.id!!, tanaan.erikoisala!!.id!!, "fi").content
        criteria.naytaPaattyneet = true
        val kaikki = palvelu.findByCriteriaAndYliopistoIdAndErikoisalaId(criteria, PageRequest.of(0, 20),
            tanaan.yliopisto!!.id!!, tanaan.erikoisala!!.id!!, "fi").content

        assertThat(voimassaolevat).contains(tanaan).doesNotContain(eilinen, katseluPaattynyt)
        assertThat(kaikki).contains(tanaan, eilinen).doesNotContain(katseluPaattynyt)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `kouluttajan seuranta huomioi molemmat valtuutusrajat ja myontamispaivan`(aika: KellonTestitapaus) {
        asetaKello(aika)
        val tanaan = luoOikeus(aika.paiva.plusDays(10))
        val kouluttaja = tanaan.erikoistuvaLaakari!!.kayttaja!!
        val oikeudet = listOf(tanaan, luoOikeus(aika.paiva.plusDays(10)), luoOikeus(aika.paiva.plusDays(10)), luoOikeus(aika.paiva.plusDays(10)))
        oikeudet.forEachIndexed { indeksi, oikeus ->
            oikeus.opintooikeudenMyontamispaiva = if (indeksi == 3) aika.paiva.plusDays(1) else aika.paiva
            em.persist(Kouluttajavaltuutus(valtuutettu = kouluttaja, valtuuttajaOpintooikeus = oikeus,
                alkamispaiva = if (indeksi == 1) aika.paiva.plusDays(1) else aika.paiva.minusDays(1),
                valtuutuksenLuontiaika = aika.aikaleima(), valtuutuksenMuokkausaika = aika.aikaleima(),
                paattymispaiva = if (indeksi == 2) aika.paiva.minusDays(1) else aika.paiva))
        }
        em.flush()

        val tulos = palvelu.findByKouluttajaValtuutus(ErikoistujanEteneminenCriteria(), PageRequest.of(0, 20), kouluttaja.id!!)

        assertThat(tulos.content).containsExactly(tanaan)
    }

    private fun asetaKello(aika: KellonTestitapaus) {
        whenever(clock.instant()).thenReturn(aika.aikaleima())
        whenever(clock.zone).thenReturn(aika.kello().zone)
    }

    private fun luoOikeus(paattymispaiva: LocalDate): Opintooikeus = ErikoistuvaLaakariHelper.createEntity(em,
        opintooikeudenAlkamispaiva = paattymispaiva.minusYears(1), opintooikeudenPaattymispaiva = paattymispaiva).getOpintooikeusKaytossa()!!
}
