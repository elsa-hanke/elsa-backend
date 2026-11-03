package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokalunTila
import fi.elsapalvelu.elsa.service.arviointi.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = ["spring.jpa.properties.hibernate.cache.use_second_level_cache=true"]
)
@Transactional
class ArviointityokaluRepositoryIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var repository: ArviointityokaluRepository

    @Autowired
    private lateinit var service: ArviointityokaluService

    @BeforeEach
    fun verifySecondLevelCacheEnabled() {
        val factory = em.entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
        assertThat(factory.sessionFactoryOptions.isSecondLevelCacheEnabled).isTrue()
    }

    @AfterEach
    fun clearSecondLevelCache() {
        em.entityManagerFactory.cache.evictAll()
    }

    private fun clearPersistenceContextAndCache() {
        em.clear()
        em.entityManagerFactory.cache.evictAll()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `existing tool without an owner can be loaded through a lazy reference`(kaytossa: Boolean) {
        val id = seedExistingArviointityokalu(kaytossa)
        clearPersistenceContextAndCache()

        val tool = em.getReference(Arviointityokalu::class.java, id)
        assertThat(Hibernate.isInitialized(tool)).isFalse()

        // Arviointityokalu's proxy is initialized by hashCode when Hibernate loads
        // the assessment answers into a PersistentSet, as in the reported stack trace.
        hashSetOf(tool)

        assertThat(Hibernate.isInitialized(tool)).isTrue()
        assertThat(tool.nimi).isEqualTo("Ownerless assessment tool")
        assertThat(tool.kayttaja).isNull()
        assertThat(tool.kaytossa).isEqualTo(kaytossa)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `tool without an owner can be persisted and reloaded`(kaytossa: Boolean) {
        val tool = Arviointityokalu(
            nimi = "New ownerless assessment tool",
            kayttaja = null,
            kaytossa = kaytossa,
            tila = ArviointityokalunTila.JULKAISTU,
            luontiaika = Instant.now(),
            muokkausaika = Instant.now()
        )
        em.persist(tool)
        em.flush()
        clearPersistenceContextAndCache()

        val reloaded = em.find(Arviointityokalu::class.java, tool.id)
        assertThat(reloaded).isNotNull()
        assertThat(reloaded.nimi).isEqualTo(tool.nimi)
        assertThat(reloaded.kayttaja).isNull()
        assertThat(reloaded.kaytossa).isEqualTo(kaytossa)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `repository finds an ownerless tool with cold caches`(kaytossa: Boolean) {
        val id = seedExistingArviointityokalu(kaytossa)
        clearPersistenceContextAndCache()

        val found = repository.findById(id)
        assertThat(found).isPresent()
        assertThat(found.get().kayttaja).isNull()
        assertThat(found.get().kaytossa).isEqualTo(kaytossa)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `service versions an ownerless tool with cold caches`(kaytossa: Boolean) {
        val id = seedExistingArviointityokalu(kaytossa)
        clearPersistenceContextAndCache()

        val updated = service.update(
            ArviointityokaluDTO(
                id = id,
                nimi = "Updated ownerless tool",
                tila = ArviointityokalunTila.JULKAISTU
            ),
            null
        )
        assertThat(updated).isNotNull()
        val newId = requireNotNull(updated?.id)
        assertThat(newId).isNotEqualTo(id)
        em.flush()
        clearPersistenceContextAndCache()

        val original = repository.findById(id).orElseThrow()
        val newVersion = repository.findById(newId).orElseThrow()
        assertThat(original.kaytossa).isFalse()
        assertThat(original.nimi).isEqualTo("Ownerless assessment tool")
        assertThat(original.kayttaja).isNull()
        assertThat(newVersion.alkuperainenId).isEqualTo(id)
        assertThat(newVersion.versio).isEqualTo(original.versio + 1)
        assertThat(newVersion.nimi).isEqualTo("Updated ownerless tool")
        assertThat(newVersion.kaytossa).isTrue()
        assertThat(newVersion.kayttaja).isNull()
    }

    private fun seedExistingArviointityokalu(kaytossa: Boolean): Long {
        // Seed an existing, schema-valid row directly: ORM validation must not prevent
        // the test from reaching the lazy-load failure seen during graduation PDF creation.
        return (em.createNativeQuery(
            """
                INSERT INTO arviointityokalu
                    (nimi, versio, kaytossa, tila, luontiaika, muokkausaika, kayttaja_id)
                VALUES
                    ('Ownerless assessment tool', 21, :kaytossa, 'JULKAISTU',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                RETURNING id
            """.trimIndent()
        ).setParameter("kaytossa", kaytossa).singleResult as Number).toLong()
    }
}
