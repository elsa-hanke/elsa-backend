package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
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
import tech.jhipster.config.JHipsterProperties
import tech.jhipster.config.cache.PrefixedKeyGenerator
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration(
    @Autowired val gitProperties: GitProperties?,
    @Autowired val buildProperties: BuildProperties?,
    jHipsterProperties: JHipsterProperties
) {

    private final val jcacheConfiguration: javax.cache.configuration.Configuration<Any, Any>

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
    fun hibernatePropertiesCustomizer(
        cacheManager: javax.cache.CacheManager
    ) = HibernatePropertiesCustomizer { hibernateProperties ->
        hibernateProperties[ConfigSettings.CACHE_MANAGER] = cacheManager
    }

    @Bean
    @Suppress("LongMethod")
    fun cacheManagerCustomizer(): JCacheManagerCustomizer {
        // @formatter:off
        return JCacheManagerCustomizer { cm ->
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Arviointiasteikko::class.java.name + ".erikoisalat")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Arviointiasteikko::class.java.name + ".opintooppaat")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Arviointiasteikko::class.java.name + ".tasot")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Arviointiasteikko::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArviointiasteikonTaso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu::class.java.name + ".kysymykset")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKategoria::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys::class.java.name + ".vaihtoehdot")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymysVaihtoehto::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus::class.java.name + ".arvioitavatOsaalueet")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus::class.java.name + ".koulutusjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria::class.java.name + ".arvioitavatKokonaisuudet")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Asetus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Authority::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name + ".arvioitavanKokonaisuudenKategoriat")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name + ".suoritteenKategoriat")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name + ".yliopistot")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name + ".opintooppaat")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name + ".sisuTutkintoohjelmat")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name + ".vastuuhenkilonTehtavatyypit")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari::class.java.name + ".opintooikeudet")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja::class.java.name + ".saadutValtuutukset")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja::class.java.name + ".yliopistotAndErikoisalat")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja::class.java.name + ".yliopistot")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala::class.java.name + ".vastuuhenkilonTehtavat")
            createCache(cm, fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKehittamistoimenpiteet::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoejaksonLoppukeskustelu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoejaksonValiarviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Kouluttajavaltuutus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso::class.java.name + ".osaamistavoitteet")
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso::class.java.name + ".tyoskentelyjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoulutussopimuksenKouluttaja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koejakso.KoulutussopimuksenKoulutuspaikka::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma::class.java.name + ".koulutusjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Kunta::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus::class.java.name + ".tyoskentelyjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus::class.java.name + ".teoriakoulutukset")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus::class.java.name + ".opintosuoritukset")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus::class.java.name + ".annetutValtuutukset")
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Opintoopas::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.seuranta.PaivakirjaAihekategoria::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta::class.java.name + ".aihekategoriat")
            createCache(cm, fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.seuranta.Seurantajakso::class.java.name + ".koulutusjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.seuranta.Seurantajakso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.suoritteet.Suorite::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria::class.java.name + ".suoritteet")
            createCache(cm, fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArvioitavaKokonaisuus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninKommentti::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi::class.java.name + ".osaalueenArvioinnit")
            createCache(cm, fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksynta::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso::class.java.name + ".koulutusjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelypaikka::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.User::class.java.name + ".authorities")
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.User::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto::class.java.name + ".erikoisalat")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto::class.java.name + ".kayttajat")
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.repository.kayttaja.UserRepository.USERS_BY_EMAIL_CACHE)
            createCache(cm, fi.elsapalvelu.elsa.repository.kayttaja.UserRepository.USERS_BY_LOGIN_CACHE)
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.ApplicationSetting::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.ErikoisalaSisuTutkintoohjelma::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusKurssikoodi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus::class.java.name + ".osakokonaisuudet")
            createCache(cm, fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusOsakokonaisuus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeusHerate::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus::class.java.name)
        }
        // @formatter:on
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
