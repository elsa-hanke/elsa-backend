package fi.elsapalvelu.elsa.service

interface ApplicationSettingService {

    fun getBooleanSettingValue(settingName: String) : Boolean?
}
