package fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.User
data class Kirjautumistulos(val kayttaja: User, val roolit: Set<Authority>)
