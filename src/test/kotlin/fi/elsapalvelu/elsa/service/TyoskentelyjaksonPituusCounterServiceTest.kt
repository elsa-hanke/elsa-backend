package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.service.helpers.KeskeytysaikaMockHelper
import fi.elsapalvelu.elsa.service.helpers.TyoskentelyjaksoMockHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class TyoskentelyjaksonPituusCounterServiceTest {

    @Autowired
    private lateinit var tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService

    @Autowired
    private lateinit var tyoskentelyjaksoService: TyoskentelyjaksoService

    @Test
    fun `test calculate without osaaikaprosenti`() {
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(30L), 100, mutableSetOf()
        )
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso, 0.0)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(31.0)
    }

    @Test
    fun `test calculate with osaaikaprosentti 50`() {
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(58L), 50, mutableSetOf()
        )
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso, 0.0)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(29.5)
    }

    @Test
    fun `test calculate with one week keskeytysaika`() {
        val keskeytysaikaMock = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(15L),
            LocalDate.ofEpochDay(22L), 100
        )

        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(37L), 100, mutableSetOf(keskeytysaikaMock)
        )
        Mockito.`when`(keskeytysaikaMock.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(30.0)
    }

    @Test
    fun `test calculate with osaaikaprosentti 50 and keskeytysaika of one week`() {
        val keskeytysaikaMock = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(15L),
            LocalDate.ofEpochDay(22L), 100
        )
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(58L), 50, mutableSetOf(keskeytysaikaMock)
        )
        Mockito.`when`(keskeytysaikaMock.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(25.5)
    }

    @Test
    fun `test calculate with osaaikaprosentti 50 and keskeytysaika of one week with poissaoloprosentti 50`() {
        val keskeytysaikaMock = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(15L),
            LocalDate.ofEpochDay(22L), 50
        )
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(58L), 50, mutableSetOf(keskeytysaikaMock)
        )
        Mockito.`when`(keskeytysaikaMock.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        // Työskentelyjakson pituus 29,5 kokonaista päivää (osa-aikaprosentti 50). Keskeytysajan pituus 4 kokonaista
        // päivää (osa-aikaprosentti 50). Vähennetään puolikkaan työpäivän kestosta puolet. Eli kun keskeytysaika
        // sijoittuu 4 päivälle ja on suuruudeltaan 50% työpäivästä joka on 50%, vähennetään kunkin päivän osalta
        // työskentelyaikaa 25%. Näin ollen keskeytysajan oikea pituus on vain 2 kokonaista päivää.
        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(27.5)
    }

    @Test
    fun `test calculate with alkamispaiva in the future and paattymispaiva null`() {
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.now().plusDays(5),
            null, 100, mutableSetOf()
        )
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                0.0
            )

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(0.0)
    }

    @Test
    fun `test calculate alkamispaiva in the past and paattymispaiva null`() {
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.now().minusDays(5),
            null, 100, mutableSetOf()
        )
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                0.0
            )

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(6.0)
    }

    @Test
    fun `test calculate with alkamispaiva in the future and paattymispaiva in the future`() {
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(10), 100, mutableSetOf()
        )
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                0.0
            )

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(0.0)
    }

    @Test
    fun `test calculate alkamispaiva in the past and paattymispaiva in the future`() {
        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.now().minusDays(5),
            LocalDate.now().plusDays(5), 100, mutableSetOf()
        )
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                0.0
            )

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(6.0)
    }

    @Test
    fun `test getHyvaksiluettavatPerYearMap`() {
        val tyoskentelyjakso1 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(400L)
        )
        val tyoskentelyjakso2 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(600),
            LocalDate.ofEpochDay(745L), 50
        )
        val tyoskentelyjakso3 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(400L),
            LocalDate.ofEpochDay(750L), 50
        )
        val hyvaksiluettavatPerYearMap =
            tyoskentelyjaksonPituusCounterService.getHyvaksiluettavatPerYearMap(
                mutableListOf(
                    tyoskentelyjakso1,
                    tyoskentelyjakso2,
                    tyoskentelyjakso3
                )
            )

        assertThat(hyvaksiluettavatPerYearMap.size).isEqualTo(3)
        assertThat(hyvaksiluettavatPerYearMap[1970]).isEqualTo(30.0)
        assertThat(hyvaksiluettavatPerYearMap[1971]).isEqualTo(30.0)
        assertThat(hyvaksiluettavatPerYearMap[1972]).isEqualTo(30.0)
    }

    @Test
    fun `test calculate with type vahennetaan kerran`() {
        val poissaolonSyyMock = Mockito.mock(PoissaolonSyy::class.java)
        Mockito.`when`(poissaolonSyyMock.vahennystyyppi)
            .thenReturn(PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)
        Mockito.`when`(poissaolonSyyMock.vahennetaanKerran).thenReturn(true)

        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(355L),
            LocalDate.ofEpochDay(370L),
            100,
            poissaolonSyy = poissaolonSyyMock
        )
        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(700L),
            LocalDate.ofEpochDay(730L),
            100,
            poissaolonSyy = poissaolonSyyMock
        )

        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(740L), 100,
            mutableSetOf(keskeytysaikaMock1, keskeytysaikaMock2)
        )
        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        // Ensimmäinen kesketysaika 16 päivää ja toinen 31 päivää. Hyväksiluetaan vain kerran 30 päivää,
        // joten työskentelyjakson pituudesta (391 päivää) vähennetään 17 päivää.
        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(374.0)
    }

    @Test
    fun `test calculate with type vahennetaan ylimeneva osa per vuosi`() {
        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(355L),
            LocalDate.ofEpochDay(370L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(390L),
            LocalDate.ofEpochDay(410L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock3 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(600L),
            LocalDate.ofEpochDay(650L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock4 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(900L),
            LocalDate.ofEpochDay(910L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock5 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(990L),
            LocalDate.ofEpochDay(1025L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock6 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(1300L),
            LocalDate.ofEpochDay(1350L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(1360L), 100, mutableSetOf(
                keskeytysaikaMock1,
                keskeytysaikaMock2,
                keskeytysaikaMock3,
                keskeytysaikaMock4,
                keskeytysaikaMock5,
                keskeytysaikaMock6
            )
        )
        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock3.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock4.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock5.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock6.tyoskentelyjakso).thenReturn(tyoskentelyjakso)

        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        // Työskentelyjakson pituus 1011 päivää.
        // 1. poissaolo (ajoittuu kahdelle vuodelle) ->  1970: 10 päivää, 1971: 6 päivää -> ei vähennä jakson pituutta
        // -> jäljellä olevia päiviä hyväksiluettavaksi vuonna 1971 jää 30 - 6 = 24 päivää.
        // 2. poissaolo -> 1971: 21 päivää. Hyväksiluetaan kokonaan. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle
        // 1971 jää 3 kpl.
        // 3. poissaolo -> 1971: 51 päivää. Tästä hyväksiluetaan 3 päivää. Jakson pitutta vähennetään 48 päivää.
        // 4. poissaolo > 1972: 11 päivää. Hyväksiluetaan kokonaan. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle
        // 1972 jää 19 kpl.
        // 5. poissaolo -> 1972: 36 päivää. Hyväksiluetaan 19 päivää. Jakson pituutta vähennetään 17 päivää.
        // 6. poissaolo -> 1973: 51 päivää. Hyväksiluetaan 30 päivää. Jakson pituutta vähennetään 21 päivää.
        // Työskentelyjakson pituus vähennysten jälkeen: 1011 - 48 - 17 - 21 = 925 päivää.
        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(925.0)
    }

    @Test
    fun `test calculate with type vahennetaan ylimeneva osa per vuosi with osa-aikaiset poissaolot`() {
        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(355L),
            LocalDate.ofEpochDay(370L),
            50,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(390L),
            LocalDate.ofEpochDay(410L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock3 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(600L),
            LocalDate.ofEpochDay(650L),
            25,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock4 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(900L),
            LocalDate.ofEpochDay(910L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock5 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(990L),
            LocalDate.ofEpochDay(1025L),
            66,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock6 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(1300L),
            LocalDate.ofEpochDay(1350L),
            80,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(1360L), 100, mutableSetOf(
                keskeytysaikaMock1,
                keskeytysaikaMock2,
                keskeytysaikaMock3,
                keskeytysaikaMock4,
                keskeytysaikaMock5,
                keskeytysaikaMock6
            )
        )

        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock3.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock4.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock5.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock6.tyoskentelyjakso).thenReturn(tyoskentelyjakso)

        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        // Työskentelyjakson pituus 1011 päivää.
        // 1. poissaolo (ajoittuu kahdelle vuodelle) ->  1970: 5 päivää (osa-aikaprosentti 50%), 1971: 3 päivää ->
        // ei vähennä jakson pituutta -> jäljellä olevia päiviä hyväksiluettavaksi vuonna 1971 jää 30 - 3 = 27 päivää.
        // 2. poissaolo -> 1971: 21 päivää. Hyväksiluetaan kokonaan. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle
        // 1971 jää 6 kpl.
        // 3. poissaolo -> 1971: 12,75 päivää (osa-aikaprosentti 25%). Tästä hyväksiluetaan 6 päivää. Jakson pitutta
        // vähennetään 6,75 päivää.
        // 4. poissaolo > 1972: 11 päivää. Hyväksiluetaan kokonaan. Jäljellä olevia päiviä hyväksiluettavaksi
        // vuodelle 1972 jää 19 kpl.
        // 5. poissaolo -> 1972: 23,76 päivää (osa-aikaprosentti 66%). Hyväksiluetaan 19 päivää.
        // Jakson pituutta vähennetään 4,76 päivää. Jäljelle jääviä hyväksiluettavia päiviä vuodelle 1972 0 kpl.
        // 6. poissaolo -> 1973: 40,8 päivää (osa-aikaprosentti 80%). Hyväksiluetaan 30 päivää.
        // Jakson pituutta vähennetään 10,8 päivää.
        // Työskentelyjakson pituus vähennysten jälkeen: 1011 - 6,75 - 4,76 - 10,8 = 988,69 päivää.
        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(988.69)
    }

    @Test
    fun `test calculate with vahennetaan ylimeneva osa with osa-aikaiset poissaolot and tyoskentelyjakso`() {
        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(355L),
            LocalDate.ofEpochDay(370L),
            50,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(390L),
            LocalDate.ofEpochDay(410L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val keskeytysaikaMock3 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(560L),
            LocalDate.ofEpochDay(650L),
            25,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
        )

        val tyoskentelyjakso = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L,
            LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(660L), 75, mutableSetOf(
                keskeytysaikaMock1,
                keskeytysaikaMock2,
                keskeytysaikaMock3
            )
        )

        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso)
        Mockito.`when`(keskeytysaikaMock3.tyoskentelyjakso).thenReturn(tyoskentelyjakso)

        val vahennettavatMap =
            tyoskentelyjaksoService.getVahennettavatPaivat(listOf(tyoskentelyjakso))
        val tyoskentelyJaksonPituusDays =
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                vahennettavatMap[tyoskentelyjakso.id]
            )

        // Työskentelyjakson pituus 233,25 kokonaista päivää (osa-aikaprosentti 75).
        // Työskentelyjaksosta ajoittuu vuodelle 1970 8,25 kokonaista päivää ja vuodelle 1971 4,5 kokonaista päivää.
        // 1. poissaolo (osa-aikaprosentti 50%) ajoittuu kahdelle vuodelle -> 1970:
        // 5 päivää * 0,75 (työskentelyjakson osa-aikaprosentti) = 3,75 pv,
        // 1971: 3 päivää * 0,75 (työskentelyjakson osa-aikaprosentti) = 2,25 pv
        // ei vähennä jakson pituutta -> jäljellä olevia päiviä hyväksiluettavaksi vuonna 1971 jää
        // 30 - 3 * 0,75 (työskentelyjakson osa-aikaprosentti) = 27,75 päivää .
        // 2. poissaolo (täysimittainen) -> 1971: 21 päivää * 0,75 (työskentelyjakson osa-aikaprosentti) = 15,75 pv.
        // Hyväksiluetaan kokonaan. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle 1971 jää 27,75 pv - 15,75 pv
        // eli 12 pv.
        // 3. poissaolo (osa-aikaprosentti 25) -> 1971: 22,75 päivää * 0,75 (työskentelyjakson osa-aikaprosentti)
        // = 17,0625 pv. Hyväksiluetaan 12 päivää. Työskentelyjakson pituutta vähennetään 5,0625 päivää.
        // Työskentelyjakson pituus: 233,25 - 5,0625 = 228,1875 päivää.
        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(228.1875)
    }

    @Test
    fun `test calculateInDays with type vahennetaan ylimeneva osa per vuosi with multiple tyoskentelyjaksot`() {
        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(355L), LocalDate.ofEpochDay(390L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(400L), LocalDate.ofEpochDay(420L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock3 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(600L), LocalDate.ofEpochDay(650L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock4 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(660L), LocalDate.ofEpochDay(670L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock5 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(990L), LocalDate.ofEpochDay(1025L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock6 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(1050L), LocalDate.ofEpochDay(1100L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock7 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(null, LocalDate.ofEpochDay(1105L), LocalDate.ofEpochDay(1139L), 100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val tyoskentelyjakso1 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(1L, LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(500L), 100, mutableSetOf(keskeytysaikaMock1, keskeytysaikaMock2))

        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso1)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso1)

        val tyoskentelyjakso2 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            2L,
            LocalDate.ofEpochDay(510L),
            LocalDate.ofEpochDay(700L), 100, mutableSetOf(
                keskeytysaikaMock3,
                keskeytysaikaMock4
            )
        )

        Mockito.`when`(keskeytysaikaMock3.tyoskentelyjakso).thenReturn(tyoskentelyjakso2)
        Mockito.`when`(keskeytysaikaMock4.tyoskentelyjakso).thenReturn(tyoskentelyjakso2)

        val tyoskentelyjakso3 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            3L,
            LocalDate.ofEpochDay(705L),
            LocalDate.ofEpochDay(1200L), 100, mutableSetOf(
                keskeytysaikaMock5,
                keskeytysaikaMock6,
                keskeytysaikaMock7
            )
        )

        Mockito.`when`(keskeytysaikaMock5.tyoskentelyjakso).thenReturn(tyoskentelyjakso3)
        Mockito.`when`(keskeytysaikaMock6.tyoskentelyjakso).thenReturn(tyoskentelyjakso3)
        Mockito.`when`(keskeytysaikaMock7.tyoskentelyjakso).thenReturn(tyoskentelyjakso3)

        var totalLength = 0.0
        val tyoskentelyjaksot = listOf(tyoskentelyjakso1, tyoskentelyjakso2, tyoskentelyjakso3)
        val vahennettavatMap = tyoskentelyjaksoService.getVahennettavatPaivat(tyoskentelyjaksot)
        tyoskentelyjaksot.map {
            totalLength += tyoskentelyjaksonPituusCounterService.calculateInDays(
                it,
                vahennettavatMap[it.id]
            )
        }

        // 1. työskentelyjakso 151 päivää:
        // 1. poissaolo (ajoittuu kahdelle vuodelle) ->  1970: 10 päivää, 1971: 26 päivää -> ei vähennä jakson pituutta
        // -> jäljellä olevia päiviä hyväksiluettavaksi vuonna 1971 jää 30 - 26 = 4 päivää.
        // 2. poissaolo -> 1971: 21 päivää. Hyväksiluetaan 4 päivää. Jakson pituutta vähennetään 17 päivää.
        // Jäljellä olevia päiviä hyväksiluettavaksi vuodelle 1971 jää 0 kpl.
        // 2. työskentelyjakso 191 päivää:
        // 1. poissaolo -> 1971: 51 päivää. Vähennetään kokonaan, koska vuodelle 1971 hyväksiluettu jo täydet 30 päivää.
        // 2. poissaolo > 1972: 11 päivää. Hyväksiluetaan kokonaan. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle
        // 1972 jää 19 kpl.
        // 3. työskentelyjakso 496 päivää
        // 1. poissaolo -> 1972: 36 päivää. Hyväksiluetaan 19 päivää. Jakson pituutta vähennetään 17 päivää.
        // Jäljellä olevia päiviä hyväksiluettavaksi vuodelle 1972 jää 0 kpl.
        // 2. poissaolo (ajoittuu kahdelle vuodelle) -> 1972: 46 päivää. Vähennetään kokonaan, koska vuodelle 1972
        // hyväksiluettu jo täydet 30 päivää. Jakson pituutta vähennetään 46 päivää. 1973: 5 päivää, jotka
        // hyväksiluetaan. Vuodelle 1973 jää hyväksiluettavia päiviä 25 kpl.
        // 3. poissaolo -> 1973: 35 päivää. Hyväksiluetaan 25 päivää. Vähennetään työskentelyjakson pituutta 10 päivää.
        // Työskentelyjaksojen yhteenlaskettu pituus vähennysten jälkeen:
        // 1. työskentelyjakso: 151 - 17 = 135
        // 2. työskentelyjakso: 191 - 51 = 140
        // 3. työskentelyjakso: 496 - 17 - 46 - 10 = 423
        // Yhteensä: 135 + 140 + 423 = 697 päivää.
        assertThat(totalLength).isEqualTo(697.0)
    }

    @Test
    fun `test calculateHyvaksiluettavatDaysLeft`() {
        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(355L), LocalDate.ofEpochDay(390L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(400L), LocalDate.ofEpochDay(420L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock3 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(900L), LocalDate.ofEpochDay(910L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock4 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(1130L), // 1973
            LocalDate.ofEpochDay(1139L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI, vahennetaanKerran = true)

        val keskeytysaikaMock5 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(1150L), // 1973
            LocalDate.ofEpochDay(1159L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI, vahennetaanKerran = true)

        val tyoskentelyjakso1 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            1L, LocalDate.ofEpochDay(350L), LocalDate.ofEpochDay(500L), 100, mutableSetOf(
                keskeytysaikaMock1,
                keskeytysaikaMock2
            )
        )

        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso1)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso1)

        val tyoskentelyjakso2 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            2L, LocalDate.ofEpochDay(510L), LocalDate.ofEpochDay(700L), 100, mutableSetOf(
                keskeytysaikaMock3,
                keskeytysaikaMock4
            )
        )

        Mockito.`when`(keskeytysaikaMock3.tyoskentelyjakso).thenReturn(tyoskentelyjakso2)
        Mockito.`when`(keskeytysaikaMock4.tyoskentelyjakso).thenReturn(tyoskentelyjakso2)

        val tyoskentelyjakso3 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            3L, LocalDate.ofEpochDay(705L), LocalDate.ofEpochDay(1200L), 100, mutableSetOf(
                keskeytysaikaMock5
            )
        )

        Mockito.`when`(keskeytysaikaMock5.tyoskentelyjakso).thenReturn(tyoskentelyjakso3)

        val tyoskentelyjaksot = listOf(tyoskentelyjakso1, tyoskentelyjakso2, tyoskentelyjakso3)

        val hyvaksiluettavatCounterData =
            tyoskentelyjaksonPituusCounterService.calculateHyvaksiluettavatDaysLeft(
                tyoskentelyjaksot
            )

        // 1. työskentelyjakso 151 päivää:
        // 1. poissaolo (ajoittuu kahdelle vuodelle) ->  1970: 10 päivää, 1971: 26 päivää
        // -> jäljellä olevia päiviä hyväksiluettavaksi vuonna 1970 jää 20 ja vuonna 1971 30 - 26 = 4 päivää.
        // 2. poissaolo -> 1971: 21 päivää. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle 1971 jää 0 kpl.
        // 2. työskentelyjakso 191 päivää:
        // 1. poissaolo -> 1972: 11 päivää. Jäljellä olevia päiviä hyväksiluettavaksi vuodelle 1972 jää 19 kpl.
        // 2. poissaolo (tyyppiä VÄHENNETÄÄN_YLIMENEVÄ_AIKA): 10 päivää,
        // Jäljellä olevia päiviä hyväksiluettavaksi ko. poissaolotyypillä jää 20 päivää.
        // 3. työskentelyjakso 496 päivää
        // 1. poissaolo (tyyppiä VÄHENNETÄÄN_YLIMENEVÄ_AIKA): 10 päivää,
        // Jäljellä olevia päiviä hyväksiluettavaksi ko. poissaolotyypillä jää 10 päivää.

        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1970]).isEqualTo(20.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1971]).isEqualTo(0.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1972]).isEqualTo(19.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1973]).isEqualTo(10.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatDays[keskeytysaikaMock4.poissaolonSyy]).isEqualTo(
            20.0
        )
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatDays[keskeytysaikaMock5.poissaolonSyy]).isEqualTo(
            20.0
        )
    }

    @Test
    fun `test calculateHyvaksiluettavatDaysLeft with calculateUntilDate`() {
        val keskeytysaikaMock1 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(355L), LocalDate.ofEpochDay(390L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock2 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(400L), LocalDate.ofEpochDay(420L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock3 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(900L), LocalDate.ofEpochDay(910L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)

        val keskeytysaikaMock4 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null, LocalDate.ofEpochDay(1130L), LocalDate.ofEpochDay(1139L), 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI,
            vahennetaanKerran = true
        )

        val keskeytysaikaMock5 = KeskeytysaikaMockHelper.createKeskeytysaikaMock(
            null,
            LocalDate.ofEpochDay(1150L),
            LocalDate.ofEpochDay(1159L),
            100,
            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI,
            vahennetaanKerran = true
        )

        val tyoskentelyjakso1 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(350L),
            LocalDate.ofEpochDay(500L), 100, mutableSetOf(
                keskeytysaikaMock1,
                keskeytysaikaMock2
            )
        )

        Mockito.`when`(keskeytysaikaMock1.tyoskentelyjakso).thenReturn(tyoskentelyjakso1)
        Mockito.`when`(keskeytysaikaMock2.tyoskentelyjakso).thenReturn(tyoskentelyjakso1)

        val tyoskentelyjakso2 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(510L),
            LocalDate.ofEpochDay(700L), 100, mutableSetOf(
                keskeytysaikaMock3,
                keskeytysaikaMock4
            )
        )

        Mockito.`when`(keskeytysaikaMock3.tyoskentelyjakso).thenReturn(tyoskentelyjakso2)
        Mockito.`when`(keskeytysaikaMock4.tyoskentelyjakso).thenReturn(tyoskentelyjakso2)

        val tyoskentelyjakso3 = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(705L),
            LocalDate.ofEpochDay(1200L), 100, mutableSetOf(
                keskeytysaikaMock5
            )
        )

        Mockito.`when`(keskeytysaikaMock5.tyoskentelyjakso).thenReturn(tyoskentelyjakso3)

        val tyoskentelyjaksot = listOf(tyoskentelyjakso1, tyoskentelyjakso2, tyoskentelyjakso3)

        val hyvaksiluettavatCounterData =
            tyoskentelyjaksonPituusCounterService.calculateHyvaksiluettavatDaysLeft(
                tyoskentelyjaksot,
                LocalDate.ofEpochDay(402L)
            )

        // 1. työskentelyjakso 151 päivää:
        // 1. poissaolo (ajoittuu kahdelle vuodelle) ->  1970: 10 päivää, 1971: 26 päivää
        // -> jäljellä olevia päiviä hyväksiluettavaksi vuonna 1970 jää 20 ja vuonna 1971: 30 - 26 = 4 päivää.
        // 2. poissaolo -> 1971: 21 päivää. Lasketaan vain 7.2.1971 asti, jolloin 5.2. alkavasta poissaolosta
        // hyväksiluetaan vain ensimmäiset 3 päivää jolloin jäljelle jää vielä yksi päivä.
        // Tästä eteenpäin poissaolot eivät vähennä hyväksiluettavien päivien määrää joten vuosille 1972 ja 1973
        // jää täydet 30 päivää.
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1970]).isEqualTo(20.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1971]).isEqualTo(1.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1972]).isEqualTo(30.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[1973]).isEqualTo(30.0)
        assertThat(hyvaksiluettavatCounterData.hyvaksiluettavatDays).isEmpty()
    }
}
