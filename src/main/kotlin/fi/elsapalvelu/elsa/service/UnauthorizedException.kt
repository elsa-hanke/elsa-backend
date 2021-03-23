package fi.elsapalvelu.elsa.service

import java.lang.RuntimeException

class UnauthorizedException(message: String?) : RuntimeException(message) {
}
