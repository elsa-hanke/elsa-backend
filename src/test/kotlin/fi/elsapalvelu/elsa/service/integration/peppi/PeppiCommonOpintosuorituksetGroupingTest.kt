package fi.elsapalvelu.elsa.service.integration.peppi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PeppiCommonOpintosuorituksetGroupingTest {

    @Test
    fun `multiple accomplishments sharing course code merge into national exam as primary with subparts as osakokonaisuudet`() {
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(ETU_SUKU_PAYLOAD))
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

            // Yksi ELOP0001-suoritus ja yksi MOJO2008-suoritus
            assertThat(items).hasSize(2)

            val elop = items.single { it.kurssikoodi == "ELOP0001" }
            assertThat(elop.yliopistoOpintooikeusId).isEqualTo("2200516")
            assertThat(elop.suorituspaiva).isEqualTo(LocalDate.of(2026, 5, 7))
            assertThat(elop.hyvaksytty).isTrue

            // Pääsuorituksen nimenä on nimenomaan valtakunnallinen erikoislääkärikuulustelu (eikä osasuoritus)
            assertThat(elop.nimi_fi).isEqualTo("VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU")
            assertThat(elop.nimi_sv).isEqualTo("VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU")

            // Osasuoritukset on liitetty osakokonaisuuksiksi
            assertThat(elop.osakokonaisuudet).hasSize(2)
            val osakokonaisuudet = elop.osakokonaisuudet!!

            assertThat(osakokonaisuudet.map { it.nimi_fi })
                .containsExactlyInAnyOrder("Patologia, esseet", "Patologia, preparaatit")

            // Osakokonaisuuksien kurssikoodeina käytetään lähdejärjestelmän id:tä, jotta ne pysyvät yksilöllisinä
            assertThat(osakokonaisuudet.map { it.kurssikoodi })
                .containsExactlyInAnyOrder("7355043", "7355044")

            assertThat(osakokonaisuudet).allSatisfy {
                assertThat(it.hyvaksytty).isTrue
                assertThat(it.suorituspaiva).isEqualTo(LocalDate.of(2026, 5, 7))
            }

            // Tavallinen kurssi ilman osasuorituksia säilyy muuttumattomana
            val mojo = items.single { it.kurssikoodi == "MOJO2008" }
            assertThat(mojo.nimi_fi).isEqualTo("ESIMIES TYÖHYVINVOINNIN JOHTAJANA")
            assertThat(mojo.opintopisteet).isEqualTo(2.0)
            assertThat(mojo.osakokonaisuudet).isNull()
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `failed attempts without date do not block passed accomplishments and passed national exam is primary`() {
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(ETU1_SUKU1_PAYLOAD))
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

            // Kolme päivätöntä/hylättyä yritystä kulkevat läpi sellaisenaan yksittäisinä suorituksina
            val undated = items.filter { it.suorituspaiva == null }
            assertThat(undated).hasSize(3)
            assertThat(undated).allSatisfy { assertThat(it.osakokonaisuudet).isNull() }

            // Päivätyt hyväksytyt suoritukset yhdistyvät yhdeksi suoritukseksi, jonka pääsuorituksena on kuulustelu
            val passed = items.filter { it.suorituspaiva != null }
            assertThat(passed).hasSize(1)

            val merged = passed.single()
            assertThat(merged.nimi_fi).isEqualTo("VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU")
            assertThat(merged.nimi_sv).isEqualTo("VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU")
            assertThat(merged.hyvaksytty).isTrue
            assertThat(merged.osakokonaisuudet).hasSize(2)

            val osakokonaisuusNimet = merged.osakokonaisuudet!!.map { it.nimi_fi }
            assertThat(osakokonaisuusNimet).containsExactlyInAnyOrder("Patologia, esseet", "Patologia, preparaatit")
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `passed accomplishment is preferred as primary over later failed attempt`() {
        val body = """
            [
              {
                "studyEntitlementId": "12345",
                "studyEntitlementKey": "key-1",
                "id": "1001",
                "kurssiKoodi": "TEST0001",
                "suoritusPvm": "2026-05-01T00:00:00",
                "opintopisteet": 5.0,
                "nimi": {"fi": "Testikurssi", "sv": "Testkurs", "en": "Test Course"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "12345",
                "studyEntitlementKey": "key-1",
                "id": "1002",
                "kurssiKoodi": "TEST0001",
                "suoritusPvm": "2026-10-01T00:00:00",
                "opintopisteet": 5.0,
                "nimi": {"fi": "Testikurssi", "sv": "Testkurs", "en": "Test Course"},
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
                        YliopistoEnum.TURUN_YLIOPISTO
                    )
            }

            val items = result!!.items!!
            assertThat(items).hasSize(1)

            val course = items.single()
            assertThat(course.hyvaksytty).isTrue
            assertThat(course.suorituspaiva).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(course.osakokonaisuudet).hasSize(1)
            assertThat(course.osakokonaisuudet!!.single().hyvaksytty).isFalse
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `later completion date is selected as primary among passed attempts of same course`() {
        val body = """
            [
              {
                "studyEntitlementId": "12345",
                "studyEntitlementKey": "key-1",
                "id": "2001",
                "kurssiKoodi": "TEST0002",
                "suoritusPvm": "2025-05-01T00:00:00",
                "opintopisteet": 3.0,
                "nimi": {"fi": "Kurssi A", "sv": "Kurs A", "en": "Course A"},
                "hyvaksytty": true,
                "arvio": {"fi": "1", "sv": "1", "en": "1"}
              },
              {
                "studyEntitlementId": "12345",
                "studyEntitlementKey": "key-1",
                "id": "2002",
                "kurssiKoodi": "TEST0002",
                "suoritusPvm": "2026-06-01T00:00:00",
                "opintopisteet": 3.0,
                "nimi": {"fi": "Kurssi A", "sv": "Kurs A", "en": "Course A"},
                "hyvaksytty": true,
                "arvio": {"fi": "4", "sv": "4", "en": "4"}
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

            val items = result!!.items!!
            assertThat(items).hasSize(1)

            val course = items.single()
            assertThat(course.suorituspaiva).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(course.arvio_fi).isEqualTo("4")
            assertThat(course.osakokonaisuudet).hasSize(1)
            assertThat(course.osakokonaisuudet!!.single().suorituspaiva).isEqualTo(LocalDate.of(2025, 5, 1))
        } finally {
            server.shutdown()
        }
    }

    private companion object {
        const val TEST_HETU = "010190-1234"

        val ETU_SUKU_PAYLOAD = """
            [
              {
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7275191",
                "kurssiKoodi": "MOJO2008",
                "suoritusPvm": "2026-04-02T00:00:00",
                "opintopisteet": 2.0,
                "nimi": {
                  "fi": "ESIMIES TYÖHYVINVOINNIN JOHTAJANA",
                  "sv": "ESIMIES TYÖHYVINVOINNIN JOHTAJANA",
                  "en": "Manager as Promoter of Well-being at Work"
                },
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7355043",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, esseet", "sv": "", "en": "Pathology, essays"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7355042",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU",
                  "sv": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU",
                  "en": "National Examination (Specialist Training in Medicine)"
                },
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7355044",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, preparaatit", "sv": "", "en": "Pathology, specimens"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              }
            ]
        """.trimIndent()

        val ETU1_SUKU1_PAYLOAD = """
            [
              {
                "studyEntitlementId": "111713",
                "studyEntitlementKey": "76383_269350_T",
                "id": "7170505",
                "kurssiKoodi": "ELOP0001",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, preparaatit", "sv": "", "en": "Pathology, specimens"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail", "en": "Fail"}
              },
              {
                "studyEntitlementId": "111713",
                "studyEntitlementKey": "76383_269350_T",
                "id": "7170504",
                "kurssiKoodi": "ELOP0001",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, esseet", "sv": "", "en": "Pathology, essays"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail", "en": "Fail"}
              },
              {
                "studyEntitlementId": "111713",
                "studyEntitlementKey": "76383_269350_T",
                "id": "7355038",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, esseet", "sv": "", "en": "Pathology, essays"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "111713",
                "studyEntitlementKey": "76383_269350_T",
                "id": "7355039",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "Patologia, preparaatit", "sv": "", "en": "Pathology, specimens"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "111713",
                "studyEntitlementKey": "76383_269350_T",
                "id": "7166265",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {"fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "sv": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "en": "National Examination"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass", "en": "Pass"}
              },
              {
                "studyEntitlementId": "111713",
                "studyEntitlementKey": "76383_269350_T",
                "id": "6889776",
                "kurssiKoodi": "ELOP0001",
                "opintopisteet": 0.0,
                "nimi": {"fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "sv": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU", "en": "National Examination"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail", "en": "Fail"}
              }
            ]
        """.trimIndent()
    }
}
