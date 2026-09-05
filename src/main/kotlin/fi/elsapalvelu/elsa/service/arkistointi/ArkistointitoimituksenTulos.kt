package fi.elsapalvelu.elsa.service.arkistointi

sealed interface ArkistointitoimituksenTulos {

    data class Onnistui(
        val ulkoinenToimitustunniste: String?
    ) : ArkistointitoimituksenTulos

    data class TilapainenVirhe(
        val virhe: Arkistointivirhe
    ) : ArkistointitoimituksenTulos

    data class PysyvaVirhe(
        val virhe: Arkistointivirhe
    ) : ArkistointitoimituksenTulos
}

class Arkistointivirhe private constructor(
    val koodi: String,
    val kuvaus: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arkistointivirhe) return false

        return koodi == other.koodi && kuvaus == other.kuvaus
    }

    override fun hashCode(): Int = 31 * koodi.hashCode() + kuvaus.hashCode()

    override fun toString(): String = "Arkistointivirhe(koodi='$koodi')"

    companion object {
        const val VIRHEKOODIN_ENIMMAISPITUUS = 100
        const val VIRHEKUVAUKSEN_ENIMMAISPITUUS = 1000

        fun luo(koodi: String, kuvaus: String): Arkistointivirhe {
            require(koodi.isNotBlank()) { "Arkistointivirheen koodi ei saa olla tyhja" }
            return Arkistointivirhe(
                koodi = koodi.take(VIRHEKOODIN_ENIMMAISPITUUS),
                kuvaus = kuvaus.take(VIRHEKUVAUKSEN_ENIMMAISPITUUS)
            )
        }
    }
}
