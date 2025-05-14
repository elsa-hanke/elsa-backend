package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import java.time.LocalDate

interface ArkistointiService {

    fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<RecordProperties>,
        asiaTunnus: String,
        asiaTyyppi: String,
        tarkastaja: String?,
        tarkastusPaiva: LocalDate?,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?
    ): String

    fun laheta(yliopisto: YliopistoEnum, filePath: String, yek: Boolean)

    fun onKaytossa(yliopisto: YliopistoEnum): Boolean
}
