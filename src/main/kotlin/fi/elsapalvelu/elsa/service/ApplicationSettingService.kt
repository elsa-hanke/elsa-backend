package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.perustiedot.ApplicationSettingTyyppi
import java.time.Instant

interface ApplicationSettingService {

    fun getDatetimeSettingValue(settingName: ApplicationSettingTyyppi): Instant?
}
