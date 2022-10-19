package fi.elsapalvelu.elsa.extensions

import java.time.Period

fun Period.format(): String {
    if (this.isZero) {
        return "0 vrk"
    }

    val result = mutableListOf<String>()

    if (this.years > 0) {
        result.add("${this.years}v")
    }

    if (this.months > 0) {
        result.add("${this.months}kk")
    }

    if (this.days > 0) {
        result.add("${this.days}pv")
    }

    return result.joinToString(separator = " ")
}
