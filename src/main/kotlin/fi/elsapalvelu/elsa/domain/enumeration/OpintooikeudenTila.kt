package fi.elsapalvelu.elsa.domain.enumeration

import fi.elsapalvelu.elsa.service.dto.enumeration.PeppiOpintooikeudenTila
import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila

enum class OpintooikeudenTila {
    AKTIIVINEN,
    AKTIIVINEN_EI_LASNA,
    PASSIIVINEN,
    VALMISTUNUT,
    PERUUTETTU,
    VANHENTUNUT;

    companion object {
        fun fromSisuOpintooikeudenTila(sisuOpintooikeudenTila: SisuOpintooikeudenTila?): OpintooikeudenTila? {
            return when (sisuOpintooikeudenTila) {
                SisuOpintooikeudenTila.ACTIVE -> AKTIIVINEN
                SisuOpintooikeudenTila.ACTIVE_NONATTENDING -> AKTIIVINEN_EI_LASNA
                SisuOpintooikeudenTila.NOT_STARTED,
                SisuOpintooikeudenTila.PASSIVE -> PASSIIVINEN
                SisuOpintooikeudenTila.GRADUATED -> VALMISTUNUT
                SisuOpintooikeudenTila.RESCINDED,
                SisuOpintooikeudenTila.CANCELLED_BY_ADMINISTRATION -> PERUUTETTU
                SisuOpintooikeudenTila.EXPIRED -> VANHENTUNUT
                else -> null
            }
        }

        fun fromSisuOpintooikeudenTilaStr(sisuOpintooikeudenTila: String?): OpintooikeudenTila? {
            return when (sisuOpintooikeudenTila) {
                SisuOpintooikeudenTila.ACTIVE.toString() -> AKTIIVINEN
                SisuOpintooikeudenTila.ACTIVE_NONATTENDING.toString() -> AKTIIVINEN_EI_LASNA
                SisuOpintooikeudenTila.NOT_STARTED.toString(),
                SisuOpintooikeudenTila.PASSIVE.toString() -> PASSIIVINEN
                SisuOpintooikeudenTila.GRADUATED.toString() -> VALMISTUNUT
                SisuOpintooikeudenTila.RESCINDED.toString(),
                SisuOpintooikeudenTila.CANCELLED_BY_ADMINISTRATION.toString() -> PERUUTETTU
                SisuOpintooikeudenTila.EXPIRED.toString() -> VANHENTUNUT
                else -> null
            }
        }

        fun fromPeppiOpintooikeudenTila(peppiOpintooikeudenTila: PeppiOpintooikeudenTila?): OpintooikeudenTila? {
            return when (peppiOpintooikeudenTila) {
                PeppiOpintooikeudenTila.ATTENDING -> AKTIIVINEN
                PeppiOpintooikeudenTila.ABSENT,
                PeppiOpintooikeudenTila.ABSENT_EXPENDING,
                PeppiOpintooikeudenTila.ABSENT_NON_EXPENDING,
                PeppiOpintooikeudenTila.OTHER_NON_EXPENDING -> PASSIIVINEN
                PeppiOpintooikeudenTila.GRADUATED -> VALMISTUNUT
                PeppiOpintooikeudenTila.RESIGNED -> PERUUTETTU
                else -> null
            }
        }

        fun allowedTilat(): List<OpintooikeudenTila> {
            return listOf(AKTIIVINEN, AKTIIVINEN_EI_LASNA, PASSIIVINEN)
        }

        fun endedTilat(): List<OpintooikeudenTila> {
            return listOf(PERUUTETTU, VALMISTUNUT)
        }
    }
}
