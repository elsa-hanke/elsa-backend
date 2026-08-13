package fi.elsapalvelu.elsa.scheduler

import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.scheduler.jobs.ScheduledOpintotietoImport
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuorituksetPersistenceService
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.koulutus.OpintotietodataPersistenceService
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.MDC
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

class ScheduledOpintotietoImportTest {

    @Mock private lateinit var opintotietodataPersistenceService: OpintotietodataPersistenceService
    @Mock private lateinit var opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService
    @Mock private lateinit var opintooikeusRepository: OpintooikeusRepository

    private val aesKey = KeyGenerator.getInstance("AES").also { it.init(128) }.generateKey()

    private lateinit var scheduler: ScheduledOpintotietoImport
    private var opintotietodataUserIdFromMdc: String? = null
    private var opintosuorituksetUserIdFromMdc: String? = null

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val appProps = ApplicationProperties()
        appProps.getSecurity().encodedKey = Base64.getEncoder().encodeToString(aesKey.encoded)
        appProps.getSecurity().secretKeyAlgorithm = "AES"
        appProps.getSecurity().cipherAlgorithm = "AES/CBC/PKCS5Padding"

        scheduler = ScheduledOpintotietoImport(
            opintotietodataFetchingService = listOf(
                ThrowingOpintotietodataService(YliopistoEnum.HELSINGIN_YLIOPISTO),
                ReturningOpintotietodataService(YliopistoEnum.OULUN_YLIOPISTO)
            ),
            opintotietodataPersistenceService = opintotietodataPersistenceService,
            opintosuorituksetFetchingService = listOf(
                ThrowingOpintosuorituksetService(YliopistoEnum.HELSINGIN_YLIOPISTO),
                ReturningOpintosuorituksetService(YliopistoEnum.OULUN_YLIOPISTO)
            ),
            opintosuorituksetPersistenceService = opintosuorituksetPersistenceService,
            opintooikeusRepository = opintooikeusRepository,
            applicationProperties = appProps
        )
    }

    @Test
    fun `import does not throw and continues when one user fetch throws`() {
        // Two users: the Helsinki one throws, the Oulu one succeeds.
        whenever(opintooikeusRepository.findAllValid()).thenReturn(
            listOf(
                makeOpintooikeus("user-fail", YliopistoEnum.HELSINGIN_YLIOPISTO),
                makeOpintooikeus("user-ok",   YliopistoEnum.OULUN_YLIOPISTO)
            )
        )

        // The whole import must complete without propagating any exception.
        assertDoesNotThrow { scheduler.import() }

        // The OK user's data was persisted.
        verify(opintotietodataPersistenceService, times(1))
            .createOrUpdateOpintotieto(eq("user-ok"), any())
        verify(opintosuorituksetPersistenceService, times(1))
            .createOrUpdateIfChanged(eq("user-ok"), any())

        // The failing user was attempted but nothing was persisted for them.
        verify(opintotietodataPersistenceService, never())
            .createOrUpdateOpintotieto(eq("user-fail"), any())
        verify(opintosuorituksetPersistenceService, never())
            .createOrUpdateIfChanged(eq("user-fail"), any())

        assertThat(opintotietodataUserIdFromMdc).isEqualTo("user-ok")
        assertThat(opintosuorituksetUserIdFromMdc).isEqualTo("user-ok")
        assertThat(MDC.get(MDC_USER_ID_KEY)).isNull()
    }

    @Test
    fun `import skips user silently when hetu is missing`() {
        // User has no hetu → getHetu returns null → fetch services are never called.
        whenever(opintooikeusRepository.findAllValid()).thenReturn(
            listOf(makeOpintooikeus("user-no-hetu", YliopistoEnum.OULUN_YLIOPISTO, encryptHetu = false))
        )

        assertDoesNotThrow { scheduler.import() }

        verify(opintotietodataPersistenceService, never()).createOrUpdateOpintotieto(any(), any())
        verify(opintosuorituksetPersistenceService, never()).createOrUpdateIfChanged(any(), any())
    }

    @Test
    fun `import skips incomplete relationship and continues with next user`() {
        whenever(opintooikeusRepository.findAllValid()).thenReturn(
            listOf(
                Opintooikeus(
                    id = 999L,
                    erikoistuvaLaakari = null,
                    yliopisto = Yliopisto(id = 999L, nimi = YliopistoEnum.OULUN_YLIOPISTO)
                ),
                makeOpintooikeus("user-ok", YliopistoEnum.OULUN_YLIOPISTO)
            )
        )

        assertDoesNotThrow { scheduler.import() }

        verify(opintotietodataPersistenceService).createOrUpdateOpintotieto(eq("user-ok"), any())
        verify(opintosuorituksetPersistenceService).createOrUpdateIfChanged(eq("user-ok"), any())
    }

    private inner class ThrowingOpintotietodataService(
        private val university: YliopistoEnum
    ) : OpintotietodataFetchingService {
        override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? =
            throw RuntimeException("Simulated fetch error for $university")
        override fun shouldFetchOpintotietodata() = true
        override fun getYliopisto() = university
    }

    private inner class ReturningOpintotietodataService(
        private val university: YliopistoEnum
    ) : OpintotietodataFetchingService {
        override suspend fun fetchOpintotietodata(hetu: String) =
            OpintotietodataDTO(syntymaaika = null, opintooikeudet = null).also {
                opintotietodataUserIdFromMdc = MDC.get(MDC_USER_ID_KEY)
            }
        override fun shouldFetchOpintotietodata() = true
        override fun getYliopisto() = university
    }

    private inner class ThrowingOpintosuorituksetService(
        private val university: YliopistoEnum
    ) : OpintosuorituksetFetchingService {
        override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? =
            throw RuntimeException("Simulated fetch error for $university")
        override fun shouldFetchOpintosuoritukset() = true
        override fun getYliopisto() = university
    }

    private inner class ReturningOpintosuorituksetService(
        private val university: YliopistoEnum
    ) : OpintosuorituksetFetchingService {
        override suspend fun fetchOpintosuoritukset(hetu: String) =
            OpintosuorituksetPersistenceDTO(yliopisto = university, items = emptyList()).also {
                opintosuorituksetUserIdFromMdc = MDC.get(MDC_USER_ID_KEY)
            }
        override fun shouldFetchOpintosuoritukset() = true
        override fun getYliopisto() = university
    }

    private var nextId = 1L

    private fun makeOpintooikeus(
        userId: String,
        yliopistoEnum: YliopistoEnum,
        encryptHetu: Boolean = true
    ): Opintooikeus {
        val erikoistuvaLaakariId = nextId++
        val yliopistoId          = nextId++

        val user = if (encryptHetu) {
            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
            val encryptedHetu = cipher.doFinal("010190-1234".toByteArray(StandardCharsets.UTF_8))
            User(id = userId, hetu = encryptedHetu, initVector = iv)
        } else {
            User(id = userId, hetu = null, initVector = null)
        }

        val kayttaja           = Kayttaja(user = user)
        val erikoistuvaLaakari = ErikoistuvaLaakari(id = erikoistuvaLaakariId, kayttaja = kayttaja)
        val yliopisto          = Yliopisto(id = yliopistoId, nimi = yliopistoEnum)

        return Opintooikeus(
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto
        )
    }
}
