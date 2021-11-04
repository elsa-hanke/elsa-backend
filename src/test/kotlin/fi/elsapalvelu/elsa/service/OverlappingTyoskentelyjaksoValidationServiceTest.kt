package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.repository.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.PoissaolonSyyDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.helpers.KeskeytysaikaMockHelper
import fi.elsapalvelu.elsa.service.helpers.TyoskentelyjaksoMockHelper
import fi.elsapalvelu.elsa.service.impl.OverlappingTyoskentelyjaksoValidationServiceImpl
import fi.elsapalvelu.elsa.service.impl.TyoskentelyjaksonPituusCounterServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OverlappingTyoskentelyjaksoValidationServiceTest {

    private fun testValidateTyoskentelyjakso(
        expectedResult: Boolean,
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        tyoskententelyjaksoDTO: TyoskentelyjaksoDTO
    ) {
        val tyoskentelyjaksotToReturn =
            tyoskentelyjaksot.filter { it.alkamispaiva!! <= tyoskententelyjaksoDTO.paattymispaiva }
        val tyoskentelyjaksoRepositoryMock = mock(TyoskentelyjaksoRepository::class.java)
        `when`(
            tyoskentelyjaksoRepositoryMock.findAllByErikoistuvaUntilDateEagerWithRelationships(
                "",
                tyoskententelyjaksoDTO.paattymispaiva!!
            )
        ).thenReturn(tyoskentelyjaksotToReturn)

        val validator = OverlappingTyoskentelyjaksoValidationServiceImpl(
            tyoskentelyjaksoRepositoryMock,
            mock(KeskeytysaikaRepository::class.java),
            TyoskentelyjaksonPituusCounterServiceImpl()
        )

        val validationResult = validator.validateTyoskentelyjakso("", tyoskententelyjaksoDTO)
        assertThat(validationResult).isEqualTo(expectedResult)
    }

    private fun testValidateKeskeytysaika(
        expectedResult: Boolean,
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ) {
        val tyoskentelyjaksotToReturn =
            tyoskentelyjaksot.filter { it.alkamispaiva!! <= keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva }
        val tyoskentelyjaksoRepositoryMock = mock(TyoskentelyjaksoRepository::class.java)
        `when`(
            tyoskentelyjaksoRepositoryMock.findAllByErikoistuvaUntilDateEagerWithRelationships(
                "",
                keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva!!
            )
        ).thenReturn(tyoskentelyjaksotToReturn)

        val validator = OverlappingTyoskentelyjaksoValidationServiceImpl(
            tyoskentelyjaksoRepositoryMock,
            mock(KeskeytysaikaRepository::class.java),
            TyoskentelyjaksonPituusCounterServiceImpl()
        )

        val validationResult = validator.validateKeskeytysaika("", keskeytysaikaDTO)
        assertThat(validationResult).isEqualTo(expectedResult)
    }

    private fun testValidateKeskeytysaikaDelete(
        expectedResult: Boolean,
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ) {
        val tyoskentelyjaksotToReturn =
            tyoskentelyjaksot.filter { it.alkamispaiva!! <= keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva }
        val tyoskentelyjaksoRepositoryMock = mock(TyoskentelyjaksoRepository::class.java)
        `when`(
            tyoskentelyjaksoRepositoryMock.findAllByErikoistuvaUntilDateEagerWithRelationships(
                "",
                keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva!!
            )
        ).thenReturn(tyoskentelyjaksotToReturn)

        val keskeytysaikaMock = KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
            keskeytysaikaDTO.id,
            keskeytysaikaDTO.alkamispaiva,
            keskeytysaikaDTO.paattymispaiva
        )
        `when`(keskeytysaikaMock.tyoskentelyjakso?.alkamispaiva).thenReturn(
            keskeytysaikaDTO.tyoskentelyjakso?.alkamispaiva!!
        )
        `when`(keskeytysaikaMock.tyoskentelyjakso?.paattymispaiva).thenReturn(
            keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva!!
        )
        `when`(keskeytysaikaMock.tyoskentelyjakso?.id).thenReturn(
            keskeytysaikaDTO.tyoskentelyjaksoId
        )
        val keskeytysaikaRepositoryMock = mock(KeskeytysaikaRepository::class.java)
        `when`(
            keskeytysaikaRepositoryMock.findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
                keskeytysaikaDTO.id!!,
                ""
            )
        ).thenReturn(keskeytysaikaMock)

        val validator = OverlappingTyoskentelyjaksoValidationServiceImpl(
            tyoskentelyjaksoRepositoryMock,
            keskeytysaikaRepositoryMock,
            TyoskentelyjaksonPituusCounterServiceImpl()
        )

        val validationResult = validator.validateKeskeytysaikaDelete("", keskeytysaikaDTO.id!!)
        assertThat(validationResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Add new overlapping tyoskentelyjakso should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    25,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(10L),
                    LocalDate.ofEpochDay(15L),
                    50,
                    mutableSetOf()
                )
            ),
            TyoskentelyjaksoDTO(
                null,
                LocalDate.ofEpochDay(0L),
                LocalDate.ofEpochDay(30L),
                null,
                25
            )
        )
    }

    @Test
    fun `Add new overlapping tyoskentelyjakso keskeytysaika exists should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(15L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(10L),
                    LocalDate.ofEpochDay(15L),
                    25,
                    mutableSetOf()
                )
            ),
            TyoskentelyjaksoDTO(
                null,
                LocalDate.ofEpochDay(0L),
                LocalDate.ofEpochDay(30L),
                null,
                25
            )
        )
    }

    @Test
    fun `Modify existing tyoskentelyjakso overlapping with others should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(20L),
                    LocalDate.ofEpochDay(30L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(31L),
                    LocalDate.ofEpochDay(40L),
                    100,
                    mutableSetOf()
                )
            ),
            // Päivitetään työskentelyjakso 1 päättymään päivää ennen jakson 2 alkamista.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(19L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify existing tyoskentelyjakso overlapping with others and keskeytysaika exists should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(20L),
                            LocalDate.ofEpochDay(20L),
                            100
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(20L),
                    LocalDate.ofEpochDay(30L),
                    25,
                    mutableSetOf()
                )
            ),
            // Päivitetään työskentelyjakso 1 päättymään samana päivänä kuin jakso 2 alkaa. Työskentelyjaksoon 1 kuitenkin
            // kohdistuu poissaolo (100%) samalle päivälle jolloin jakso 2 alkaa.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify existing tyoskentelyjakso overlapping with others and multiple keskeytysaika exists should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    50,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(10L),
                            LocalDate.ofEpochDay(15L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(16L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(16L),
                            LocalDate.ofEpochDay(20L),
                            75
                        )
                    )
                )
            ),
            // Muokataan työskentelyjakso 1 päättymään samaan aikaan työskentelyjakso 2:n kanssa, mutta
            // muokataan osa-aikaprosentiksi 25, jolloin yli 100% työskentelyaika ei ylity koska jakso 2:n
            // kohdistuu 75% keskeytysaika.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                null,
                25
            )
        )
    }

    @Test
    fun `Modify overlapping tyoskentelyjakso when keskeytysaika with hyvaksiluettavat days exists should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(69L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(40L),
                            LocalDate.ofEpochDay(69L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(70L),
                    LocalDate.ofEpochDay(80L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(81L),
                    LocalDate.ofEpochDay(100L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(81L),
                            LocalDate.ofEpochDay(90L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(91L),
                    LocalDate.ofEpochDay(150L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(100L),
                            LocalDate.ofEpochDay(130L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
            ),
            // Työskentelyjaksoon 1 kohdistuu 30 päivän poissaolo, joka hyväksiluetaan kokonaan.
            // Työskentelyjaksoon 3 kohdistuu 10 päivän poissaolo sen alkamispäivästä lähtien.
            // Pidennetään työskentelyjakso 2 loppumaan samana päivänä, joka on jakso 3:n viimeinen poissaolopäivä.
            // Koska 30 päivää on jo hyväksiluetty jakso 1:n osalta, vähentää jakso 3:n kohdistuva poissaolo
            // työskentelyaikaa täysimääräisesti. Näin ollen 100% allokaatio ei ylity vaikka jakso 2:n päättymispäivää
            // pidennetään.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(70L),
                LocalDate.ofEpochDay(90L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify overlapping tyoskentelyjakso when keskeytysaika with hyvaksiluettavat days exists should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(68L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(40L),
                            LocalDate.ofEpochDay(68L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(40L),
                    LocalDate.ofEpochDay(68L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(69L),
                    LocalDate.ofEpochDay(100L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(69L),
                            LocalDate.ofEpochDay(78L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(80L),
                    LocalDate.ofEpochDay(150L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(100L),
                            LocalDate.ofEpochDay(130L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
            ),
            // Työskentelyjakso 1 ja 2 ovat päällekkäisiä 29 päivän osalta. Jaksoon 1 kuitenkin kohdistuu poissaolo
            // viimeisen 29 päivän osalta, joten 100% allokaatio ei ylity. Työskentelyjaksoon 3 kohdistuu poissaolo
            // ensimmäisen 10 päivän osalta. Muokataan jakson 2 päättymispäiväksi sama, jona jakson 3 poissaolo päättyy.
            // Koska poissaolot ovat tyyppiä VAHENNETAAN_YLIMENEVA_AIKA, hyväksiluetaan jakson 1 poissaolosta 29
            // päivää. Hyväksiluettavia päiviä jää siis yksi, joten jakson 3 ensimmäinen poissaolopäivä hyväksiluetaan
            // ja näin ollen 100% allokaatio ylittyy.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(40L),
                LocalDate.ofEpochDay(78L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify overlapping tyoskentelyjakso when keskeytysaika with hyvaksiluettavat days per year exists should return true`() {
        testValidateTyoskentelyjakso(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(69L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(40L),
                            LocalDate.ofEpochDay(69L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(370L),
                    LocalDate.ofEpochDay(450L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(390L),
                            LocalDate.ofEpochDay(419L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(451L),
                    LocalDate.ofEpochDay(460L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(461L),
                    LocalDate.ofEpochDay(500L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(461L),
                            LocalDate.ofEpochDay(470L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(91L),
                    LocalDate.ofEpochDay(150L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(100L),
                            LocalDate.ofEpochDay(130L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
            ),
            // Työskentelyjaksoon 1 kohdistuu 30 päivän poissaolo vuodelle 1970, joka hyväksiluetaan kokonaan.
            // Työskentelyjaksoon 2 kohdistuu 30 poissaolo vuodelle 1971, joka hyväksiluetaan kokonaan.
            // Työskentelyjaksoon 4 kohdistuu 10 päivän poissaolo sen alkamispäivästä lähtien.
            // Pidennetään työskentelyjakso 3 loppumaan samana päivänä, joka on jakso 4:n viimeinen poissaolopäivä.
            // Koska 30 päivää on jo hyväksiluetty jakso 2:n osalta, vähentää jakso 4:n kohdistuva poissaolo
            // työskentelyaikaa täysimääräisesti. Näin ollen 100% allokaatio ei ylity vaikka jakso 3:n päättymispäivää
            // pidennetään.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(451L),
                LocalDate.ofEpochDay(470L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify overlapping tyoskentelyjakso when keskeytysaika with hyvaksiluettavat days per year exists should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(69L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(40L),
                            LocalDate.ofEpochDay(69L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(360L),
                    LocalDate.ofEpochDay(370L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(360L),
                            LocalDate.ofEpochDay(370L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(360L),
                    LocalDate.ofEpochDay(364L),
                    100,
                    mutableSetOf()
                )
            ),
            // Työskentelyjaksoon 1 kohdistuu 30 päivän poissaolo vuodelle 1970, joka hyväksiluetaan kokonaan.
            // Työskentelyjaksoon 2 kohdistuu 30 poissaolo, joka jakautuu vuosille 1970 ja 1971. Vuoden 1970 osuus tästä
            // vähentää työskentelyaikaa, koska 30 päivää per vuosi on jo hyväksiluettu. Sen sijaan vuodelle 1971
            // ajoittuva osuus ei vähennä työskentelyaikaa, koska hyväksiluettavia päiviä on taas uudet 30 kpl.
            // Näin ollen kun jaksoa 3 pidennetään seuraavan vuoden puolelle, ylittyy 100% allokaatio heti vuoden
            // ensimmäisenä päivänä.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(360L),
                LocalDate.ofEpochDay(370L),
                null,
                100
            )
        )
    }

    @Test
    fun `Add new overlapping tyoskentelyjakso should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    50,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(10L),
                    LocalDate.ofEpochDay(15L),
                    50,
                    mutableSetOf()
                )
            ),
            TyoskentelyjaksoDTO(
                null,
                LocalDate.ofEpochDay(0L),
                LocalDate.ofEpochDay(30L),
                null,
                25
            )
        )
    }

    @Test
    fun `Add new overlapping tyoskentelyjakso and keskeytysaika exists should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(15L),
                            25
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(10L),
                    LocalDate.ofEpochDay(15L),
                    25,
                    mutableSetOf()
                )
            ),
            TyoskentelyjaksoDTO(
                null,
                LocalDate.ofEpochDay(0L),
                LocalDate.ofEpochDay(30L),
                null,
                25
            )
        )
    }

    @Test
    fun `Add new tyoskentelyjakso overlapping with others and hyvaksiluettavat days left should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(50L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(31L),
                            LocalDate.ofEpochDay(50L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                )
            ),
            // Olemassaolevaan työskentelyjaksoon kohdistuu 20 päivän mittainen poissaolo tyyppiä
            // VAHENNETAAN_YLIMENEVA_AIKA. Hyväksiluettavia päiviä jää vielä 10. Uusi työskentelyjakso alkaa
            // samana päivänä kuin olemassaolevan jakson poissaolo. Poissaolo kuitenkin hyväksiluetaan
            // kokonaisuudessaan, joten työskentelyjakson lisääminen aiheuttaisi yli 100% allokaation
            // kaikille siihen kuuluville päiville.
            TyoskentelyjaksoDTO(
                null,
                LocalDate.ofEpochDay(31L),
                LocalDate.ofEpochDay(50L),
                null,
                50
            )
        )
    }

    @Test
    fun `Add new tyoskentelyjakso overlapping with others and hyvaksiluettavat days per vuosi left should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(50L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(21L),
                            LocalDate.ofEpochDay(50L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(370L),
                    LocalDate.ofEpochDay(510L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(490L),
                            LocalDate.ofEpochDay(500L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN
                        ),
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(501L),
                            LocalDate.ofEpochDay(510L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                )
            ),
            // Työskentelyjaksoon 1 (vuosi 1970) kohdistuu 30 päivän mittainen poissaolo tyyppiä
            // VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI. Työskentelyjaksoon 2 (vuosi 1971) kohdistuu ensin 11 päivän
            // poissaolo tyyppiä VAHENNETAAN_SUORAAN, joten työskentelyjakson lisääminen ei aiheuta yli 100%
            // allokaatiota ensimmäisen 11 päivän osalta. Tästä eteenpäin työskentelyjakso 2:n kuitenkin kohdistuu
            // toinen 10 päivän mittainen poissaolo tyyppiä VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI, joka hyväksiluetaan
            // kokonaan, joten uusi työskentelyjakso aiheuttaa yli 100% allokaation poissaolo 2:n alkamispäivästä
            // lähtien.
            TyoskentelyjaksoDTO(
                null,
                LocalDate.ofEpochDay(490L),
                LocalDate.ofEpochDay(510L),
                null,
                50
            )
        )
    }

    @Test
    fun `Modify existing tyoskentelyjakso overlapping with others should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(20L),
                    LocalDate.ofEpochDay(30L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(31L),
                    LocalDate.ofEpochDay(40L),
                    100,
                    mutableSetOf()
                )
            ),
            // Päivitetään työskentelyjakso 1 päättymään samana päivänä kuin 2 alkaa.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify existing tyoskentelyjakso overlapping with others and keskeytysaika exists should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf()
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(20L),
                    LocalDate.ofEpochDay(30L),
                    25,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(21L),
                            LocalDate.ofEpochDay(21L),
                            100
                        )
                    )
                )
            ),
            // Päivitetään työskentelyjakso 1 päättymään samana päivänä kuin jakso 2 alkaa. Työskentelyjaksoon 2
            // kohdistuu poissaolo mutta vasta seuraavalle päivälle jona jakso 1 ja 2 ovat päällekkäin.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                null,
                100
            )
        )
    }

    @Test
    fun `Modify existing tyoskentelyjakso overlapping with others and multiple keskeytysaika exists should return false`() {
        testValidateTyoskentelyjakso(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoWithMockDependencies(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    50,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(10L),
                            LocalDate.ofEpochDay(15L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(16L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(16L),
                            LocalDate.ofEpochDay(20L),
                            49
                        )
                    )
                )
            ),
            // Päivitetään työskentelyjakso 1 päättymään samaan aikaan työskentelyjakson 2 kanssa.
            // Työskentelyjaksoon 2 kohdistuu poissaolo, mutta koska sen osa-aikaprosentti on 49
            // ylittyy 100% työaika niukasti kun jakson 1 päättymispäivää siirretään.
            TyoskentelyjaksoDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                null,
                50
            )
        )
    }

    @Test
    fun `Modify keskeytysaika when overlapping tyoskentelyjaksot should return true`() {
        testValidateKeskeytysaika(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(20L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(16L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(16L),
                            LocalDate.ofEpochDay(19L),
                            50
                        ),
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(20L),
                            LocalDate.ofEpochDay(20L),
                            100
                        ),
                    )
                )
            ),
            // Työskentelyjakso 1:n kohdistuu 50% poissaolo. Työskentelyjaksoon taas yksi 50% poissaolo ja viimeiselle
            // päivälle 100% poissaolo. Päivitetään jakso 1:n poissaoloa yhden päivän lyhyemmäksi, jolloin 100%
            // allokaatio ei ylity viimeisen päivän osalta.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(19L),
                50,
                PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN,
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L)
            )
        )
    }

    @Test
    fun `Modify keskeytysaika when overlapping tyoskentelyjaksot should return false`() {
        testValidateKeskeytysaika(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(20L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(20L),
                            50
                        )
                    )
                )
            ),
            // Työskentelyjaksot 1 ja 2 ovat kokonaan päällekkäisiä, mutta molempiin kohdistuu 50% poissaolo.
            // Muokataan työskentelyjakso 1:n poissaoloprosenttia pienemmäksi kuin 50, jolloin 100%
            // työskentelyaika ylittyy.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                49,
                PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN,
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L)
            )
        )
    }

    @Test
    fun `Remove keskeytysaika when overlapping tyoskentelyjaksot should return true`() {
        testValidateKeskeytysaikaDelete(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(15L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(15L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(15L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(15L),
                            LocalDate.ofEpochDay(15L),
                            100
                        )
                    )
                )
            ),
            // Työskentelyjaksoon 1 kohdistuu 50% poissaolo. Työskentelyjakso 2 alkaa jakso 1:n viimeisenä päivänä,
            // mutta koska jakso 2:n kohdistuu ensimmäiselle päivälle 100% poissaolo, voidaan jakso 1:n poissaolo
            // poistaa eikä 100% työskentelyaika ylity.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(15L),
                null,
                null,
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(15L)
            )
        )
    }

    @Test
    fun `Remove keskeytysaika when overlapping tyoskentelyjaksot should return false`() {
        testValidateKeskeytysaikaDelete(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(20L),
                            50
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(5L),
                    LocalDate.ofEpochDay(20L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(5L),
                            LocalDate.ofEpochDay(20L),
                            50
                        )
                    )
                )
            ),
            // Päällekkäisiin työskentelyjaksoihin 1 ja 2 kohdistuu 50% poissaolo. Poistetaan jakso 1:n poissaolo, jolloin
            // 100% työskentelyaika ylittyy.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L),
                null,
                null,
                1,
                LocalDate.ofEpochDay(5L),
                LocalDate.ofEpochDay(20L)
            )
        )
    }

    @Test
    fun `Modify keskeytysaika type when overlapping tyoskentelyjaksot should return true`() {
        testValidateKeskeytysaika(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(364L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(50L),
                            LocalDate.ofEpochDay(79L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(360L),
                    LocalDate.ofEpochDay(370L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(360L),
                            LocalDate.ofEpochDay(364L),
                            100
                        )
                    )
                )
            ),
            // Työskentelyjaksoon 1 (365 päivää) kohdistuu 30 päivän poissaolo, joka hyväksiluetaan.
            // Työskentelyjakso 2 on päällekkäinen viimeisen viiden päivän osalta mutta koska kyseisille päiville
            // kohdistuu 100% poissaolo, ei 100% allokaatio ylity. Vaihdetaan poissaolon tyypiksi
            // VAHENNETAAN_YLIMENEVA_AIKA. Poissaolo ei muuten vähentäisi työaikaa, mutta koska kyseisellä
            // poissaolotyypillä on jo hyväksiluettu 30 päivää, vähentää poissaolo työskentelyaikaa täyden päivän verran
            // päällekkäisten 5 päivän osalta, jolloin 100% allokaatio ei ylity.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(360L),
                LocalDate.ofEpochDay(364L),
                100,
                PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA,
                1,
                LocalDate.ofEpochDay(360L),
                LocalDate.ofEpochDay(370L)
            )
        )
    }

    @Test
    fun `Modify keskeytysaika type when overlapping tyoskentelyjaksot should return false`() {
        testValidateKeskeytysaika(
            false,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(364L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(50L),
                            LocalDate.ofEpochDay(78L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(360L),
                    LocalDate.ofEpochDay(370L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(360L),
                            LocalDate.ofEpochDay(364L),
                            100
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(371L),
                    LocalDate.ofEpochDay(415L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(380),
                            LocalDate.ofEpochDay(410),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(416L),
                    LocalDate.ofEpochDay(500),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(430),
                            LocalDate.ofEpochDay(470),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(501L),
                    LocalDate.ofEpochDay(600),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(550),
                            LocalDate.ofEpochDay(590),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN
                        )
                    )
                )
            ),
            // Työskentelyjaksoon 1 (365 päivää) kohdistuu 29 päivän poissaolo, joka hyväksiluetaan.
            // Työskentelyjakso 2 on päällekkäinen viimeisen viiden päivän osalta mutta koska kyseisille päiville
            // kohdistuu 100% poissaolo, ei 100% allokaatio ylity. Vaihdetaan poissaolon tyypiksi
            // VAHENNETAAN_YLIMENEVA_AIKA. Koska hyväksiluettavia päiviä on vielä jäljellä yksi, ylittyy 100%
            // allokaatio ensimmäisen päivän osalta jolloin jaksot ovat päällekkäisiä. Hyväksiluetut
            // päivät lasketaan vain siihen päivään asti, jolloin 100% allokaatio ylittyisi ja aletaan tarkastelemaan
            // poissaoloja. Näin ollen työskentelyjakson 3, 4 ja 5 poissaolot eivät vähennä hyväksiluettavien päivien
            // määrää, kun jaksoon 2 kohdistuvaa poissaoloa muokataan.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(360L),
                LocalDate.ofEpochDay(364L),
                100,
                PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA,
                1,
                LocalDate.ofEpochDay(360L),
                LocalDate.ofEpochDay(370L)
            )
        )
    }

    @Test
    fun `Modify keskeytysaika type per year when overlapping tyoskentelyjaksot should return true`() {
        testValidateKeskeytysaika(
            true,
            listOf(
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(0L),
                    LocalDate.ofEpochDay(364L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(50L),
                            LocalDate.ofEpochDay(79L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    null,
                    LocalDate.ofEpochDay(365L),
                    LocalDate.ofEpochDay(600L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                            null,
                            LocalDate.ofEpochDay(365L),
                            LocalDate.ofEpochDay(394L),
                            100,
                            PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI
                        )
                    )
                ),
                TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
                    1,
                    LocalDate.ofEpochDay(395L),
                    LocalDate.ofEpochDay(420L),
                    100,
                    mutableSetOf(
                        KeskeytysaikaMockHelper.createKeskeytysaikaWithTyoskentelyjaksoMock(
                            1,
                            LocalDate.ofEpochDay(395L),
                            LocalDate.ofEpochDay(420L),
                            100
                        )
                    )
                )
            ),
            // Työskentelyjaksoon 1 kohdistuu 30 päivän poissaolo, joka hyväksiluetaan.
            // Työskentelyjakso 2 alkaa seuraavana päivänä, kun jakso 1 päättyy. Työskentelyjaksoon 2
            // kohdistuu myös 30 päivän poissaolo. Työskentelyjakso 3 on päällekkäinen jakso 2:n kanssa
            // 21 päivän osalta. Vaihdetaan jakso 3:n poissaolotyypiksi VAHENNETAAN_YLIMENEVA_AIKA.
            // Koska vuodelle 1971 on jo hyväksiluettu 30 päivää, vähentää poissaolo työskentelyaikaa
            // koko ajaltaan. Näin ollen 100% allokaatio ei ylity yhdenkään päivän osalta.
            createKeskeytysaikaDTO(
                1,
                LocalDate.ofEpochDay(395L),
                LocalDate.ofEpochDay(420L),
                100,
                PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI,
                1,
                LocalDate.ofEpochDay(395L),
                LocalDate.ofEpochDay(420L)
            )
        )
    }

    companion object {
        @JvmStatic
        private fun createKeskeytysaikaDTO(
            id: Long,
            alkamispaiva: LocalDate?,
            paattymispaiva: LocalDate?,
            poissaoloProsentti: Int?,
            poissaolonSyy: PoissaolonSyyTyyppi? = PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN,
            tyoskentelyjaksoId: Long?,
            tyoskentelyjaksonAlkamispaiva: LocalDate?,
            tyoskentelyjaksonPaattymispaiva: LocalDate?
        ): KeskeytysaikaDTO {
            return KeskeytysaikaDTO(
                id,
                alkamispaiva,
                paattymispaiva,
                poissaoloProsentti,
                null,
                tyoskentelyjaksoId,
                PoissaolonSyyDTO(
                    null,
                    "",
                    poissaolonSyy,
                    LocalDate.ofEpochDay(0L)
                ),
                TyoskentelyjaksoDTO(
                    tyoskentelyjaksoId,
                    tyoskentelyjaksonAlkamispaiva,
                    tyoskentelyjaksonPaattymispaiva
                )
            )
        }
    }
}
