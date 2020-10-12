package fi.elsapalvelu.elsa.web.rest.vm

import fi.elsapalvelu.elsa.service.dto.UserDTO

/**
 * View Model extending the [UserDTO], which is meant to be used in the user management UI.
 */
class ManagedUserVM : UserDTO() {

    override fun toString() = "ManagedUserVM{${super.toString()}}"
}
