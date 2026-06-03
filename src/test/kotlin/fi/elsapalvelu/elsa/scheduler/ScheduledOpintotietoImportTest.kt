package fi.elsapalvelu.elsa.scheduler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.scheduler.jobs.ScheduledOpintotietoImport
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetPersistenceService
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintotietodataPersistenceService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mock
import org.mockito.MockitoAnnotations
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
            OpintotietodataDTO(syntymaaika = null, opintooikeudet = null)
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
            OpintosuorituksetPersistenceDTO(yliopisto = university, items = emptyList())
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

