package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class CachingResourceRetrieverTest {

    private lateinit var server: HttpServer
    private val pyyntoja = AtomicInteger()
    private val retriever = CachingResourceRetriever()

    private val sisalto = ".fa { font-family: 'Font Awesome 5 Free'; }".toByteArray()

    @BeforeEach
    fun kaynnista() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/all.css") { exchange ->
            pyyntoja.incrementAndGet()
            exchange.sendResponseHeaders(200, sisalto.size.toLong())
            exchange.responseBody.use { it.write(sisalto) }
        }
        server.createContext("/puuttuu.css") { exchange ->
            pyyntoja.incrementAndGet()
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.start()
    }

    @AfterEach
    fun sammuta() {
        server.stop(0)
    }

    private fun osoite(polku: String) =
        URI("http://127.0.0.1:${server.address.port}$polku").toURL()

    @Test
    fun `hakee etaresurssin vain kerran vaikka sita pyydetaan monta kertaa`() {
        repeat(10) {
            assertThat(retriever.getByteArrayByUrl(osoite("/all.css"))).isEqualTo(sisalto)
        }

        assertThat(pyyntoja.get()).isEqualTo(1)
    }

    @Test
    fun `palauttaa saman sisallon myos syotevirtana`() {
        val virrasta = retriever.getInputStreamByUrl(osoite("/all.css"))?.use { it.readBytes() }

        assertThat(virrasta).isEqualTo(sisalto)
        assertThat(pyyntoja.get()).isEqualTo(1)
    }

    @Test
    fun `epaonnistunut haku palauttaa nullin eika sita yriteta uudelleen`() {
        repeat(10) {
            assertThat(retriever.getByteArrayByUrl(osoite("/puuttuu.css"))).isNull()
        }

        assertThat(pyyntoja.get()).isEqualTo(1)
    }

    @Test
    fun `valimuistin tyhjennys pakottaa uuden haun`() {
        retriever.getByteArrayByUrl(osoite("/all.css"))
        retriever.tyhjennaValimuisti()
        retriever.getByteArrayByUrl(osoite("/all.css"))

        assertThat(pyyntoja.get()).isEqualTo(2)
    }
}

