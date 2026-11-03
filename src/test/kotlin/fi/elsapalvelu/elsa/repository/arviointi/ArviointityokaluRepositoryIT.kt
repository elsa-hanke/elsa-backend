package fi.elsapalvelu.elsa.repository.arviointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokalunTila
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ArviointityokaluRepositoryIT {

    @Autowired
    private lateinit var em: EntityManager

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `existing tool without an owner can be loaded through a lazy reference`(kaytossa: Boolean) {
        // Seed an existing, schema-valid row directly: ORM validation must not prevent
        // the test from reaching the lazy-load failure seen during graduation PDF creation.
        val id = (em.createNativeQuery(
            """
                INSERT INTO arviointityokalu
                    (nimi, versio, kaytossa, tila, luontiaika, muokkausaika, kayttaja_id)
                VALUES
                    ('Ownerless assessment tool', 21, :kaytossa, 'JULKAISTU',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                RETURNING id
            """.trimIndent()
        ).setParameter("kaytossa", kaytossa).singleResult as Number).toLong()
        em.clear()

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
        em.clear()

        val reloaded = em.find(Arviointityokalu::class.java, tool.id)
        assertThat(reloaded).isNotNull()
        assertThat(reloaded.nimi).isEqualTo(tool.nimi)
        assertThat(reloaded.kayttaja).isNull()
        assertThat(reloaded.kaytossa).isEqualTo(kaytossa)
    }
}
