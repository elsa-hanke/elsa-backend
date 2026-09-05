package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.arkistointi.Arkistointitehtava
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointitehtavaAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.time.Instant

data class Arkistointiasiakirjaviite(
    val asiakirja: Asiakirja,
    val asiakirjatyyppi: RecordType,
    val jarjestys: Int,
    val tiedostonimi: String,
    val sisaltotyyppi: String,
    val sha256: String
)

data class ArkistointitehtavanLuontipyynto(
    val yliopisto: YliopistoEnum,
    val asiatyyppi: CaseType,
    val metatiedot: String,
    val metatietoversio: String,
    val idempotenssiavain: String,
    val asiakirjat: List<Arkistointiasiakirjaviite>
) {
    override fun toString() = "ArkistointitehtavanLuontipyynto(" +
        "yliopisto=$yliopisto, " +
        "asiatyyppi=$asiatyyppi, " +
        "metatietoversio='$metatietoversio', " +
        "asiakirjoja=${asiakirjat.size})"
}

interface ArkistointitehtavanLuontipalvelu {
    fun luo(pyynto: ArkistointitehtavanLuontipyynto): Arkistointitehtava
}

data class ArkistointitehtavienVarauspyynto(
    val enintaan: Int,
    val nyt: Instant,
    val varausPaattyy: Instant
) {
    init {
        require(enintaan > 0) { "Varattavien arkistointitehtavien maaran on oltava positiivinen" }
        require(varausPaattyy.isAfter(nyt)) { "Kasittelyvarauksen tulee paattya tulevaisuudessa" }
    }
}

interface ArkistointitehtavienVarauspalvelu {
    fun varaa(pyynto: ArkistointitehtavienVarauspyynto): List<Arkistointitehtava>
}

data class LadattuArkistointiasiakirja(
    val viite: ArkistointitehtavaAsiakirja,
    val sisalto: ByteArray
)

interface ArkistointiasiakirjanLatauspalvelu {
    fun lataa(viite: ArkistointitehtavaAsiakirja): LadattuArkistointiasiakirja
}

data class Arkistointitoimituspyynto(
    val tehtavaId: Long,
    val idempotenssiavain: String,
    val yliopisto: YliopistoEnum,
    val asiatyyppi: CaseType,
    val metatiedot: String,
    val metatietoversio: String,
    val asiakirjat: List<LadattuArkistointiasiakirja>
) {
    override fun toString() = "Arkistointitoimituspyynto(" +
        "tehtavaId=$tehtavaId, " +
        "yliopisto=$yliopisto, " +
        "asiatyyppi=$asiatyyppi, " +
        "metatietoversio='$metatietoversio', " +
        "asiakirjoja=${asiakirjat.size})"
}

interface ArkistointitehtavaAdapter {
    val yliopisto: YliopistoEnum

    fun arkistoi(pyynto: Arkistointitoimituspyynto): ArkistointitoimituksenTulos
}
