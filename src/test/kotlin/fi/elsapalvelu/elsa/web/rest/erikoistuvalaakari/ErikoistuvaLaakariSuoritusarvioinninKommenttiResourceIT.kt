package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest(classes = [ElsaBackendApp::class])
@AutoConfigureMockMvc
@WithMockUser
class ErikoistuvaLaakariSuoritusarvioinninKommenttiResourceIT {
    // TODO: toteuta oleelliset testit
}
