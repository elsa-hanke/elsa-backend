package fi.elsapalvelu.elsa.service.impl.koulutus

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusKurssikoodiRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.mapper.koulutus.OpintosuoritusMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.OpintosuoritusOsakokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.OpintosuoritusTyyppiMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.Optional

class OpintosuorituksetPersistenceServiceImplTest {

    private val erikoistuvaLaakariRepository = mock<ErikoistuvaLaakariRepository>()
    private val userRepository = mock<UserRepository>()
    private val opintosuoritusRepository = mock<OpintosuoritusRepository>()
    private val opintosuoritusMapper = mock<OpintosuoritusMapper>()
    private val opintosuoritusKurssikoodiRepository = mock<OpintosuoritusKurssikoodiRepository>()
    private val opintosuoritusOsakokonaisuusMapper = mock<OpintosuoritusOsakokonaisuusMapper>()
    private val opintosuoritusTyyppiMapper = mock<OpintosuoritusTyyppiMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()

    private val service = OpintosuorituksetPersistenceServiceImpl(
        erikoistuvaLaakariRepository,
        userRepository,
        opintosuoritusRepository,
        opintosuoritusMapper,
        opintosuoritusKurssikoodiRepository,
        opintosuoritusOsakokonaisuusMapper,
        opintosuoritusTyyppiMapper,
        opintooikeusRepository
    )

    @Test
    fun `trainer without erikoistuva laakari is skipped without error log`() {
        givenUserWithoutErikoistuvaLaakari(KOULUTTAJA)

        val logs = captureLogs {
            service.createOrUpdateIfChanged(USER_ID, opintosuoritukset())
        }

        assertThat(logs).noneMatch { it.level == Level.ERROR }
        verifyPersistenceWasNotStarted()
    }

    @Test
    fun `student role without erikoistuva laakari is logged as data inconsistency`() {
        givenUserWithoutErikoistuvaLaakari(ERIKOISTUVA_LAAKARI)

        val logs = captureLogs {
            service.createOrUpdateIfChanged(USER_ID, opintosuoritukset())
        }

        assertThat(logs.map { it.level to it.formattedMessage }).contains(
            Level.ERROR to
                "Käyttäjällä $USER_ID on erikoistuvan lääkärin tai YEK-koulutettavan rooli, " +
                "mutta erikoistuvan lääkärin tietuetta ei löydy. Opintosuorituksia ei tallennettu."
        )
        verifyPersistenceWasNotStarted()
    }

    private fun givenUserWithoutErikoistuvaLaakari(authority: String) {
        whenever(erikoistuvaLaakariRepository.findOneByKayttajaUserId(USER_ID)).thenReturn(null)
        whenever(userRepository.findByIdWithAuthorities(USER_ID)).thenReturn(
            Optional.of(
                User(
                    id = USER_ID,
                    authorities = mutableSetOf(Authority(authority))
                )
            )
        )
    }

    private fun verifyPersistenceWasNotStarted() {
        verifyNoInteractions(
            opintosuoritusRepository,
            opintosuoritusMapper,
            opintosuoritusKurssikoodiRepository,
            opintosuoritusOsakokonaisuusMapper,
            opintosuoritusTyyppiMapper,
            opintooikeusRepository
        )
    }

    private fun opintosuoritukset() = OpintosuorituksetPersistenceDTO(
        yliopisto = YliopistoEnum.OULUN_YLIOPISTO,
        items = listOf(
            OpintosuoritusDTO(
                nimi_fi = "Johtamisopinnot",
                kurssikoodi = "COURSE-1",
                suorituspaiva = LocalDate.of(2026, 9, 1),
                hyvaksytty = true,
                yliopistoOpintooikeusId = "STUDY-RIGHT-1"
            )
        )
    )

    private fun captureLogs(block: () -> Unit): List<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(OpintosuorituksetPersistenceServiceImpl::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
        return try {
            block()
            appender.list.toList()
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private companion object {
        const val USER_ID = "user-id"
    }
}
