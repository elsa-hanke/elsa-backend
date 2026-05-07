package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat

/**
 * Unit tests to reproduce the production crash:
 *
 *   java.lang.NoSuchFieldError: _anyGetterWriter
 *
 * Root cause: Jackson binary incompatibility.
 * The field `_anyGetterWriter` in BeanSerializerBase was renamed to
 * `_anyGetterWriters` in Jackson databind 2.18.0.
 * Any Jackson module compiled against databind < 2.18.0 will throw
 * NoSuchFieldError at runtime when Jackson databind 2.18.x is on the classpath.
 *
 * The crash happens in ArkistointiServiceImpl.muodostaSahke() →
 *   XmlMapper().writeValueAsBytes(metadata)
 *
 * Test 1 mirrors the production code verbatim.
 * Test 2 adds SPI-based module auto-discovery (findAndAddModules) to force
 * all Jackson modules on the classpath to be registered, which is the most
 * likely trigger for the broken module being loaded.
 * Test 3 asserts that the jackson-dataformat-xml version matches jackson-databind
 * so a version drift is caught at build time.
 */
class ArkistointiXmlSerializationTest {

    /**
     * Mirrors ArkistointiServiceImpl.muodostaSahke() line-for-line.
     * Will throw NoSuchFieldError: _anyGetterWriter if any Jackson module
     * on the classpath was compiled against databind < 2.18.0.
     */
    @Test
    fun `XmlMapper serializes ArkistointiMetadata without Jackson field name mismatch`() {
        // Exact copy of the mapper setup in ArkistointiServiceImpl.muodostaSahke()
        val mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.dateFormat = SimpleDateFormat("dd.MM.yyyy")
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)

        val metadata = ArkistointiMetadata()

        // This must NOT throw NoSuchFieldError: _anyGetterWriter
        val bytes = mapper.writeValueAsBytes(metadata)
        assertThat(bytes).isNotEmpty()
    }

    /**
     * Same serialization but with every Jackson Module on the classpath registered
     * via SPI (findAndAddModules). This is the most direct trigger for the bug:
     * if jackson-datatype-hibernate5-jakarta (or any other module) is present and
     * compiled against an old databind, it gets loaded here and causes the error.
     */
    @Test
    fun `XmlMapper with all auto-discovered modules serializes ArkistointiMetadata`() {
        val mapper = XmlMapper.builder()
            .findAndAddModules()
            .build()
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)

        val metadata = ArkistointiMetadata()

        // This must NOT throw NoSuchFieldError: _anyGetterWriter
        val bytes = mapper.writeValueAsBytes(metadata)
        assertThat(bytes).isNotEmpty()
    }

    /**
     * Asserts that jackson-dataformat-xml and jackson-databind are at the same
     * version so that any future drift is caught at build time rather than at
     * runtime in production.
     *
     * A mismatch here (e.g. dataformat-xml 2.18.3 vs databind 2.18.6) means
     * XmlBeanSerializer was compiled against a different databind than what runs —
     * the exact recipe for NoSuchFieldError on internal protected fields.
     */
    @Test
    fun `jackson-dataformat-xml version matches jackson-databind version`() {
        val databindVersion = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION
        val dataformatXmlVersion = com.fasterxml.jackson.dataformat.xml.PackageVersion.VERSION

        val databindDotted = "${databindVersion.majorVersion}.${databindVersion.minorVersion}.${databindVersion.patchLevel}"
        val xmlDotted      = "${dataformatXmlVersion.majorVersion}.${dataformatXmlVersion.minorVersion}.${dataformatXmlVersion.patchLevel}"

        assertThat(xmlDotted)
            .withFailMessage(
                "Jackson version mismatch — jackson-dataformat-xml $xmlDotted " +
                    "vs jackson-databind $databindDotted. " +
                    "XmlBeanSerializer extends BeanSerializer and accesses protected internal fields " +
                    "(e.g. _anyGetterWriter / _anyGetterWriters) that are renamed between minor " +
                    "releases. All jackson-* modules must resolve to the same version. " +
                    "Fix: remove explicit version pins from jackson-dataformat-xml and " +
                    "jackson-module-kotlin in build.gradle so Spring Boot BOM manages all " +
                    "Jackson modules consistently."
            )
            .isEqualTo(databindDotted)
    }
}

