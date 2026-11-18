package fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen

import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuorituksetPersistenceService
import fi.elsapalvelu.elsa.service.koulutus.OpintotietodataPersistenceService
import javax.crypto.Cipher
import javax.crypto.SecretKey
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Service
class KirjautumisenOpintotietopalvelu(
    private val opintotietohakupalvelut: List<OpintotietodataFetchingService>,
    private val opintotietojenTallennus: OpintotietodataPersistenceService,
    private val opintosuoritushakupalvelut: List<OpintosuorituksetFetchingService>,
    private val opintosuoritustenTallennus: OpintosuorituksetPersistenceService,
    private val ioSuorittaja: CoroutineDispatcher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun luoEnsikirjautumisenOpintotiedot(
        salain: Cipher, salausavain: SecretKey, hetu: String, etunimi: String, sukunimi: String
    ) {
        runBlocking {
            try {
                haeOpintotiedot(hetu).let {
                    opintotietojenTallennus.create(
                        salain,
                        salausavain,
                        hetu,
                        etunimi,
                        sukunimi,
                        it
                    )
                }
            } catch (ex: Exception) {
                if (ex.message == LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name) throw Exception(LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name)
                log.error("Virhe opintotietodatan haussa tai tallentamisessa: ${ex.message} ${ExceptionUtils.getStackTrace(ex)}")
            }
        }
    }

    fun paivitaOpintotiedot(
        kayttajatunnus: String,
        hetu: String,
        etunimi: String,
        sukunimi: String
    ) {
        MDC.putCloseable(MDC_USER_ID_KEY, kayttajatunnus).use {
            runBlocking {
                try {
                    haeOpintotiedot(hetu).let {
                        if (it.isNotEmpty()) {
                            opintotietojenTallennus.createOrUpdateIfChanged(
                                kayttajatunnus,
                                etunimi,
                                sukunimi,
                                it
                            )
                        } else {
                            log.info("Kirjautuessa ladatut opintotiedot olivat tyhjät käyttäjälle: $kayttajatunnus")
                        }
                    }
                } catch (ex: Exception) {
                    if (ex.message == LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name) throw Exception(LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name)
                    log.error("Virhe opintotietodatan haussa tai päivittämisessä käyttäjälle $kayttajatunnus: ${ex.message} ${ExceptionUtils.getStackTrace(ex)}")
                }
            }
        }
    }

    suspend fun haeOpintotiedot(hetu: String): List<OpintotietodataDTO> =
        supervisorScope {
            opintotietohakupalvelut.map { hakupalvelu ->
                async(ioSuorittaja + MDCContext()) {
                    haeYliopistonOpintotiedot(hakupalvelu, hetu)
                }
            }.awaitAll().filterNotNull()
        }

    private suspend fun haeYliopistonOpintotiedot(
        hakupalvelu: OpintotietodataFetchingService,
        hetu: String
    ): OpintotietodataDTO? =
        try {
            if (hakupalvelu.shouldFetchOpintotietodata()) {
                hakupalvelu.fetchOpintotietodata(hetu)
            } else {
                null
            }
        } catch (ex: CancellationException) {
            throw ex  // Korutiinin peruutusta ei saa ohittaa.
        } catch (ex: Exception) {
            if (ex.message == LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name) throw ex
            log.error(
                "Virhe opintotietodatan haussa yliopistolle ${hakupalvelu.getYliopisto().name}: " +
                    "${ex.message} ${ExceptionUtils.getStackTrace(ex)}"
            )
            null
        }

    fun paivitaOpintosuorituksetTaustalla(kayttajatunnus: String, hetu: String) {
        val suoritusalue = CoroutineScope(
            SupervisorJob() + ioSuorittaja + MDCContext(mapOf(MDC_USER_ID_KEY to kayttajatunnus))
        )
        opintosuoritushakupalvelut.filter { it.shouldFetchOpintosuoritukset() }
            .forEach { hakupalvelu ->
                suoritusalue.launch {
                    try {
                        hakupalvelu.fetchOpintosuoritukset(hetu)?.let {
                            opintosuoritustenTallennus.createOrUpdateIfChanged(kayttajatunnus, it)
                        }
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        log.error(
                            "Virhe opintosuoritusten haussa yliopistolle ${hakupalvelu.getYliopisto().name} " +
                                "käyttäjälle $kayttajatunnus: ${ex.message} ${ExceptionUtils.getStackTrace(ex)}"
                        )
                    }
                }
            }
    }

    fun luoKayttajaKehitysymparistossa(salain: Cipher, salausavain: SecretKey, hetu: String, etunimi: String, sukunimi: String) {
        opintotietojenTallennus.createWithoutOpintotietodataOnlyDevDoNotUseInProd(salain, salausavain, hetu, etunimi, sukunimi)
    }
}
