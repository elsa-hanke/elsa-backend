package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import fi.elsapalvelu.elsa.repository.ApplicationSettingRepository
import fi.elsapalvelu.elsa.service.ApplicationSettingService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ApplicationSettingServiceImpl(
    private val applicationSettingRepository: ApplicationSettingRepository
) : ApplicationSettingService {

    override fun getBooleanSettingValue(settingName: ApplicationSettingTyyppi): Boolean? {
        return applicationSettingRepository.findOneBySettingName(settingName)?.booleanSetting
    }

    override fun getDatetimeSettingValue(settingName: ApplicationSettingTyyppi): Instant? {
        return applicationSettingRepository.findOneBySettingName(settingName)?.datetimeSetting
    }
}
