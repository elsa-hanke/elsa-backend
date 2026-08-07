package fi.elsapalvelu.elsa.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfiguration {

    @Bean(destroyMethod = "")
    @Suppress("InjectDispatcher")
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
