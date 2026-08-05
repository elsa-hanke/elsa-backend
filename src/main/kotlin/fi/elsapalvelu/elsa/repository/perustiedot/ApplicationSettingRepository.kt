package fi.elsapalvelu.elsa.repository.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.ApplicationSetting
import fi.elsapalvelu.elsa.domain.perustiedot.ApplicationSettingTyyppi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationSettingRepository : JpaRepository<ApplicationSetting, Long> {

    fun findOneBySettingName(settingName: ApplicationSettingTyyppi): ApplicationSetting?
}
