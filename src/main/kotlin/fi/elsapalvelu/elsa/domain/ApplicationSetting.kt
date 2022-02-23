package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.Instant
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "application_setting")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
data class ApplicationSetting(

    @Id
    var id: Long? = null,

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_name", nullable = false)
    var settingName: ApplicationSettingTyyppi? = null,

    @Column(name = "boolean_setting", nullable = true)
    var booleanSetting: Boolean? = null,

    @Column(name = "datetime_setting", nullable = true)
    var datetimeSetting: Instant? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApplicationSetting) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ApplicationSetting{" +
        "id=$id" +
        "nimi=$settingName" +
        "}"
}
