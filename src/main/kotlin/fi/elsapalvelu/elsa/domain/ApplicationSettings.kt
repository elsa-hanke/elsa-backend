package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.Instant
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "application_setting")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class ApplicationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_name", nullable = false)
    var settingName: ApplicationSettingTyyppi? = null

    @Column(name = "boolean_setting", nullable = true)
    var booleanSetting: Boolean? = null

    @Column(name = "datetime_setting", nullable = true)
    var datetimeSetting: Instant? = null

}
