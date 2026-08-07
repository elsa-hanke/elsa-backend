package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties

interface ArkistointiService {

    fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<RecordProperties>,
        caseId: String?,
        tarkastaja: String?,
        tarkastusPaiva: LocalDate?,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?,
        yliopisto: YliopistoEnum?,
        caseType: CaseType = CaseType.VALMISTUMINEN
    ): ArkistointiResult

    fun laheta(
        yliopisto: YliopistoEnum,
        filePath: String,
        caseType: CaseType,
        yek: Boolean,
        caseId: String? = null,
        erikoistujanNimi: String? = null
    )

    fun onKaytossa(yliopisto: YliopistoEnum, caseType: CaseType): Boolean
}
