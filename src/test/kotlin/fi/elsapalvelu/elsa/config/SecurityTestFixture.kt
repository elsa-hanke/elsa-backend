package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.repository.kayttaja.*
import fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen.*
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
import java.time.Clock
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
    kirjautumisenKonteksti: KirjautumisenTestikonteksti = KirjautumisenTestikonteksti()
) = testKirjautumisenPalvelut(
    opintotietodataServices,
    opintosuorituksetServices,
    opintosuorituksetPersistenceService,
    applicationProperties,
    userService,
    opintooikeusService,
    opintotietodataPersistenceService,
    kayttajaRepository,
    userRepository,
    kirjautumisenKonteksti
).konfiguraatio

internal fun testKirjautumisenPalvelut(
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
    kirjautumisenKonteksti: KirjautumisenTestikonteksti = KirjautumisenTestikonteksti()
): KirjautumisenTestipalvelut {
    val opintotietopalvelu = KirjautumisenOpintotietopalvelu(
        opintotietodataServices, opintotietodataPersistenceService,
        opintosuorituksetServices, opintosuorituksetPersistenceService, DirectTestDispatcher
    )
    val kutsupalvelu = Kirjautumiskutsupalvelu(
        kirjautumisenKonteksti.verificationTokenRepository, userRepository, userService, Clock.systemDefaultZone()
    )
    val kayttooikeuspalvelu = KirjautumisenKayttooikeuspalvelu(
        kayttajaRepository, kirjautumisenKonteksti.opintooikeusRepository, opintooikeusService, userService
    )
    val kirjautumispalvelu = Kirjautumispalvelu(
        userService, userRepository, kutsupalvelu, opintotietopalvelu, kayttooikeuspalvelu, applicationProperties, kirjautumisenKonteksti.ymparisto
    )
    val konfiguraatio = SecurityConfiguration(
        mock(CorsFilter::class.java), applicationProperties,
        kirjautumisenKonteksti.opintooikeusRepository, kayttajaRepository, userRepository,
        mock(KouluttajavaltuutusRepository::class.java), kirjautumisenKonteksti.ymparisto,
        mock(ApplicationContext::class.java), kirjautumispalvelu, Clock.systemDefaultZone()
    )
    return KirjautumisenTestipalvelut(konfiguraatio, kirjautumispalvelu, opintotietopalvelu)
}

internal data class KirjautumisenTestipalvelut(
    val konfiguraatio: SecurityConfiguration,
    val kirjautumispalvelu: Kirjautumispalvelu,
    val opintotietopalvelu: KirjautumisenOpintotietopalvelu
)

internal class KirjautumisenTestikonteksti(
    val verificationTokenRepository: VerificationTokenRepository = mock(VerificationTokenRepository::class.java),
    val opintooikeusRepository: OpintooikeusRepository = mock(OpintooikeusRepository::class.java),
    val ymparisto: Environment = org.springframework.mock.env.MockEnvironment()
)

private object DirectTestDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}
