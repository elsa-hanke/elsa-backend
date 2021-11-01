package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.helpers.KeskeytysaikaMockHelper
import fi.elsapalvelu.elsa.service.helpers.TyoskentelyjaksoMockHelper
import fi.elsapalvelu.elsa.service.impl.OverlappingKeskeytysaikaValidationServiceImpl
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OverlappingKeskeytysaikaValidationServiceTest {

    @Autowired
    private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper

    private lateinit var tyoskentelyjaksoRepositoryMock: TyoskentelyjaksoRepository

    private lateinit var tyoskentelyjaksoMock: Tyoskentelyjakso

    fun initTest(tyoskentelyjaksoOsaaikaprosentti: Int = 100) {
        tyoskentelyjaksoRepositoryMock = mock(TyoskentelyjaksoRepository::class.java)
        tyoskentelyjaksoMock = TyoskentelyjaksoMockHelper.createTyoskentelyjaksoMock(
            null,
            LocalDate.ofEpochDay(0L),
            LocalDate.ofEpochDay(30L),
            tyoskentelyjaksoOsaaikaprosentti,
            mutableSetOf()
        )
    }

    private fun testValidateKeskeytysaika(
        expectedResult: Boolean,
        keskeytysaikaDTO: KeskeytysaikaDTO,
        keskeytykset: MutableSet<Keskeytysaika>
    ) {
        `when`(tyoskentelyjaksoMock.keskeytykset).thenReturn(keskeytykset)
        `when`(
            tyoskentelyjaksoRepositoryMock.findOneByIdAndErikoistuvaLaakariKayttajaUserIdEagerWithKeskeytykset(1, "")
        ).thenReturn(tyoskentelyjaksoMock)

        val validator = OverlappingKeskeytysaikaValidationServiceImpl(
            keskeytysaikaMapper, tyoskentelyjaksoRepositoryMock
        )

        val validationResult = validator.validateKeskeytysaika("", keskeytysaikaDTO)
        assertThat(validationResult).isEqualTo(expectedResult)
    }

    @Test
    fun `add second keskeytysaika with 50 percent when tyoskentelyaika 100 percent should return true`() {
        initTest()
        testValidateKeskeytysaika(
            true,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                )
            )
        )
    }

    @Test
    fun `modify second keskeytysaika into 50 percent when tyoskentelyaika 100 percent should return true`() {
        initTest()
        testValidateKeskeytysaika(
            true,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    25
                )
            )
        )
    }

    @Test
    fun `add third keskeytysaika with 25 percent when tyoskentelyaika 100 percent should return true`() {
        initTest()
        testValidateKeskeytysaika(
            true,
            KeskeytysaikaDTO(
                3,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                25,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(10),
                    LocalDate.ofEpochDay(10),
                    25
                )
            )
        )
    }


    @Test
    fun `add second keskeytysaika with 50 percent when tyoskentelyaika 50 percent should return true`() {
        initTest(50)
        testValidateKeskeytysaika(
            true,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                )
            )
        )
    }

    @Test
    fun `modify second keskeytysaika into 50 percent when tyoskentelyaika 50 percent should return true`() {
        initTest(50)
        testValidateKeskeytysaika(
            true,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    25
                )
            )
        )
    }

    @Test
    fun `add third keskeytysaika with 25 percent when tyoskentelyaika 50 percent should return true`() {
        initTest(50)
        testValidateKeskeytysaika(
            true,
            KeskeytysaikaDTO(
                3,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                25,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(10),
                    LocalDate.ofEpochDay(10),
                    25
                )
            )
        )
    }

    @Test
    fun `add second keskeytysaika with 50 percent when tyoskentelyaika 100 percent should return false`() {
        initTest()
        testValidateKeskeytysaika(
            false,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    100
                )
            )
        )
    }

    @Test
    fun `modify second keskeytysaika into 75 percent when tyoskentelyaika 100 percent should return false`() {
        initTest()
        testValidateKeskeytysaika(
            false,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                75,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                )
            )
        )
    }

    @Test
    fun `add third keskeytysaika with 50 percent when tyoskentelyaika 100 percent should return false`() {
        initTest()
        testValidateKeskeytysaika(
            false,
            KeskeytysaikaDTO(
                3,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(10),
                    LocalDate.ofEpochDay(10),
                    25
                )
            )
        )
    }

    @Test
    fun `add second keskeytysaika with 50 percent when tyoskentelyaika 50 percent should return false`() {
        initTest(50)
        testValidateKeskeytysaika(
            false,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    100
                )
            )
        )
    }

    @Test
    fun `modify second keskeytysaika into 75 percent when tyoskentelyaika 50 percent should return false`() {
        initTest(50)
        testValidateKeskeytysaika(
            false,
            KeskeytysaikaDTO(
                2,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                75,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                )
            )
        )
    }

    @Test
    fun `add third keskeytysaika with 50 percent when tyoskentelyaika 50 percent should return false`() {
        initTest(50)
        testValidateKeskeytysaika(
            false,
            KeskeytysaikaDTO(
                3,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                50,
                null,
                1
            ),
            mutableSetOf(
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    1,
                    LocalDate.ofEpochDay(5),
                    LocalDate.ofEpochDay(10),
                    50
                ),
                KeskeytysaikaMockHelper.createKeskeytysaikaMock(
                    2,
                    LocalDate.ofEpochDay(10),
                    LocalDate.ofEpochDay(10),
                    25
                )
            )
        )
    }
}
