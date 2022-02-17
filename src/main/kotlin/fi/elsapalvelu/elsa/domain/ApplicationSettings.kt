package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
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
    @Column(name = "setting_name", nullable = false)
    var settingName: String? = null

    @Column(name = "boolean_setting", nullable = true)
    var booleanSetting: Boolean? = null

}
