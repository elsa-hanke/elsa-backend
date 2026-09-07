package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ArkistointiAsiakirjaLoaderImplTest {

    @Mock
    private lateinit var repository: AsiakirjaRepository

    @Test
    fun `loads the immutable bytes when checksum matches`() {
        val data = "immutable-content".toByteArray()
        val asiakirja = Asiakirja(id = 11L, asiakirjaData = AsiakirjaData(data = data))
        whenever(repository.findOneWithDataById(11L)).thenReturn(asiakirja)
        val reference = reference(asiakirja, ArkistointiChecksum.sha256(data))

        val loaded = ArkistointiAsiakirjaLoaderImpl(repository).load(reference)

        assertThat(loaded.viite).isSameAs(reference)
        assertThat(loaded.sisalto).containsExactly(*data)
        assertThat(loaded.sisalto).isNotSameAs(data)
    }

    @Test
    fun `checksum mismatch is a controlled permanent document error`() {
        val asiakirja = Asiakirja(
            id = 11L,
            asiakirjaData = AsiakirjaData(data = "changed-content".toByteArray())
        )
        whenever(repository.findOneWithDataById(11L)).thenReturn(asiakirja)

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            ArkistointiAsiakirjaLoaderImpl(repository).load(reference(asiakirja, "a".repeat(64)))
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
    }

    @Test
    fun `missing stored document is a controlled permanent document error`() {
        val asiakirja = Asiakirja(id = 11L)
        whenever(repository.findOneWithDataById(11L)).thenReturn(null)

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            ArkistointiAsiakirjaLoaderImpl(repository).load(reference(asiakirja, "a".repeat(64)))
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
    }

    private fun reference(asiakirja: Asiakirja, checksum: String) = ArkistointiJobAsiakirja(
        id = 1L,
        asiakirja = asiakirja,
        filename = "archive.pdf",
        sha256 = checksum
    )
}
