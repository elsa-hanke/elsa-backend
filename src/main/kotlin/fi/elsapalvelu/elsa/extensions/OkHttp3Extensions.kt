package fi.elsapalvelu.elsa.extensions

import okhttp3.Response

fun Response.responseCount(): Int = generateSequence(this) { it.priorResponse }.count()
