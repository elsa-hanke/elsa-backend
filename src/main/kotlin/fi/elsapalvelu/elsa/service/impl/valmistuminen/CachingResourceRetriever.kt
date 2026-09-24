package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.styledxmlparser.resolver.resource.DefaultResourceRetriever
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * PDF-mallien etaresurssien (esim. FontAwesome-tyylitiedosto ja sen kuvakefontti) hakija.
 *
 * iTextin oletustoteutus hakee jokaisen resurssin uudelleen jokaista renderointia kohden eika
 * kayta aikakatkaisua. Kun valmistumispyyntoon kuuluu tuhansia merkintoja, tama tarkoitti
 * mitattuna n. 260 ms lisaa jokaista dokumenttia kohden, ja jos isanta ei vastannut, yli 75 s
 * jokaista dokumenttia kohden ilman ylarajaa.
 *
 * Tassa toteutuksessa:
 * - jokainen resurssi haetaan korkeintaan kerran ja sailotaan muistissa,
 * - haulla on aikakatkaisu,
 * - epaonnistunut haku muistetaan, jolloin sita ei yriteta uudelleen jokaisella sivulla.
 *
 * Kuvakkeet siis renderoituvat edelleen normaalisti, mutta verkko ei voi hidastaa koontia
 * kuin kerran.
 */
@Component
class CachingResourceRetriever : DefaultResourceRetriever() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val valimuisti = ConcurrentHashMap<String, Optional<ByteArray>>()

    override fun getInputStreamByUrl(url: URL): InputStream? =
        getByteArrayByUrl(url)?.let { ByteArrayInputStream(it) }

    override fun getByteArrayByUrl(url: URL): ByteArray? {
        if (!onEtaresurssi(url)) {
            return super.getByteArrayByUrl(url)
        }
        return valimuisti.computeIfAbsent(url.toExternalForm()) { osoite ->
            Optional(hae(url, osoite))
        }.arvo
    }

    private fun hae(url: URL, osoite: String): ByteArray? = try {
        val yhteys = url.openConnection().apply {
            connectTimeout = YHTEYDEN_AIKAKATKAISU_MS
            readTimeout = LUKEMISEN_AIKAKATKAISU_MS
        }
        val data = yhteys.getInputStream().use { it.readBytes() }
        log.info("PDF-mallin etaresurssi haettu ja tallennettu valimuistiin [$osoite, ${data.size} tavua]")
        data
    } catch (e: Exception) {
        log.warn(
            "PDF-mallin etaresurssin haku epaonnistui, dokumentti muodostetaan ilman sita " +
                "[$osoite]: ${e.message}"
        )
        null
    }

    private fun onEtaresurssi(url: URL): Boolean {
        val protokolla = url.protocol?.lowercase(Locale.ROOT)
        return protokolla == "http" || protokolla == "https"
    }

    /** Nollattava vain testeissa. */
    fun tyhjennaValimuisti() = valimuisti.clear()

    /** ConcurrentHashMap ei salli null-arvoja, joten myos epaonnistunut haku kaaritaan. */
    private class Optional<T>(val arvo: T?)

    private companion object {
        const val YHTEYDEN_AIKAKATKAISU_MS = 3000
        const val LUKEMISEN_AIKAKATKAISU_MS = 5000
    }
}

