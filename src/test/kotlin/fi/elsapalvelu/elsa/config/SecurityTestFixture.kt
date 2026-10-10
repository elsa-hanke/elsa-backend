package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.repository.kayttaja.*
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.koulutus.OpintotietodataPersistenceService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuorituksetPersistenceService
import kotlinx.coroutines.CoroutineDispatcher
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.web.filter.CorsFilter
import kotlin.coroutines.CoroutineContext

internal fun testSecurityConfiguration(
    opintotietodataServices: List<OpintotietodataFetchingService> = emptyList(),
    opintosuorituksetServices: List<OpintosuorituksetFetchingService> = emptyList(),
    opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService =
        mock(OpintosuorituksetPersistenceService::class.java)
) = SecurityConfiguration(
    mock(CorsFilter::class.java),
    ApplicationProperties(),
    mock(UserService::class.java),
    mock(OpintooikeusService::class.java),
    opintotietodataServices,
    mock(OpintotietodataPersistenceService::class.java),
    opintosuorituksetServices,
    opintosuorituksetPersistenceService,
    mock(VerificationTokenRepository::class.java),
    mock(OpintooikeusRepository::class.java),
    mock(KayttajaRepository::class.java),
    mock(UserRepository::class.java),
    mock(KouluttajavaltuutusRepository::class.java),
    mock(Environment::class.java),
    mock(ApplicationContext::class.java),
    DirectTestDispatcher
)

private object DirectTestDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}

