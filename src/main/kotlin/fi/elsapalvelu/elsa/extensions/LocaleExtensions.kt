package fi.elsapalvelu.elsa.extensions

import java.util.*

fun Locale.pattern(): String {
    return if (this.language == "fi") "dd.MM.yyyy" else "yyyy-MM-dd"
}
