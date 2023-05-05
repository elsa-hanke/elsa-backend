package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.impl.OpintooikeusServiceImpl
import kotlinx.coroutines.*
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class ScheduledOpintotietoImport(
    private val opintotietodataFetchingService: List<OpintotietodataFetchingService>,
    private val opintotietodataPersistenceService: OpintotietodataPersistenceService,
    private val opintosuorituksetFetchingService: List<OpintosuorituksetFetchingService>,
    private val opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val applicationProperties: ApplicationProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 4 ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "opintotietoImport", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun import() {
        val timestamp = LocalDateTime.now()
        val cipher = Cipher.getInstance(applicationProperties.getSecurity().cipherAlgorithm)
        val decodedKey = Base64.getDecoder().decode(applicationProperties.getSecurity().encodedKey)
        val originalKey: SecretKey = SecretKeySpec(
            decodedKey, 0, decodedKey.size, applicationProperties.getSecurity().secretKeyAlgorithm
        )
        val opintotietoServices =
            opintotietodataFetchingService.filter { it.shouldFetchOpintotietodata() }
                .associateBy { it.getYliopisto() }
        val opintosuoritusServices =
            opintosuorituksetFetchingService.filter { it.shouldFetchOpintosuoritukset() }
                .associateBy { it.getYliopisto() }
        val now = LocalDate.now()
        opintooikeusRepository.findAllValid(
            now, OpintooikeudenTila.allowedTilat(), OpintooikeudenTila.endedTilat()
        )
            .distinctBy { Pair(it.erikoistuvaLaakari?.id, it.yliopisto?.id) }.forEach {
                val user = it.erikoistuvaLaakari?.kayttaja?.user!!
                getHetu(user, cipher, originalKey)?.let { hetu ->
                    runBlocking {
                        try {
                            opintotietoServices[it.yliopisto?.nimi]?.fetchOpintotietodata(hetu)
                                ?.let { data ->
                                    opintotietodataPersistenceService.createOrUpdateOpintotieto(
                                        user.id!!,
                                        data
                                    )
                                }
                            opintosuoritusServices[it.yliopisto?.nimi]?.fetchOpintosuoritukset(hetu)
                                ?.let { data ->
                                    opintosuorituksetPersistenceService.createOrUpdateIfChanged(
                                        user.id!!,
                                        data
                                    )
                                }
                        } catch (e: Exception) {
                            log.error("OpintotietoImport virhe: ${e.message}")
                        }
                    }
                }
            }
        log.info(
            "OpintotietoImport completed in ${
                Duration.between(timestamp, LocalDateTime.now()).toSeconds()
            } seconds."
        )
    }

    private fun getHetu(user: User, cipher: Cipher, originalKey: SecretKey): String? {
        if (user.hetu == null || user.initVector == null) {
            return null
        }
        cipher.init(Cipher.DECRYPT_MODE, originalKey, IvParameterSpec(user.initVector))
        return String(cipher.doFinal(user.hetu), StandardCharsets.UTF_8)
    }
}
