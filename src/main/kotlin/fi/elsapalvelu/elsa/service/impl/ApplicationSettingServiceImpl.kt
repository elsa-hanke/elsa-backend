package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ApplicationSettingRepository
import fi.elsapalvelu.elsa.service.ApplicationSettingService
import org.springframework.stereotype.Service

@Service
class ApplicationSettingServiceImpl(
    private val applicationSettingRepository: ApplicationSettingRepository
) : ApplicationSettingService {
    override fun getBooleanSettingValue(settingName: String): Boolean? {
        return applicationSettingRepository.findOneBySettingName(settingName)?.booleanSetting
    }
}
