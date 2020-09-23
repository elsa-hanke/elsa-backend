package fi.oulu.elsa.web.rest.vm

import fi.oulu.elsa.service.dto.UserDTO

/**
 * View Model extending the [UserDTO], which is meant to be used in the user management UI.
 */
class ManagedUserVM : UserDTO() {

    override fun toString() = "ManagedUserVM{${super.toString()}}"
}
