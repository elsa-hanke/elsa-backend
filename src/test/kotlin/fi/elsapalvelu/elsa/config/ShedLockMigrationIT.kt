package fi.elsapalvelu.elsa.config

import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.boot.test.context.TestConfiguration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.Instant

class ShedLockMigrationIT {
    private lateinit var dataSource: HikariDataSource

    @BeforeEach
    fun createDatabase() {
        // A separate database keeps these standalone lock tests independent of
        // the application schema and its Liquibase-managed ShedLock table.
        dataSource = HikariDataSource().apply {
            driverClassName = "org.testcontainers.jdbc.ContainerDatabaseDriver"
            jdbcUrl = "jdbc:tc:postgresql:16.9:///shedlock?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
            username = "test"
            password = "test"
            maximumPoolSize = 2
        }
        JdbcTemplate(dataSource).execute(
            "create table shedlock (name varchar(64) primary key, lock_until timestamp not null, locked_at timestamp not null, locked_by varchar(255) not null)"
        )
    }

    @AfterEach
    fun closeDatabase() {
        try {
            JdbcTemplate(dataSource).execute("drop table if exists shedlock")
        } finally {
            dataSource.close()
        }
    }

    @Test
    fun independentInstancesCannotHoldTheSameJobLockAtTheSameTime() {
        val firstInstance = ShedLockConfig().lockProvider(dataSource)
        val secondInstance = ShedLockConfig().lockProvider(dataSource)
        val lock = LockConfiguration(Instant.now(), "paattyvaOpintooikeusHerate", Duration.ofMinutes(1), Duration.ZERO)
        val firstLock = firstInstance.lock(lock)
        assertThat(firstLock).isPresent
        try {
            assertThat(secondInstance.lock(lock)).isEmpty
        } finally {
            firstLock.orElseThrow().unlock()
        }
        val nextLock = secondInstance.lock(LockConfiguration(Instant.now(), lock.name, Duration.ofMinutes(1), Duration.ZERO))
        assertThat(nextLock).isPresent
        nextLock.orElseThrow().unlock()
    }
    @Test
    fun springSevenProxySkipsJobWhileAnotherApplicationInstanceHoldsLock() {
        val first = jobContext(dataSource)
        val second = jobContext(dataSource)
        val executor = Executors.newSingleThreadExecutor()
        val firstState = first.getBean(JobState::class.java)
        val secondState = second.getBean(JobState::class.java)
        try {
            val running = executor.submit { first.getBean(LockedJob::class.java).run() }
            assertThat(firstState.entered.await(5, TimeUnit.SECONDS)).isTrue()
            second.getBean(LockedJob::class.java).run()
            assertThat(secondState.invocations.get()).isZero()
            firstState.release.countDown()
            running.get(5, TimeUnit.SECONDS)
            secondState.release.countDown()
            second.getBean(LockedJob::class.java).run()
            assertThat(secondState.invocations.get()).isEqualTo(1)
        } finally {
            firstState.release.countDown()
            secondState.release.countDown()
            executor.shutdownNow()
            first.close()
            second.close()
        }
    }

    private fun jobContext(dataSource: DataSource) = AnnotationConfigApplicationContext().apply {
        beanFactory.registerSingleton("dataSource", dataSource)
        register(ShedLockConfig::class.java, JobConfiguration::class.java)
        refresh()
    }

    @TestConfiguration(proxyBeanMethods = false)
    class JobConfiguration {
        @Bean fun jobState() = JobState()
        @Bean fun lockedJob(state: JobState) = LockedJob(state)
    }

    class JobState {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val invocations = AtomicInteger()
    }

    open class LockedJob(private val state: JobState) {
        @SchedulerLock(name = "paattyvaOpintooikeusHerate")
        open fun run() {
            state.invocations.incrementAndGet()
            state.entered.countDown()
            check(state.release.await(10, TimeUnit.SECONDS)) { "Test did not release the job" }
        }
    }

}
