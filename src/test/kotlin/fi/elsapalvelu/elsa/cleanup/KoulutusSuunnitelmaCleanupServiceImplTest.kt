package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class KoulutusSuunnitelmaCleanupServiceImplTest {

    private val asiakirjaCleanupService = mock<AsiakirjaCleanupService>()
    private val koulutusSuunnitelmaCleanupService: KoulutusSuunnitelmaCleanupService = KoulutusSuunnitelmaCleanupServiceImpl(
        asiakirjaCleanupService = asiakirjaCleanupService
    )

    @Test
    fun clearKoulutusSuunnitelma() {

        val koulutussuunnitelma = mockKoulutusSuunnitelma()
        koulutusSuunnitelmaCleanupService.clearKoulutusSuunnitelma(koulutussuunnitelma)

        assertThat(koulutussuunnitelma.elamankentta).isEqualTo("")
        assertThat(koulutussuunnitelma.motivaatiokirje).isEqualTo("")
        assertThat(koulutussuunnitelma.opiskeluJaTyohistoria).isEqualTo("")
        assertThat(koulutussuunnitelma.osaamisenKartuttaminen).isEqualTo("")
        assertThat(koulutussuunnitelma.tulevaisuudenVisiointi).isEqualTo("")
        assertThat(koulutussuunnitelma.vahvuudet).isEqualTo("")
        verifyNoInteractions(asiakirjaCleanupService)
    }

    private fun mockKoulutusSuunnitelma(): Koulutussuunnitelma {
        return Koulutussuunnitelma(
            id = 1L,
            elamankentta = "test-elamankentta",
            motivaatiokirje = "test-motivaatiokirje",
            opiskeluJaTyohistoria = "test-opiskeluJaTyohistoria",
            osaamisenKartuttaminen = "test-osaamisenKartuttaminen",
            tulevaisuudenVisiointi = "test-tulevaisuudenVisiointi",
            vahvuudet = "test-vahvuudet"
        )
    }

}
