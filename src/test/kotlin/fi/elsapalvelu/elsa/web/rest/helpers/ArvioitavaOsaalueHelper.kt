package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue
import fi.elsapalvelu.elsa.domain.enumeration.CanmedsOsaalue
import java.time.LocalDate
import java.time.ZoneId

class ArvioitavaOsaalueHelper {

    companion object {

        private const val DEFAULT_TUNNUS = "AAAAAAAAAA"
        private const val UPDATED_TUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

        private const val DEFAULT_OSAAMISEN_RAJAARVO = "AAAAAAAAAA"
        private const val UPDATED_OSAAMISEN_RAJAARVO = "BBBBBBBBBB"

        private const val DEFAULT_ARVIOINTIKRITEERIT = "AAAAAAAAAA"
        private const val UPDATED_ARVIOINTIKRITEERIT = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_ROOLI: CanmedsOsaalue = CanmedsOsaalue.AMMATILLISUUS
        private val UPDATED_ROOLI: CanmedsOsaalue = CanmedsOsaalue.VUOROVAIKUTUSTAIDOT

        @JvmStatic
        fun createEntity(): ArvioitavaOsaalue {
            val arvioitavaOsaalue = ArvioitavaOsaalue(
                tunnus = DEFAULT_TUNNUS,
                nimi = DEFAULT_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                osaamisenRajaarvo = DEFAULT_OSAAMISEN_RAJAARVO,
                arviointikriteerit = DEFAULT_ARVIOINTIKRITEERIT,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU,
                rooli = DEFAULT_ROOLI
            )

            return arvioitavaOsaalue
        }

        @JvmStatic
        fun createUpdatedEntity(): ArvioitavaOsaalue {
            val arvioitavaOsaalue = ArvioitavaOsaalue(
                tunnus = UPDATED_TUNNUS,
                nimi = UPDATED_NIMI,
                kuvaus = UPDATED_KUVAUS,
                osaamisenRajaarvo = UPDATED_OSAAMISEN_RAJAARVO,
                arviointikriteerit = UPDATED_ARVIOINTIKRITEERIT,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU,
                rooli = UPDATED_ROOLI
            )

            return arvioitavaOsaalue
        }
    }
}
