package fi.elsapalvelu.elsa.web.rest

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

open class ResourceIntegrationTestBase {
    @Autowired
    protected lateinit var em: EntityManager
}
