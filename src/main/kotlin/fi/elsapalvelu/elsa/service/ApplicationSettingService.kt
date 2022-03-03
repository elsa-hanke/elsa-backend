package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import java.time.Instant

interface ApplicationSettingService {

    fun getDatetimeSettingValue(settingName: ApplicationSettingTyyppi): Instant?
}
