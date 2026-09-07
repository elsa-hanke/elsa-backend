package fi.elsapalvelu.elsa.domain.arkistointi

enum class ArkistointiJobStatus {
    ODOTTAA,
    KASITTELYSSA,
    LAHETETTY,
    VAATII_TOIMENPITEITA;

    fun canTransitionTo(newStatus: ArkistointiJobStatus): Boolean =
        newStatus in sallitutSiirtymat.getValue(this)

    companion object {
        private val sallitutSiirtymat = mapOf(
            ODOTTAA to setOf(KASITTELYSSA),
            KASITTELYSSA to setOf(ODOTTAA, LAHETETTY, VAATII_TOIMENPITEITA),
            LAHETETTY to emptySet(),
            VAATII_TOIMENPITEITA to setOf(ODOTTAA)
        )
    }
}
