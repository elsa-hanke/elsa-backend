package fi.elsapalvelu.elsa.web.rest.test

import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@Profile("dev", "test")
class TestCacheResource(
    private val cacheManager: CacheManager
) {
    val log = LoggerFactory.getLogger(TestCacheResource::class.java)

    @PostMapping("/cache/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clearCaches() {

        cacheManager.cacheNames.forEach { name ->
            log.info("Clearing cache $name")
            cacheManager.getCache(name)?.clear()
        }
    }
}
