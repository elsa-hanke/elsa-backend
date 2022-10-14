import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/*
    Script to quickly decrypt social security numbers from the database.
    Input the values as command line arguments. The social sec number will be printed to stdout.
 */

// secret key, can be fetched from aws parameter store /prod/application/key
val secretKeyBase64 = args[0]

// hetu from jhi_user in hex format
val hetuHex = args[1]

// initialization vector from jhi_user in hex format
val ivHex = args[2]

fun decrypt(): String {
    val decodedKey = Base64.getDecoder().decode(secretKeyBase64)

    val key = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    val iv = IvParameterSpec(ivHex.decodeHex())
    val hetu = hetuHex.decodeHex()

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    return String(cipher.doFinal(hetu), StandardCharsets.UTF_8)
}

fun String.decodeHex(): ByteArray {
    return replace(" ", "")
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

println(decrypt())
