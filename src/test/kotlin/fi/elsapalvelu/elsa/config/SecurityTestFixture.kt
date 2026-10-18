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
        mock(OpintosuorituksetPersistenceService::class.java),
    applicationProperties: ApplicationProperties = ApplicationProperties(),
    userService: UserService = mock(UserService::class.java),
    opintooikeusService: OpintooikeusService = mock(OpintooikeusService::class.java),
    opintotietodataPersistenceService: OpintotietodataPersistenceService = mock(OpintotietodataPersistenceService::class.java),
    kayttajaRepository: KayttajaRepository = mock(KayttajaRepository::class.java),
    userRepository: UserRepository = mock(UserRepository::class.java),
    verificationTokenRepository: VerificationTokenRepository = mock(VerificationTokenRepository::class.java)
) = SecurityConfiguration(
    mock(CorsFilter::class.java),
    applicationProperties,
    userService,
    opintooikeusService,
    opintotietodataServices,
    opintotietodataPersistenceService,
    opintosuorituksetServices,
    opintosuorituksetPersistenceService,
    verificationTokenRepository,
    mock(OpintooikeusRepository::class.java),
    kayttajaRepository,
    userRepository,
    mock(KouluttajavaltuutusRepository::class.java),
    mock(Environment::class.java),
    mock(ApplicationContext::class.java),
    DirectTestDispatcher
)

private object DirectTestDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}
