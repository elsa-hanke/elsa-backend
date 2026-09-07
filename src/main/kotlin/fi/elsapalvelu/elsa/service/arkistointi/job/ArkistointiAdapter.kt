package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum

interface ArkistointiAdapter {
    val university: YliopistoEnum

    fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult
}
