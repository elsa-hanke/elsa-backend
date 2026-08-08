package fi.elsapalvelu.elsa.scheduler.jobs

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.koejakso.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.service.arviointi.*
import fi.elsapalvelu.elsa.service.suoritteet.*
import fi.elsapalvelu.elsa.service.koulutus.*
import fi.elsapalvelu.elsa.service.seuranta.*
import fi.elsapalvelu.elsa.service.valmistuminen.*
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.perustiedot.*
import kotlinx.coroutines.*
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import fi.elsapalvelu.elsa.scheduler.AbstractTriggerableJob
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService

@Component
class ScheduledOpintotietoImport(
    private val opintotietodataFetchingService: List<OpintotietodataFetchingService>,
    private val opintotietodataPersistenceService: OpintotietodataPersistenceService,
    private val opintosuorituksetFetchingService: List<OpintosuorituksetFetchingService>,
    private val opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val applicationProperties: ApplicationProperties
) : AbstractTriggerableJob() {

    override val jobName = "opintotietoImport"

    @Scheduled(cron = "0 0 4 ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "opintotietoImport", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun import() {
        runJob()
    }

    override fun runJob() {
        log.info("OpintotietoImport käynnistetty")
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
        val opintooikeudet = opintooikeusRepository.findAllValid()
            .distinctBy { Pair(it.erikoistuvaLaakari?.id, it.yliopisto?.id) }
        log.info("OpintotietoImport: löydetty ${opintooikeudet.size} käyttäjää")
        opintooikeudet.forEachIndexed { index, opintooikeus ->
            val user = getUserOrLogIncompleteRelationship(opintooikeus, index, opintooikeudet.size)
                ?: return@forEachIndexed
            val yliopistoNimi = opintooikeus.yliopisto?.nimi
            log.info(
                "OpintotietoImport: käyttäjä ${index + 1}/${opintooikeudet.size}: " +
                    "userId=${user.id}, yliopisto=$yliopistoNimi"
            )
            getHetu(user, cipher, originalKey)?.let { hetu ->
                runBlocking {
                    try {
                        opintotietoServices[yliopistoNimi]?.fetchOpintotietodata(hetu)
                            ?.let { data ->
                                opintotietodataPersistenceService.createOrUpdateOpintotieto(
                                    user.id.required(),
                                    data
                                )
                            }
                        opintosuoritusServices[yliopistoNimi]?.fetchOpintosuoritukset(hetu)
                            ?.let { data ->
                                opintosuorituksetPersistenceService.createOrUpdateIfChanged(
                                    user.id.required(),
                                    data
                                )
                            }
                    } catch (e: Exception) {
                        log.error(
                            "OpintotietoImport virhe käyttäjälle ${user.id} " +
                                "(yliopisto=$yliopistoNimi): ${e.message}",
                            e
                        )
                    }
                }
            }
        }
        log.info(
            "OpintotietoImport valmis ${
                Duration.between(timestamp, LocalDateTime.now()).toSeconds()
            } sekunnissa"
        )
    }

    private fun getUserOrLogIncompleteRelationship(
        opintooikeus: Opintooikeus,
        index: Int,
        totalCount: Int
    ): User? {
        val user = opintooikeus.erikoistuvaLaakari?.kayttaja?.user
        if (user == null) {
            log.error(
                "OpintotietoImport: opinto-oikeudelta ${opintooikeus.id} puuttuu käyttäjä; " +
                    "ohitetaan tietue ${index + 1}/$totalCount"
            )
        }
        return user
    }

    private fun getHetu(user: User, cipher: Cipher, originalKey: SecretKey): String? {
        if (user.hetu == null || user.initVector == null) {
            return null
        }
        cipher.init(Cipher.DECRYPT_MODE, originalKey, IvParameterSpec(user.initVector))
        return String(cipher.doFinal(user.hetu), StandardCharsets.UTF_8)
    }
}
