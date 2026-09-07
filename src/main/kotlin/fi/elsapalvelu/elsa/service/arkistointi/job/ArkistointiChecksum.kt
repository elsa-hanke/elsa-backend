package fi.elsapalvelu.elsa.service.arkistointi.job

import org.apache.commons.codec.digest.DigestUtils

object ArkistointiChecksum {
    fun sha256(data: ByteArray): String = DigestUtils.sha256Hex(data)
}
