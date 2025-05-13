package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import java.time.LocalDate

interface ArkistointiService {

    fun muodostaSahke(
        opintooikeus: Opintooikeus?,
        asiakirjat: List<Pair<Asiakirja, String>>,
        asiaTunnus: String,
        asiaTyyppi: String,
        hyvaksyja: String?,
        hyvaksymisPaiva: LocalDate?
    ): String

    fun laheta(yliopisto: YliopistoEnum, filePath: String, yek: Boolean)

    fun onKaytossa(yliopisto: YliopistoEnum): Boolean
}
