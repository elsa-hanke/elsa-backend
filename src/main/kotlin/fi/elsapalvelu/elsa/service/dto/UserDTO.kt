package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.config.LOGIN_REGEX
import fi.elsapalvelu.elsa.domain.User
import java.time.Instant
import javax.persistence.Lob
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

open class UserDTO(
    var id: String? = null,

    @field:NotBlank
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    var login: String? = null,

    @field:Size(max = 50)
    var firstName: String? = null,

    @field:Size(max = 50)
    var lastName: String? = null,

    @field:Email
    @field:Size(min = 5, max = 254)
    var email: String? = null,

    @field:Size(max = 254)
    var phoneNumber: String? = null,

    @Lob
    var avatar: ByteArray? = null,

    var activated: Boolean = false,

    var eppn: String? = null,

    @field:Size(min = 2, max = 10)
    var langKey: String? = null,

    var createdBy: String? = null,

    var createdDate: Instant? = null,

    var lastModifiedBy: String? = null,

    var lastModifiedDate: Instant? = null,

    var authorities: Set<String>? = null,

    var impersonated: Boolean? = false
) {
    constructor(user: User) :
        this(
            user.id, user.login, user.firstName, user.lastName, user.email, user.phoneNumber,
            user.avatar,
            user.activated, user.eppn, user.langKey,
            user.createdBy, user.createdDate, user.lastModifiedBy, user.lastModifiedDate,
            user.authorities.map { it.name }.filterNotNullTo(mutableSetOf())
        )

    fun isActivated(): Boolean = activated

    override fun toString() = "UserDTO{" +
        "login='" + login + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        ", email='" + email + '\'' +
        ", phoneNumber='" + phoneNumber + '\'' +
        ", avatar=?" +
        ", activated=" + activated +
        ", eppn=" + eppn +
        ", langKey='" + langKey + '\'' +
        ", createdBy=" + createdBy +
        ", createdDate=" + createdDate +
        ", lastModifiedBy='" + lastModifiedBy + '\'' +
        ", lastModifiedDate=" + lastModifiedDate +
        ", authorities=" + authorities +
        "}"
}
