package fi.elsapalvelu.elsa.security.logout

import org.springframework.security.saml2.Saml2Exception
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream


class Saml2Utils {

    companion object {
        fun samlEncode(b: ByteArray?): String? {
            return Base64.getEncoder().encodeToString(b)
        }

        fun samlDecode(s: String?): ByteArray? {
            return Base64.getDecoder().decode(s)
        }

        fun samlDeflate(s: String): ByteArray? {
            return try {
                val b = ByteArrayOutputStream()
                val deflater = DeflaterOutputStream(b, Deflater(Deflater.DEFLATED, true))
                deflater.write(s.toByteArray(StandardCharsets.UTF_8))
                deflater.finish()
                b.toByteArray()
            } catch (ex: IOException) {
                throw Saml2Exception("Unable to deflate string", ex)
            }
        }

        fun samlInflate(b: ByteArray?): String? {
            return try {
                val out = ByteArrayOutputStream()
                val iout = InflaterOutputStream(out, Inflater(true))
                iout.write(b!!)
                iout.finish()
                String(out.toByteArray(), StandardCharsets.UTF_8)
            } catch (ex: IOException) {
                throw Saml2Exception("Unable to inflate string", ex)
            }
        }
    }
}
