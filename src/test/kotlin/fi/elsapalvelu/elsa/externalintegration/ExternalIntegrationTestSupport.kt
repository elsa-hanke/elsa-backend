package fi.elsapalvelu.elsa.externalintegration

import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag

@Tag("external-integration")
abstract class ExternalIntegrationTestSupport {

    /**
     * Asserts that [fetchOpintotietodata] completed without a network or parsing error.
     * A null return value always indicates a caught exception inside the service.
     */
    protected fun assertValidOpintotietodata(result: OpintotietodataDTO?) {
        assertThat(result)
            .describedAs("fetchOpintotietodata must not return null (indicates network/parsing failure)")
            .isNotNull
    }

    /**
     * Asserts that [fetchOpintosuoritukset] completed without a network or parsing error
     * and that the items list was populated (may be empty if the test person has no suoritukset).
     */
    protected fun assertValidOpintosuoritukset(result: OpintosuorituksetPersistenceDTO?) {
        assertThat(result)
            .describedAs("fetchOpintosuoritukset must not return null (indicates network/parsing failure)")
            .isNotNull
        assertThat(result!!.items)
            .describedAs("items list must not be null")
            .isNotNull
    }

    protected companion object {
        /** Finnish personal identity code used in all external integration tests. */
        const val HETU = "030884-227C"
    }
}
