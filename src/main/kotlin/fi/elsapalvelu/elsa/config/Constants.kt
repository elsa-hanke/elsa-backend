package fi.elsapalvelu.elsa.config

// Regex for acceptable logins
const val LOGIN_REGEX: String =
    "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$"
const val SYSTEM_ACCOUNT: String = "system"
const val ANONYMOUS_USER: String = "anonymoususer"
const val DEFAULT_LANGUAGE: String = "fi"
const val ERIKOISTUVA_LAAKARI_SISU_KOULUTUS = "urn:code:education-classification:koulutus_775101"
const val ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS = "urn:code:education-classification:koulutus_775201"
const val APP_SETTING_QUERY_OPINTOTIETO_DATA: String = "APP_SETTING_QUERY_OPINTOTIETO_DATA"
