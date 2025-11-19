package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import java.time.LocalDate

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

    fun laheta(yliopisto: YliopistoEnum, filePath: String, erikoisala: Erikoisala, yek: Boolean)

    fun onKaytossa(yliopisto: YliopistoEnum, caseType: CaseType): Boolean
}
