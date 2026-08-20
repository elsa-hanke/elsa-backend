package fi.elsapalvelu.elsa.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(
    prefix = "application.scheduling",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
@EnableScheduling
class ShedLockConfig {

    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(dataSource, "shedlock")
    }
}
