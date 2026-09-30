package fi.elsapalvelu.elsa.service.integration.peppi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// Nämä testit ajavat vastauksen datan läpi ilman verkkoyhteyttä oikeaan Pepin testijärjestelmään
// (MockWebServer palvelee tuotannon JSON-muotoa vastaavaa, anonymisoitua vastausta), joten ne eivät
// ole AbstractOpintosuorituksetFetchingService-pohjaisia ulkoisia integraatiotestejä.
class PeppiCommonOpintosuorituksetGroupingTest {

    @Test
    fun `several accomplishments sharing entitlement and course code are merged instead of overwriting each other`() {
        // Anonymisoitu, tuotannon kaltainen vastaus: sama opinto-oikeus ja kurssikoodi (ELOP0001) esiintyy
        // kolmesti (esim. esseet, kuulustelu ja preparaatit), joilla on eri Pepin oma tunniste (id) mutta
        // sama suorituspäivä. Mukana on myös yksi tavallinen, ainutkertainen suoritus, jonka on pysyttävä
        // muuttumattomana.
        val body = """
            [
              {
                "studyEntitlementId": "900001",
                "studyEntitlementKey": "9900001",
                "id": "8000001",
                "kurssiKoodi": "MOJO1234",
                "suoritusPvm": "2026-03-02T00:00:00",
                "opintopisteet": 2.0,
                "nimi": {"fi": "Testikurssi", "sv": "", "en": "Test course"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "900001",
                "studyEntitlementKey": "9900001",
                "id": "9000001",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, esseet", "sv": "", "en": "Pathology, essays"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "900001",
                "studyEntitlementKey": "9900001",
                "id": "9000002",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "sv": "", "en": "National Examination"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "900001",
                "studyEntitlementKey": "9900001",
                "id": "9000003",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, preparaatit", "sv": "", "en": "Pathology, specimens"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              }
            ]
        """.trimIndent()

        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(body))
        }
        try {
            val result = runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                    .fetchOpintosuoritukset(
                        server.url("/attainments").toString(),
                        OkHttpClient(),
                        TEST_HETU,
                        YliopistoEnum.TURUN_YLIOPISTO
                    )
            }

            assertThat(result).isNotNull
            val items = result!!.items!!

            // Ei ylimääräisiä tai kadonneita ylätason suorituksia: yksi ELOP0001-suoritus ja yksi
            // muuttumaton MOJO1234-suoritus.
            assertThat(items).hasSize(2)

            val elop = items.single { it.kurssikoodi == "ELOP0001" }
            assertThat(elop.yliopistoOpintooikeusId).isEqualTo("9900001")
            assertThat(elop.osakokonaisuudet).hasSize(2)

            // Kaikkien kolmen alkuperäisen suorituksen nimet löytyvät joko pääsuoritukselta tai
            // osakokonaisuuksilta - mikään ei ylikirjoitu hiljaisesti.
            val allNames = setOf(elop.nimi_fi) + elop.osakokonaisuudet!!.map { it.nimi_fi }.toSet()
            assertThat(allNames).containsExactlyInAnyOrder(
                "Patologia, esseet",
                "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU",
                "Patologia, preparaatit"
            )

            // Osakokonaisuuksien kurssikoodina käytetään lähdejärjestelmän omaa tunnistetta (id), jotta ne
            // pysyvät samoina myöhemmillä tuontikerroilla - ei siis alkuperäistä, jaettua ELOP0001-koodia.
            val osakokonaisuusKoodit = elop.osakokonaisuudet!!.map { it.kurssikoodi }.toSet()
            assertThat(osakokonaisuusKoodit).hasSize(2)
            assertThat(osakokonaisuusKoodit).isSubsetOf("9000001", "9000002", "9000003")
            assertThat(osakokonaisuusKoodit).doesNotContain("ELOP0001")

            val muu = items.single { it.kurssikoodi == "MOJO1234" }
            assertThat(muu.nimi_fi).isEqualTo("Testikurssi")
            assertThat(muu.osakokonaisuudet).isNull()
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `failed attempts without a completion date do not block or merge with the passed accomplishments`() {
        // Anonymisoitu, tuotannon kaltainen vastaus: samalla opinto-oikeudella ja kurssikoodilla on sekä
        // hylättyjä yrityksiä ilman suorituspäivää että myöhemmin hyväksyttyjä, päivätty suorituksia.
        val body = """
            [
              {
                "studyEntitlementId": "900002",
                "studyEntitlementKey": "9900002",
                "id": "9100001",
                "kurssiKoodi": "ELOP0001",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, preparaatit", "sv": "", "en": "Pathology, specimens"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail", "en": "Fail"}
              },
              {
                "studyEntitlementId": "900002",
                "studyEntitlementKey": "9900002",
                "id": "9100002",
                "kurssiKoodi": "ELOP0001",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, esseet", "sv": "", "en": "Pathology, essays"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail", "en": "Fail"}
              },
              {
                "studyEntitlementId": "900002",
                "studyEntitlementKey": "9900002",
                "id": "9100003",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, esseet", "sv": "", "en": "Pathology, essays"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "900002",
                "studyEntitlementKey": "9900002",
                "id": "9100004",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, preparaatit", "sv": "", "en": "Pathology, specimens"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "900002",
                "studyEntitlementKey": "9900002",
                "id": "9100005",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "sv": "", "en": "National Examination"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "900002",
                "studyEntitlementKey": "9900002",
                "id": "9100006",
                "kurssiKoodi": "ELOP0001",
                "opintopisteet": 0.0,
                "nimi": {"fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "sv": "", "en": "National Examination"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail", "en": "Fail"}
              }
            ]
        """.trimIndent()

        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(body))
        }
        try {
            val result = runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                    .fetchOpintosuoritukset(
                        server.url("/attainments").toString(),
                        OkHttpClient(),
                        TEST_HETU,
                        YliopistoEnum.ITA_SUOMEN_YLIOPISTO
                    )
            }

            assertThat(result).isNotNull
            val items = result!!.items!!

            // Kolme päivätöntä/hylättyä yritystä kulkevat läpi sellaisenaan, yksittäisinä suorituksina -
            // tallennuspalvelu ohittaa ne edelleen puuttuvan suorituspäivän takia, aivan kuten ennen tätä
            // korjausta. Niitä EI yhdistetä hyväksyttyjen suoritusten kanssa.
            val undated = items.filter { it.suorituspaiva == null }
            assertThat(undated).hasSize(3)
            assertThat(undated).allSatisfy { assertThat(it.osakokonaisuudet).isNull() }

            // Kolme hyväksyttyä, päivättyä suoritusta yhdistyvät yhdeksi suoritukseksi, jonka
            // osakokonaisuuksina kaksi muuta säilyvät - mikään niistä ei katoa.
            val passed = items.filter { it.suorituspaiva != null }
            assertThat(passed).hasSize(1)

            val merged = passed.single()
            assertThat(merged.osakokonaisuudet).hasSize(2)
            val allPassedNames = setOf(merged.nimi_fi) + merged.osakokonaisuudet!!.map { it.nimi_fi }.toSet()
            assertThat(allPassedNames).containsExactlyInAnyOrder(
                "Patologia, esseet",
                "Patologia, preparaatit",
                "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU"
            )
            assertThat(merged.osakokonaisuudet!!).allSatisfy {
                assertThat(it.hyvaksytty).isTrue
                assertThat(it.suorituspaiva).isNotNull
            }
        } finally {
            server.shutdown()
        }
    }

    private companion object {
        const val TEST_HETU = "010190-1234"
    }
}
