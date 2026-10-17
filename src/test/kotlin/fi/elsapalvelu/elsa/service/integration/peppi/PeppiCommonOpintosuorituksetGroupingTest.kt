package fi.elsapalvelu.elsa.service.integration.peppi

import com.fasterxml.jackson.databind.DeserializationFeature
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

    private val objectMapper = jacksonObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @Test
    fun `multiple accomplishments sharing course code merge into national exam as primary with subparts as osakokonaisuudet`() {
        // Tuotantovastauksen rakennetta vastaava synteettinen testiaineisto (Etu Suku): 10 suoritusta, joista 3 jakaa kurssikoodin ELOP0001
        // samalla päivämäärällä (esseet, valtakunnallinen kuulustelu, preparaatit).
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(ETU_SUKU_PAYLOAD))
        }
        try {
            val result = runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(objectMapper)
                    .fetchOpintosuoritukset(
                        server.url("/attainments").toString(),
                        OkHttpClient(),
                        TEST_HETU,
                        YliopistoEnum.TURUN_YLIOPISTO
                    )
            }

            assertThat(result).isNotNull
            val items = result!!.items!!

            // Alkuperäisestä 10 suorituksesta 3 ELOP0001-suoritusta yhdistyy yhdeksi, joten kokonaismäärä on 8
            assertThat(items).hasSize(8)

            val elop = items.single { it.kurssikoodi == "ELOP0001" }
            assertThat(elop.yliopistoOpintooikeusId).isEqualTo("fixture-8")
            assertThat(elop.suorituspaiva).isEqualTo(LocalDate.of(2026, 5, 7))
            assertThat(elop.hyvaksytty).isTrue

            // Pääsuorituksen nimenä on nimenomaan valtakunnallinen erikoislääkärikuulustelu (eikä kumpikaan osasuoritus)
            assertThat(elop.nimi_fi).isEqualTo("VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU")
            assertThat(elop.nimi_sv).isEqualTo("VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU")

            // Molemmat osasuoritukset on liitetty osakokonaisuuksiksi
            assertThat(elop.osakokonaisuudet).hasSize(2)
            val osakokonaisuudet = elop.osakokonaisuudet!!

            assertThat(osakokonaisuudet.map { it.nimi_fi })
                .containsExactlyInAnyOrder("Patologia, esseet", "Patologia, preparaatit")

            // Osakokonaisuuksien kurssikoodeina käytetään lähdejärjestelmän id:tä, jotta ne pysyvät yksilöllisinä
            assertThat(osakokonaisuudet.map { it.kurssikoodi })
                .containsExactlyInAnyOrder("fixture-10", "fixture-12")

            assertThat(osakokonaisuudet).allSatisfy {
                assertThat(it.hyvaksytty).isTrue
                assertThat(it.suorituspaiva).isEqualTo(LocalDate.of(2026, 5, 7))
            }

            // Muut 7 suoritusta ovat säilyneet ennallaan ilman osakokonaisuuksia
            val muutKoodit = items.filter { it.kurssikoodi != "ELOP0001" }.map { it.kurssikoodi }
            assertThat(muutKoodit).containsExactlyInAnyOrder(
                "MOJO2008", "MOJO2038", "MOJO2012", "MOJO2028", "YLÄÄ0057", "ELOP0002", "YLEE0028"
            )
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `failed attempts without date do not block passed accomplishments and passed national exam is primary`() {
        // Tuotantovastauksen rakennetta vastaava synteettinen testiaineisto (Etu1 Suku1): 17 suoritusta, joista 6 jakaa kurssikoodin ELOP0001.
        // 3 on hylättyä ilman päivämäärää, ja 3 on hyväksyttyä päivämäärällä 2026-05-07.
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(ETU1_SUKU1_PAYLOAD))
        }
        try {
            val result = runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(objectMapper)
                    .fetchOpintosuoritukset(
                        server.url("/attainments").toString(),
                        OkHttpClient(),
                        TEST_HETU,
                        YliopistoEnum.TURUN_YLIOPISTO
                    )
            }

            assertThat(result).isNotNull
            val items = result!!.items!!

            // 17 suorituksesta 3 hyväksyttyä ELOP0001-suoritusta yhdistyy yhdeksi, 3 päivätöntä kulkee läpi,
            // joten kokonaismäärä on 17 - 3 + 1 = 15
            assertThat(items).hasSize(15)

            // Kolme päivätöntä/hylättyä yritystä kulkevat läpi sellaisenaan yksittäisinä suorituksina
            val undatedElop = items.filter { it.kurssikoodi == "ELOP0001" && it.suorituspaiva == null }
            assertThat(undatedElop).hasSize(3)
            assertThat(undatedElop).allSatisfy { assertThat(it.osakokonaisuudet).isNull() }

            // Päivätyt hyväksytyt suoritukset yhdistyvät yhdeksi suoritukseksi, jonka pääsuorituksena on kuulustelu
            val passedElop = items.filter { it.kurssikoodi == "ELOP0001" && it.suorituspaiva != null }
            assertThat(passedElop).hasSize(1)

            val merged = passedElop.single()
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
    fun `ordinary course repeat attempts retain the original flat mapping`() {
        val body = """
            [
              {
                "studyEntitlementId": "fixture-1",
                "studyEntitlementKey": "fixture-2",
                "id": "fixture-3",
                "kurssiKoodi": "TEST0001",
                "suoritusPvm": "2026-05-01T00:00:00",
                "opintopisteet": 5.0,
                "nimi": {"fi": "Testikurssi", "sv": "Testkurs"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass"}
              },
              {
                "studyEntitlementId": "fixture-1",
                "studyEntitlementKey": "fixture-2",
                "id": "fixture-4",
                "kurssiKoodi": "TEST0001",
                "suoritusPvm": "2026-10-01T00:00:00",
                "opintopisteet": 5.0,
                "nimi": {"fi": "Testikurssi", "sv": "Testkurs"},
                "hyvaksytty": false,
                "arvio": {"fi": "Hylätty", "sv": "Fail"}
              }
            ]
        """.trimIndent()

        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(body))
        }
        try {
            val result = runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(objectMapper)
                    .fetchOpintosuoritukset(
                        server.url("/attainments").toString(),
                        OkHttpClient(),
                        TEST_HETU,
                        YliopistoEnum.TURUN_YLIOPISTO
                    )
            }

            val items = result!!.items!!
            assertThat(items).hasSize(2)
            assertThat(items.map { it.hyvaksytty }).containsExactly(true, false)
            assertThat(items).allSatisfy { assertThat(it.osakokonaisuudet).isNull() }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `ordinary course completions preserve response order without creating subparts`() {
        val body = """
            [
              {
                "studyEntitlementId": "fixture-1",
                "studyEntitlementKey": "fixture-2",
                "id": "fixture-5",
                "kurssiKoodi": "TEST0002",
                "suoritusPvm": "2025-05-01T00:00:00",
                "opintopisteet": 3.0,
                "nimi": {"fi": "Kurssi A", "sv": "Kurs A"},
                "hyvaksytty": true,
                "arvio": {"fi": "1", "sv": "1"}
              },
              {
                "studyEntitlementId": "fixture-1",
                "studyEntitlementKey": "fixture-2",
                "id": "fixture-6",
                "kurssiKoodi": "TEST0002",
                "suoritusPvm": "2026-06-01T00:00:00",
                "opintopisteet": 3.0,
                "nimi": {"fi": "Kurssi A", "sv": "Kurs A"},
                "hyvaksytty": true,
                "arvio": {"fi": "4", "sv": "4"}
              }
            ]
        """.trimIndent()

        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody(body))
        }
        try {
            val result = runBlocking {
                PeppiCommonOpintosuorituksetFetchingServiceImpl(objectMapper)
                    .fetchOpintosuoritukset(
                        server.url("/attainments").toString(),
                        OkHttpClient(),
                        TEST_HETU,
                        YliopistoEnum.TURUN_YLIOPISTO
                    )
            }

            val items = result!!.items!!
            assertThat(items).hasSize(2)
            assertThat(items.map { it.arvio_fi }).containsExactly("1", "4")
            assertThat(items).allSatisfy { assertThat(it.osakokonaisuudet).isNull() }
        } finally {
            server.shutdown()
        }
    }

    private companion object {
        const val TEST_HETU = "010190-1234"

        val ETU_SUKU_PAYLOAD = """
            [
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-9",
                "kurssiKoodi": "MOJO2008",
                "suoritusPvm": "2026-04-02T00:00:00",
                "opintopisteet": 2.0,
                "nimi": {
                  "fi": "ESIMIES TYÖHYVINVOINNIN JOHTAJANA",
                  "sv": "ESIMIES TYÖHYVINVOINNIN JOHTAJANA",
                  "en": "Manager as Promoter of Well-being at Work"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-10",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "Patologia, esseet",
                  "sv": "",
                  "en": "Pathology, essays"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-11",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU",
                  "sv": "VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU",
                  "en": "National Examination (Specialist Training in Medicine)"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-12",
                "kurssiKoodi": "ELOP0001",
                "suoritusPvm": "2026-05-07T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "Patologia, preparaatit",
                  "sv": "",
                  "en": "Pathology, specimens"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-13",
                "kurssiKoodi": "MOJO2038",
                "suoritusPvm": "2025-06-03T00:00:00",
                "opintopisteet": 2.0,
                "nimi": {
                  "fi": "Potilasturvallisuuden merimerkit erikoistuville",
                  "sv": "Potilasturvallisuuden merimerkit erikoistuville",
                  "en": "Patient Safety Seamarks for Doctors in specialist training"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-14",
                "kurssiKoodi": "MOJO2012",
                "suoritusPvm": "2025-10-16T00:00:00",
                "opintopisteet": 2.0,
                "nimi": {
                  "fi": "TALOUSSUUNNITTELU JA KUSTANNUSJOHTAMINEN SOSIAALI- JA TERVEYDENHUOLLOSSA",
                  "sv": "TALOUSSUUNNITTELU JA KUSTANNUSJOHTAMINEN SOSIAALI- JA TERVEYDENHUOLLOSSA",
                  "en": "Financial Planning and Cost Management"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-15",
                "kurssiKoodi": "MOJO2028",
                "suoritusPvm": "2023-02-28T00:00:00",
                "opintopisteet": 2.0,
                "nimi": {
                  "fi": "TYÖN ILO JA ITSENSÄ KEHITTÄMINEN",
                  "sv": "TYÖN ILO JA ITSENSÄ KEHITTÄMINEN",
                  "en": "Happiness at Work and Self-development"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-16",
                "kurssiKoodi": "YLÄÄ0057",
                "suoritusPvm": "2022-08-08T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "TERVEYSKESKUSKOULUTUS 9 KUUKAUTTA",
                  "sv": "TERVEYSKESKUSKOULUTUS 9 KUUKAUTTA",
                  "en": "Health Center Training Period, 9 months"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-7",
                "studyEntitlementKey": "fixture-8",
                "id": "fixture-17",
                "kurssiKoodi": "ELOP0002",
                "suoritusPvm": "2022-12-20T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "KOEJAKSO, 6 KUUKAUTTA",
                  "sv": "KOEJAKSO, 6 KUUKAUTTA",
                  "en": "Trial period, 6 months"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "fixture-18",
                "studyEntitlementKey": "fixture-19",
                "id": "fixture-20",
                "kurssiKoodi": "YLEE0028",
                "suoritusPvm": "2021-11-19T00:00:00",
                "opintopisteet": 0.0,
                "nimi": {
                  "fi": "SAIRAUSVAKUUTUS (8 TUNTIA)",
                  "sv": "SAIRAUSVAKUUTUS (8 TUNTIA)",
                  "en": "Health Insurance (8 hours)"
                },
                "hyvaksytty": true,
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              }
            ]
        """.trimIndent()

        val ETU1_SUKU1_PAYLOAD = """
            [
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-23","kurssiKoodi":"MOJO2037","suoritusPvm":"2026-03-02T00:00:00","opintopisteet":2.0,"nimi":{"fi":"Lääketieteellinen päätöksenteko ja terveydenhuollon laadunhallinta","sv":"Lääketieteellinen päätöksenteko ja terveydenhuollon laadunhallinta","en":"Medical Decision Making and Quality Management in health care"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-24","kurssiKoodi":"ELOP0001","opintopisteet":0.0,"nimi":{"fi":"Patologia, preparaatit","sv":"","en":"Pathology, specimens"},"hyvaksytty":false,"arvio":{"fi":"Hylätty","sv":"Fail","en":"Fail"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-25","kurssiKoodi":"ELOP0001","opintopisteet":0.0,"nimi":{"fi":"Patologia, esseet","sv":"","en":"Pathology, essays"},"hyvaksytty":false,"arvio":{"fi":"Hylätty","sv":"Fail","en":"Fail"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-26","kurssiKoodi":"ELOP0001","suoritusPvm":"2026-05-07T00:00:00","opintopisteet":0.0,"nimi":{"fi":"Patologia, esseet","sv":"","en":"Pathology, essays"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-27","kurssiKoodi":"ELOP0004","suoritusPvm":"2026-09-01T00:00:00","opintopisteet":290.0,"nimi":{"fi":"Erikoislääkärin pätevyys","sv":"Erikoislääkärin pätevyys","en":"Medical Specialist Qualification"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-28","kurssiKoodi":"ELOP0001","suoritusPvm":"2026-05-07T00:00:00","opintopisteet":0.0,"nimi":{"fi":"Patologia, preparaatit","sv":"","en":"Pathology, specimens"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-29","kurssiKoodi":"ELOP0001","suoritusPvm":"2026-05-07T00:00:00","opintopisteet":0.0,"nimi":{"fi":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","sv":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","en":"National Examination (Specialist Training in Medicine)"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-30","kurssiKoodi":"MOJO9001","suoritusPvm":"2017-02-13T00:00:00","opintopisteet":2.0,"nimi":{"fi":"AIEMMIN SUORITETUT TEOREETTISET MEDIMERCOPINNOT","sv":"AIEMMIN SUORITETUT TEOREETTISET MEDIMERCOPINNOT","en":"Previous Theoretical MediMerc Courses"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-31","kurssiKoodi":"MOJO2033","suoritusPvm":"2025-11-21T00:00:00","opintopisteet":2.0,"nimi":{"fi":"Oikeudellisia lähtökohtia terveydenhuollon ammattilaisille","sv":"Oikeudellisia lähtökohtia terveydenhuollon ammattilaisille","en":"Health Professional’s Judicial Knowledge"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-32","studyEntitlementKey":"fixture-33","id":"fixture-34","kurssiKoodi":"YLEE0022","suoritusPvm":"2017-04-12T00:00:00","opintopisteet":0.0,"nimi":{"fi":"MITEN TUEN PÄRJÄÄMISTÄ TYÖSSÄ JA KOTONA, 21.3.2017 (8 TUNTIA)","sv":"MITEN TUEN PÄRJÄÄMISTÄ TYÖSSÄ JA KOTONA, 21.3.2017 (8 TUNTIA)","en":"How to Support People to Get Along Home and in the Work, 21.3.2017 (8 h)"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-35","studyEntitlementKey":"fixture-36","id":"fixture-37","kurssiKoodi":"PGS_1788","suoritusPvm":"2018-03-22T00:00:00","opintopisteet":0.0,"nimi":{"fi":"GERIATRIAN SYMPOSIUM 6 T","sv":"GERIATRIAN SYMPOSIUM 6 T","en":"Geriatric Symposium 6 H"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-38","kurssiKoodi":"EL-2009-MOJO1000","suoritusPvm":"2026-08-25T00:00:00","opintopisteet":10.0,"nimi":{"fi":"Erikoistuvan lääkärin/hammaslääkärin johtamisopinnot","sv":"Management studies in medical/dental specialist training","en":"Management studies in medical/dental specialist training"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-39","kurssiKoodi":"MOJO4002","suoritusPvm":"2026-08-25T00:00:00","opintopisteet":2.0,"nimi":{"fi":"ERIKOISTUVAN JOHTAMISOPINTOJEN PROJEKTITYÖ","sv":"ERIKOISTUVAN JOHTAMISOPINTOJEN PROJEKTITYÖ","en":"Project Work, Management Studies for Doctors in Specialist Training"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-40","kurssiKoodi":"YLÄÄ0057","suoritusPvm":"2023-05-08T00:00:00","opintopisteet":0.0,"nimi":{"fi":"TERVEYSKESKUSKOULUTUS 9 KUUKAUTTA","sv":"TERVEYSKESKUSKOULUTUS 9 KUUKAUTTA","en":"Health Center Training Period, 9 months"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-41","kurssiKoodi":"ELOP0001","opintopisteet":0.0,"nimi":{"fi":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","sv":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","en":"National Examination (Specialist Training in Medicine)"},"hyvaksytty":false,"arvio":{"fi":"Hylätty","sv":"Fail","en":"Fail"}},
              {"studyEntitlementId":"fixture-32","studyEntitlementKey":"fixture-33","id":"fixture-42","kurssiKoodi":"YLEE0021","suoritusPvm":"2016-11-30T00:00:00","opintopisteet":0.0,"nimi":{"fi":"SAIRAUSVAKUUTUS, 30.11.2016, 8 TUNTIA","sv":"SAIRAUSVAKUUTUS, 30.11.2016, 8 TUNTIA","en":"Health Insurance, 30.11.2016, 8 Hours"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"fixture-21","studyEntitlementKey":"fixture-22","id":"fixture-43","kurssiKoodi":"MOJO2027","suoritusPvm":"2025-01-09T00:00:00","opintopisteet":2.0,"nimi":{"fi":"AJANKÄYTÖN HALLINNALLA HYVINVOINTIIN","sv":"AJANKÄYTÖN HALLINNALLA HYVINVOINTIIN","en":"Time management"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}}
            ]
        """.trimIndent()
    }
}
