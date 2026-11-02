package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyynnonTarkistusRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mail.TransactionalMailService
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusMapper
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusUpdateMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ValmistumispyyntoNotificationTest {
    private val mailService = mock<MailService>()
    private val properties = ApplicationProperties().apply {
        getOpintohallintoemail().turku = "opintohallinto@example.test"
    }
    private val ilmoitusService = ValmistumispyynnonIlmoitusService(
        TransactionalMailService(mailService), properties
    )
    private val repository = mock<ValmistumispyyntoRepository>()
    private val tarkistusRepository = mock<ValmistumispyynnonTarkistusRepository>()
    private val tarkistusMapper = mock<ValmistumispyynnonTarkistusMapper>()
    private val updateMapper = mock<ValmistumispyynnonTarkistusUpdateMapper>()
    private val osapuoliService = mock<ValmistumispyynnonOsapuoliService>()
    private val clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC)
    private val service = ValmistumispyyntoServiceImpl(
        valmistumispyyntoRepository = repository,
        valmistumispyyntoQueryService = mock(),
        valmistumispyyntoMapper = mock(),
        valmistumispyyntoOsaamisenArviointiMapper = mock(),
        clock = clock,
        valmistumispyynnonTarkistusRepository = tarkistusRepository,
        valmistumispyynnonTarkistusMapper = tarkistusMapper,
        valmistumispyynnonTarkistusUpdateMapper = updateMapper,
        erikoistuvaLaakariService = mock(),
        vanhentumisService = mock(),
        tilaService = mock(),
        osapuoliService = osapuoliService,
        arviointienTilaService = mock(),
        viimeistelyService = mock(),
        ilmoitusService = ilmoitusService,
        tarkistusService = mock()
    )

    @ParameterizedTest
    @CsvSource(
        "false, false, false", "false, false, true",
        "false, true, false", "false, true, true",
        "true, false, false", "true, false, true",
        "true, true, false", "true, true, true"
    )
    fun `sending request for final approval does not send email`(
        yek: Boolean, returned: Boolean, existingCheck: Boolean
    ) {
        val pyynto = request(yek).apply {
            if (returned) {
                vastuuhenkiloHyvaksyjaKorjausehdotus = "Korjaa tiedot"
                vastuuhenkiloHyvaksyjaPalautusaika = LocalDate.now(clock).minusDays(1)
                virkailijanKorjausehdotus = "Korjaa tiedot"
            }
        }
        val officer = Kayttaja(id = 2L, yliopistot = mutableSetOf(pyynto.opintooikeus!!.yliopisto!!))
        val form = ValmistumispyynnonTarkistusUpdateDTO(
            keskenerainen = false, lisatiedotVastuuhenkilolle = "Tarkistettu"
        )
        val tarkistus = ValmistumispyynnonTarkistus(valmistumispyynto = pyynto)
        val expected = ValmistumispyynnonTarkistusDTO()
        whenever(osapuoliService.haeKayttaja("officer")).thenReturn(officer)
        whenever(tarkistusMapper.toDto(tarkistus)).thenReturn(expected)
        if (existingCheck) {
            whenever(tarkistusRepository
                .findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(1L, 1L))
                .thenReturn(tarkistus)
        } else {
            whenever(repository.findByIdAndOpintooikeusYliopistoId(1L, 1L)).thenReturn(pyynto)
            whenever(updateMapper.toEntity(form)).thenReturn(tarkistus)
        }

        assertThat(service.updateTarkistusByVirkailijaUserId(1L, "officer", form, null))
            .isSameAs(expected)

        assertThat(pyynto.virkailijanKuittausaika).isEqualTo(LocalDate.now(clock))
        assertThat(pyynto.virkailija).isSameAs(officer)
        assertThat(pyynto.virkailijanSaate).isEqualTo("Tarkistettu")
        assertThat(pyynto.vastuuhenkiloHyvaksyjaKorjausehdotus).isNull()
        assertThat(pyynto.vastuuhenkiloHyvaksyjaPalautusaika).isNull()
        assertThat(pyynto.virkailijanKorjausehdotus).isNull()
        verify(repository).save(pyynto)
        verify(tarkistusRepository).save(tarkistus)
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `return notification still emails trainee and university`(yek: Boolean) {
        val pyynto = request(yek)
        val user = pyynto.opintooikeus!!.erikoistuvaLaakari!!.kayttaja!!.user!!

        ilmoitusService.lahetaIlmoitusPalautuksesta(pyynto, ilmoitaYliopistolle = true)

        verify(mailService).sendEmailFromTemplate(
            user, listOf(), "valmistumispyyntoPalautettuErikoistuja.html",
            "email.valmistumispyyntoPalautettuErikoistuja.title", emptyArray(), emptyMap()
        )
        val name = user.getName()
        verify(mailService).sendEmailFromTemplate(
            "opintohallinto@example.test", listOf(),
            if (yek) "valmistumispyyntoPalautettuMuutYek.html" else "valmistumispyyntoPalautettuMuut.html",
            "email.valmistumispyyntoPalautettuMuut.title", arrayOf(name), mapOf(MailProperty.NAME to name)
        )
        verifyNoMoreInteractions(mailService)
    }

    private fun request(yek: Boolean) = Valmistumispyynto(
        id = 1L,
        opintooikeus = Opintooikeus(
            id = 1L,
            yliopisto = Yliopisto(id = 1L, nimi = YliopistoEnum.TURUN_YLIOPISTO),
            erikoisala = Erikoisala(id = if (yek) YEK_ERIKOISALA_ID else 1L),
            erikoistuvaLaakari = ErikoistuvaLaakari(
                kayttaja = Kayttaja(user = User(firstName = "Test", lastName = "Trainee"))
            )
        )
    )
}
