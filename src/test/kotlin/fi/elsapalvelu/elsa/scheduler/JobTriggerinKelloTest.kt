package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.sql.Timestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate

class JobTriggerinKelloTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `manuaalinen kaynnistys vertaa lukkoa kellon hetkeen`(aika: KellonTestitapaus) {
        val jdbc = mock<JdbcTemplate>()
        val tehtava = mock<TriggerableJob>()
        whenever(tehtava.jobName).thenReturn("testitehtava")
        val paate = JobTriggerEndpoint(listOf(tehtava), jdbc, aika.kello())

        assertThat(paate.trigger("testitehtava").status).isEqualTo(200)
        verify(jdbc).queryForObject(eq("SELECT COUNT(*) FROM shedlock WHERE name = ? AND lock_until > ?"),
            eq(Int::class.java), eq("testitehtava"), eq(Timestamp.from(aika.aikaleima())))
        verify(tehtava).runJob()
    }
}
