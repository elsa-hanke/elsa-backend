package fi.elsapalvelu.elsa.repository.kayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = ["spring.jpa.properties.hibernate.cache.use_second_level_cache=true"]
)
@Transactional
class OpintooikeusRepositoryTest {





}
