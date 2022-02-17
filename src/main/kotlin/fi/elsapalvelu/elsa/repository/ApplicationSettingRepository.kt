package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ApplicationSetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationSettingRepository : JpaRepository<ApplicationSetting, Long> {

    fun findOneBySettingName(settingName: String) : ApplicationSetting?
}
