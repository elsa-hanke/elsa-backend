package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class TyoskentelypaikkaResourceIT {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUNTA = "AAAAAAAAAA"
        private const val UPDATED_KUNTA = "BBBBBBBBBB"

        private val DEFAULT_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        private val UPDATED_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA

        @JvmStatic
        fun createEntity(): Tyoskentelypaikka {

            return Tyoskentelypaikka(
                nimi = DEFAULT_NIMI,
                kunta = DEFAULT_KUNTA,
                tyyppi = DEFAULT_TYYPPI
            )
        }

        @JvmStatic
        fun createUpdatedEntity(): Tyoskentelypaikka {

            return Tyoskentelypaikka(
                nimi = UPDATED_NIMI,
                kunta = UPDATED_KUNTA,
                tyyppi = UPDATED_TYYPPI
            )
        }
    }
}
