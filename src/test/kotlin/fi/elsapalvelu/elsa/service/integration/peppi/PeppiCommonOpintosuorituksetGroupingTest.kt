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
        // Todellinen, anonymisoitu tuotantovastaus (Etu Suku): 10 suoritusta, joista 3 jakaa kurssikoodin ELOP0001
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
            assertThat(elop.yliopistoOpintooikeusId).isEqualTo("2200516")
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
                .containsExactlyInAnyOrder("7355043", "7355044")

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
        // Todellinen, anonymisoitu tuotantovastaus (Etu1 Suku1): 17 suoritusta, joista 6 jakaa kurssikoodin ELOP0001.
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
                "nimi": {"fi": "Testikurssi", "sv": "Testkurs"},
                "hyvaksytty": true,
                "arvio": {"fi": "Hyväksytty", "sv": "Pass"}
              },
              {
                "studyEntitlementId": "12345",
                "studyEntitlementKey": "key-1",
                "id": "1002",
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
                "nimi": {"fi": "Kurssi A", "sv": "Kurs A"},
                "hyvaksytty": true,
                "arvio": {"fi": "1", "sv": "1"}
              },
              {
                "studyEntitlementId": "12345",
                "studyEntitlementKey": "key-1",
                "id": "2002",
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
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7355043",
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
                "arvio": {
                  "fi": "Hyväksytty",
                  "sv": "Pass",
                  "en": "Pass"
                }
              },
              {
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7355044",
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
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7068983",
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
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "7139985",
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
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "6451150",
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
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "6314489",
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
                "studyEntitlementId": "466408",
                "studyEntitlementKey": "2200516",
                "id": "6418192",
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
                "studyEntitlementId": "179015",
                "studyEntitlementKey": "512111_299129_T",
                "id": "6166870",
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
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7248745","kurssiKoodi":"MOJO2037","suoritusPvm":"2026-03-02T00:00:00","opintopisteet":2.0,"nimi":{"fi":"Lääketieteellinen päätöksenteko ja terveydenhuollon laadunhallinta","sv":"Lääketieteellinen päätöksenteko ja terveydenhuollon laadunhallinta","en":"Medical Decision Making and Quality Management in health care"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7170505","kurssiKoodi":"ELOP0001","opintopisteet":0.0,"nimi":{"fi":"Patologia, preparaatit","sv":"","en":"Pathology, specimens"},"hyvaksytty":false,"arvio":{"fi":"Hylätty","sv":"Fail","en":"Fail"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7170504","kurssiKoodi":"ELOP0001","opintopisteet":0.0,"nimi":{"fi":"Patologia, esseet","sv":"","en":"Pathology, essays"},"hyvaksytty":false,"arvio":{"fi":"Hylätty","sv":"Fail","en":"Fail"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7355038","kurssiKoodi":"ELOP0001","suoritusPvm":"2026-05-07T00:00:00","opintopisteet":0.0,"nimi":{"fi":"Patologia, esseet","sv":"","en":"Pathology, essays"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7391358","kurssiKoodi":"ELOP0004","suoritusPvm":"2026-09-01T00:00:00","opintopisteet":290.0,"nimi":{"fi":"Erikoislääkärin pätevyys","sv":"Erikoislääkärin pätevyys","en":"Medical Specialist Qualification"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7355039","kurssiKoodi":"ELOP0001","suoritusPvm":"2026-05-07T00:00:00","opintopisteet":0.0,"nimi":{"fi":"Patologia, preparaatit","sv":"","en":"Pathology, specimens"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7166265","kurssiKoodi":"ELOP0001","suoritusPvm":"2026-05-07T00:00:00","opintopisteet":0.0,"nimi":{"fi":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","sv":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","en":"National Examination (Specialist Training in Medicine)"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7336509","kurssiKoodi":"MOJO9001","suoritusPvm":"2017-02-13T00:00:00","opintopisteet":2.0,"nimi":{"fi":"AIEMMIN SUORITETUT TEOREETTISET MEDIMERCOPINNOT","sv":"AIEMMIN SUORITETUT TEOREETTISET MEDIMERCOPINNOT","en":"Previous Theoretical MediMerc Courses"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7167139","kurssiKoodi":"MOJO2033","suoritusPvm":"2025-11-21T00:00:00","opintopisteet":2.0,"nimi":{"fi":"Oikeudellisia lähtökohtia terveydenhuollon ammattilaisille","sv":"Oikeudellisia lähtökohtia terveydenhuollon ammattilaisille","en":"Health Professional’s Judicial Knowledge"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111711","studyEntitlementKey":"76383_219591_T","id":"4652656","kurssiKoodi":"YLEE0022","suoritusPvm":"2017-04-12T00:00:00","opintopisteet":0.0,"nimi":{"fi":"MITEN TUEN PÄRJÄÄMISTÄ TYÖSSÄ JA KOTONA, 21.3.2017 (8 TUNTIA)","sv":"MITEN TUEN PÄRJÄÄMISTÄ TYÖSSÄ JA KOTONA, 21.3.2017 (8 TUNTIA)","en":"How to Support People to Get Along Home and in the Work, 21.3.2017 (8 h)"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111712","studyEntitlementKey":"76383_238322_T","id":"4740060","kurssiKoodi":"PGS_1788","suoritusPvm":"2018-03-22T00:00:00","opintopisteet":0.0,"nimi":{"fi":"GERIATRIAN SYMPOSIUM 6 T","sv":"GERIATRIAN SYMPOSIUM 6 T","en":"Geriatric Symposium 6 H"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7378135","kurssiKoodi":"EL-2009-MOJO1000","suoritusPvm":"2026-08-25T00:00:00","opintopisteet":10.0,"nimi":{"fi":"Erikoistuvan lääkärin/hammaslääkärin johtamisopinnot","sv":"Management studies in medical/dental specialist training","en":"Management studies in medical/dental specialist training"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"7378132","kurssiKoodi":"MOJO4002","suoritusPvm":"2026-08-25T00:00:00","opintopisteet":2.0,"nimi":{"fi":"ERIKOISTUVAN JOHTAMISOPINTOJEN PROJEKTITYÖ","sv":"ERIKOISTUVAN JOHTAMISOPINTOJEN PROJEKTITYÖ","en":"Project Work, Management Studies for Doctors in Specialist Training"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"6511750","kurssiKoodi":"YLÄÄ0057","suoritusPvm":"2023-05-08T00:00:00","opintopisteet":0.0,"nimi":{"fi":"TERVEYSKESKUSKOULUTUS 9 KUUKAUTTA","sv":"TERVEYSKESKUSKOULUTUS 9 KUUKAUTTA","en":"Health Center Training Period, 9 months"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"6889776","kurssiKoodi":"ELOP0001","opintopisteet":0.0,"nimi":{"fi":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","sv":"VALTAKUNNALLINEN ERIKOISLÄÄKÄRIKUULUSTELU","en":"National Examination (Specialist Training in Medicine)"},"hyvaksytty":false,"arvio":{"fi":"Hylätty","sv":"Fail","en":"Fail"}},
              {"studyEntitlementId":"111711","studyEntitlementKey":"76383_219591_T","id":"4615873","kurssiKoodi":"YLEE0021","suoritusPvm":"2016-11-30T00:00:00","opintopisteet":0.0,"nimi":{"fi":"SAIRAUSVAKUUTUS, 30.11.2016, 8 TUNTIA","sv":"SAIRAUSVAKUUTUS, 30.11.2016, 8 TUNTIA","en":"Health Insurance, 30.11.2016, 8 Hours"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}},
              {"studyEntitlementId":"111713","studyEntitlementKey":"76383_269350_T","id":"6943216","kurssiKoodi":"MOJO2027","suoritusPvm":"2025-01-09T00:00:00","opintopisteet":2.0,"nimi":{"fi":"AJANKÄYTÖN HALLINNALLA HYVINVOINTIIN","sv":"AJANKÄYTÖN HALLINNALLA HYVINVOINTIIN","en":"Time management"},"hyvaksytty":true,"arvio":{"fi":"Hyväksytty","sv":"Pass","en":"Pass"}}
            ]
        """.trimIndent()
    }
}
