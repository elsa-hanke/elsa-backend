package fi.elsapalvelu.elsa.externalintegration

import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory

@Tag("external-integration")
abstract class ExternalIntegrationTestSupport {

    protected val log = LoggerFactory.getLogger(javaClass)

}
