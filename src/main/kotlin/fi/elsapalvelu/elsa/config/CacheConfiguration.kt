package fi.elsapalvelu.elsa.config

import io.github.jhipster.config.JHipsterProperties
import io.github.jhipster.config.cache.PrefixedKeyGenerator
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.jsr107.Eh107Configuration
import org.hibernate.cache.jcache.ConfigSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration(
    @Autowired val gitProperties: GitProperties?,
    @Autowired val buildProperties: BuildProperties?,
    jHipsterProperties: JHipsterProperties
) {

    private val jcacheConfiguration: javax.cache.configuration.Configuration<Any, Any>

    init {
        val ehcache = jHipsterProperties.cache.ehcache

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Any::class.java,
                Any::class.java,
                ResourcePoolsBuilder.heap(ehcache.maxEntries)
            )
                .withExpiry(
                    ExpiryPolicyBuilder
                        .timeToLiveExpiration(Duration.ofSeconds(ehcache.timeToLiveSeconds.toLong()))
                )
                .build()
        )
    }

    @Bean
    fun hibernatePropertiesCustomizer(cacheManager: javax.cache.CacheManager) = HibernatePropertiesCustomizer {
        hibernateProperties ->
        hibernateProperties[ConfigSettings.CACHE_MANAGER] = cacheManager
    }

    @Bean
    fun cacheManagerCustomizer(): JCacheManagerCustomizer {
        return JCacheManagerCustomizer { cm ->
            createCache(cm, fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Authority::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.EpaOsaamisalue::class.java.name + ".arvioitavaOsaalues")
            createCache(cm, fi.elsapalvelu.elsa.domain.EpaOsaamisalue::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Erikoisala::class.java.name + ".yliopistos")
            createCache(cm, fi.elsapalvelu.elsa.domain.Erikoisala::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari::class.java.name + ".osoites")
            createCache(cm, fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari::class.java.name + ".tyoskentelyjaksos")
            createCache(cm, fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Hops::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Kayttaja::class.java.name + ".keskustelus")
            createCache(cm, fi.elsapalvelu.elsa.domain.Kayttaja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Koejakso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Opintooikeustiedot::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.OsaalueenArviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Osoite::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Pikaviesti::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.PikaviestiKeskustelu::class.java.name + ".keskustelijas")
            createCache(cm, fi.elsapalvelu.elsa.domain.PikaviestiKeskustelu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.SoteOrganisaatio::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Suoritusarviointi::class.java.name + ".osaalueenArviointis")
            createCache(cm, fi.elsapalvelu.elsa.domain.Suoritusarviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Tyoskentelyjakso::class.java.name + ".suoritusarvioinnit")
            createCache(cm, fi.elsapalvelu.elsa.domain.Tyoskentelyjakso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Tyoskentelypaikka::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.User::class.java.name + ".authorities")
            createCache(cm, fi.elsapalvelu.elsa.domain.User::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Yliopisto::class.java.name + ".erikoisalas")
            createCache(cm, fi.elsapalvelu.elsa.domain.Yliopisto::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.repository.UserRepository.USERS_BY_EMAIL_CACHE)
            createCache(cm, fi.elsapalvelu.elsa.repository.UserRepository.USERS_BY_LOGIN_CACHE)
            // jhipster-needle-ehcache-add-entry
        }
    }

    private fun createCache(cm: javax.cache.CacheManager, cacheName: String) {
        val cache: javax.cache.Cache<Any, Any>? = cm.getCache(cacheName)
        if (cache == null) {
            cm.createCache(cacheName, jcacheConfiguration)
        }
    }

    @Bean
    fun keyGenerator() = PrefixedKeyGenerator(gitProperties, buildProperties)
}
