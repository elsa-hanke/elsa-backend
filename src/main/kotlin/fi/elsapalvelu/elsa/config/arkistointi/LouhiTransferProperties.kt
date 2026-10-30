package fi.elsapalvelu.elsa.config.arkistointi

import java.time.Duration

class LouhiTransferProperties {
    var operationTimeout: Duration = Duration.ofSeconds(30)
    var transferTimeout: Duration = Duration.ofMinutes(2)

    fun validate() {
        require(operationTimeout.toMillis() in 1..Int.MAX_VALUE.toLong())
        require(!transferTimeout.isNegative && transferTimeout.toMillis() > 0)
    }
}
