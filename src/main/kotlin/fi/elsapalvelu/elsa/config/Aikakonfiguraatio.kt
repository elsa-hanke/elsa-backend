package fi.elsapalvelu.elsa.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Aikakonfiguraatio {
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
