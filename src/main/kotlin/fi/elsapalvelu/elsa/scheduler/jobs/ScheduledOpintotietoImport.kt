package fi.elsapalvelu.elsa.scheduler.jobs

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
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
import org.slf4j.MDC
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
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY

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
        importOpintotiedot()
        log.info(
            "OpintotietoImport valmis ${
                Duration.between(timestamp, LocalDateTime.now()).toSeconds()
            } sekunnissa"
        )
    }

    private fun importOpintotiedot() {
        val context = createImportContext()
        val opintooikeudet = findImportableOpintooikeudet()
        log.info("OpintotietoImport: löydetty ${opintooikeudet.size} käyttäjää")
        opintooikeudet.forEachIndexed { index, opintooikeus ->
            importOpintooikeus(opintooikeus, index, opintooikeudet.size, context)
        }
    }

    private fun createImportContext(): ImportContext {
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

        return ImportContext(cipher, originalKey, opintotietoServices, opintosuoritusServices)
    }

    private fun findImportableOpintooikeudet(): List<Opintooikeus> =
        opintooikeusRepository.findAllValid()
            .distinctBy { Pair(it.erikoistuvaLaakari?.id, it.yliopisto?.id) }

    private fun importOpintooikeus(
        opintooikeus: Opintooikeus,
        index: Int,
        totalCount: Int,
        context: ImportContext
    ) {
        val user = getUserOrLogIncompleteRelationship(opintooikeus, index, totalCount) ?: return
        val userId = user.id.required()
        val yliopistoNimi = opintooikeus.yliopisto?.nimi
        log.info(
            "OpintotietoImport: käyttäjä ${index + 1}/$totalCount: " +
                "userId=$userId, yliopisto=$yliopistoNimi"
        )
        MDC.putCloseable(MDC_USER_ID_KEY, userId).use {
            getHetu(user, context.cipher, context.originalKey)?.let { hetu ->
                runBlocking {
                    fetchAndPersistOpintotiedot(userId, hetu, yliopistoNimi, context)
                }
            }
        }
    }

    private suspend fun fetchAndPersistOpintotiedot(
        userId: String,
        hetu: String,
        yliopistoNimi: YliopistoEnum?,
        context: ImportContext
    ) {
        try {
            context.opintotietoServices[yliopistoNimi]?.fetchOpintotietodata(hetu)
                ?.let { data ->
                    opintotietodataPersistenceService.createOrUpdateOpintotieto(userId, data)
                }
            context.opintosuoritusServices[yliopistoNimi]?.fetchOpintosuoritukset(hetu)
                ?.let { data ->
                    opintosuorituksetPersistenceService.createOrUpdateIfChanged(userId, data)
                }
        } catch (e: Exception) {
            log.error(
                "OpintotietoImport virhe käyttäjälle $userId " +
                    "(yliopisto=$yliopistoNimi): ${e.message}",
                e
            )
        }
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

    private data class ImportContext(
        val cipher: Cipher,
        val originalKey: SecretKey,
        val opintotietoServices: Map<YliopistoEnum, OpintotietodataFetchingService>,
        val opintosuoritusServices: Map<YliopistoEnum, OpintosuorituksetFetchingService>
    )
}
