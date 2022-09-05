package fi.elsapalvelu.elsa.domain.enumeration

import fi.elsapalvelu.elsa.config.ApplicationProperties

enum class YliopistoEnum {
    HELSINGIN_YLIOPISTO,
    TAMPEREEN_YLIOPISTO,
    TURUN_YLIOPISTO,
    OULUN_YLIOPISTO,
    ITA_SUOMEN_YLIOPISTO;

    fun getOpintohallintoEmailAddress(applicationProperties: ApplicationProperties): String? {
        return when (this) {
            OULUN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().oulu
            TAMPEREEN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().tre
            TURUN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().turku
            ITA_SUOMEN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().uef
            HELSINGIN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().hki
        }
    }
}
