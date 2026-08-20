package fi.elsapalvelu.elsa

fun <T : Any> T?.required(): T = requireNotNull(this)
