package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class TyoskentelyjaksonPituusCounterServiceTest {

    @Autowired
    private lateinit var tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService

    @Test
    fun `assert that tyoskentelyjakso length without osaaikaprosenti is 31 days`() {
        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31), 100, mutableSetOf())
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(31.0)
    }

    @Test
    fun `assert that tyoskentelyjakso length with osaaikaprosentti 50 is 29 days`() {
        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 28), 50, mutableSetOf())
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(29.5)
    }

    @Test
    fun `assert that tyoskentelyjakso length with one week keskeytysaika is 30 days`() {
        val keskeytysaikaMock = createKeskeytysaikaMock(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 22), 0)

        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 7), 100, mutableSetOf(keskeytysaikaMock))
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(30.0)
    }

    @Test
    fun `assert that tyoskentelyjakso length with osaaikaprosentti 50 and keskeytysaika of one week is 29,5 days`() {
        val keskeytysaikaMock = createKeskeytysaikaMock(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 22), 0)
        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 28), 50, mutableSetOf(keskeytysaikaMock))
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(21.5)
    }

    @Test
    fun `assert that tyoskentelyjakso length with osaaikaprosentti 50 and keskeytysaika of one week with osaaikaprosentti 50 is 25,5 days`() {
        val keskeytysaikaMock = createKeskeytysaikaMock(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 22), 50)
        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 28), 50, mutableSetOf(keskeytysaikaMock))
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(25.5)
    }

    @Test
    fun `assert that tyoskentelyjakso length is zero if alkamispaiva is in the future and paattymispaiva null`() {
        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.now().plusDays(5),
            null, 100, mutableSetOf())
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(0.0)
    }

    @Test
    fun `assert that tyoskentelyjakso length is calculated only until current day if still in progress`() {
        val tyoskentelyjakso = createTyoskentelyjakso(LocalDate.now().minusDays(5),
            null, 100, mutableSetOf())
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)

        assertThat(tyoskentelyJaksonPituusDays).isEqualTo(6.0)
    }

    companion object {
        @JvmStatic
        fun createTyoskentelyjakso(alkamispaiva: LocalDate?, paattymispaiva: LocalDate?, osaaikaprosentti: Int, keskeytykset: MutableSet<Keskeytysaika>): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                osaaikaprosentti = osaaikaprosentti,
                keskeytykset = keskeytykset
            )
            tyoskentelyjakso.tyoskentelypaikka = mock(Tyoskentelypaikka::class.java)
            tyoskentelyjakso.erikoistuvaLaakari = mock(ErikoistuvaLaakari::class.java)

            return tyoskentelyjakso
        }

        fun createKeskeytysaikaMock(alkamispaiva: LocalDate?, paattymispaiva: LocalDate?, osaaikaprosentti: Int): Keskeytysaika {
            val keskeytysaikaMock = mock(Keskeytysaika::class.java)
            Mockito.`when`(keskeytysaikaMock.alkamispaiva).thenReturn(alkamispaiva)
            Mockito.`when`(keskeytysaikaMock.paattymispaiva).thenReturn(paattymispaiva)
            Mockito.`when`(keskeytysaikaMock.osaaikaprosentti).thenReturn(osaaikaprosentti)
            val poissaolonSyyMock = mock(PoissaolonSyy::class.java)
            Mockito.`when`(poissaolonSyyMock.vahennystyyppi).thenReturn(PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN)
            Mockito.`when`(keskeytysaikaMock.poissaolonSyy).thenReturn(poissaolonSyyMock)

            return keskeytysaikaMock
        }
    }
}
