package fi.elsapalvelu.elsa.externalintegration

import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory

@Tag("external-integration")
open class ExternalIntegrationTestSupport {

    protected val log = LoggerFactory.getLogger(javaClass)

}
