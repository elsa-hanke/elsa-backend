package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

@ExtendWith(MockitoExtension::class)
class ArkistointiAsiakirjaLoaderImplTest {

    @Mock
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `loads the immutable bytes when checksum matches`() {
        val data = "immutable-content".toByteArray()
        val asiakirja = Asiakirja(id = 11L)
        storedBytes(data)
        val reference = reference(asiakirja, ArkistointiChecksum.sha256(data))

        val loaded = ArkistointiAsiakirjaLoaderImpl(jdbc).load(reference)

        assertThat(loaded.asiakirjaId).isEqualTo(11L)
        assertThat(loaded.asiakirjatyyppi).isEqualTo(RecordType.YHTEENVETO)
        assertThat(loaded.jarjestysnumero).isZero()
        assertThat(loaded.filename).isEqualTo("archive.pdf")
        assertThat(loaded.contentType).isEqualTo("application/pdf")
        assertThat(loaded.sha256).isEqualTo(reference.sha256)
        assertThat(loaded.sisalto).containsExactly(*data)
        assertThat(loaded.sisalto).isSameAs(data)
        assertThat(LoadedArkistointiAsiakirja::class.java.declaredFields.map { it.type })
            .doesNotContain(ArkistointiJobAsiakirja::class.java, Asiakirja::class.java)
    }

    @Test
    fun `checksum mismatch is a controlled permanent document error`() {
        val asiakirja = Asiakirja(id = 11L)
        storedBytes("changed-content".toByteArray())

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            ArkistointiAsiakirjaLoaderImpl(jdbc).load(reference(asiakirja, "a".repeat(64)))
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
    }

    @Test
    fun `missing stored document is a controlled permanent document error`() {
        val asiakirja = Asiakirja(id = 11L)
        storedBytes(null)

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            ArkistointiAsiakirjaLoaderImpl(jdbc).load(reference(asiakirja, "a".repeat(64)))
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
    }

    @Test
    fun `empty stored document is a controlled permanent document error`() {
        storedBytes(byteArrayOf())

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            ArkistointiAsiakirjaLoaderImpl(jdbc).load(reference(Asiakirja(id = 11L), "a".repeat(64)))
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_EMPTY")
    }

    private fun storedBytes(data: ByteArray?) {
        whenever(jdbc.query(any<String>(), any<RowMapper<ByteArray?>>(), eq(11L)))
            .thenReturn(if (data == null) emptyList() else listOf(data))
    }

    private fun reference(asiakirja: Asiakirja, checksum: String) = ArkistointiDocumentDescriptor(
        asiakirjaId = asiakirja.id!!,
        asiakirjatyyppi = RecordType.YHTEENVETO,
        jarjestysnumero = 0,
        filename = "archive.pdf",
        contentType = "application/pdf",
        sha256 = checksum
    )
}
