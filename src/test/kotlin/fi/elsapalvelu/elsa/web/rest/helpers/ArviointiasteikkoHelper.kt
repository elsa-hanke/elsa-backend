package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Arviointiasteikko
import fi.elsapalvelu.elsa.domain.ArviointiasteikonTaso
import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikkoTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikonTasoTyyppi

class ArviointiasteikkoHelper {

    companion object {
        @JvmStatic
        fun createEntity(): Arviointiasteikko {
            val arviointiasteikko = Arviointiasteikko(
                nimi = ArviointiasteikkoTyyppi.EPA,
                tasot = mutableSetOf(
                    ArviointiasteikonTaso(
                        taso = 1,
                        nimi = ArviointiasteikonTasoTyyppi.OHJAAJAN_TOIMINNAN_SEURAAMINEN
                    ),
                    ArviointiasteikonTaso(
                        taso = 2,
                        nimi = ArviointiasteikonTasoTyyppi.TOIMINTA_SUORAN_OHJAUKSEN_ALAISENA
                    ),
                    ArviointiasteikonTaso(
                        taso = 3,
                        nimi = ArviointiasteikonTasoTyyppi.TOIMINTA_EPASUORAN_OHJAUKSEN_ALAISENA
                    ),
                    ArviointiasteikonTaso(
                        taso = 4,
                        nimi = ArviointiasteikonTasoTyyppi.TOIMINTA_ILMAN_OHJAUSTA
                    ),
                    ArviointiasteikonTaso(
                        taso = 5,
                        nimi = ArviointiasteikonTasoTyyppi.TOIMINTA_OHJAAJANA
                    )
                )
            )

            return arviointiasteikko
        }

        @JvmStatic
        fun createUpdatedEntity(): Arviointiasteikko {
            val arviointiasteikko = Arviointiasteikko(
                nimi = ArviointiasteikkoTyyppi.ETAPPI,
                tasot = mutableSetOf(
                    ArviointiasteikonTaso(
                        taso = 1,
                        nimi = ArviointiasteikonTasoTyyppi.TULOKAS
                    ),
                    ArviointiasteikonTaso(
                        taso = 2,
                        nimi = ArviointiasteikonTasoTyyppi.ALKUVAIHEEN_ERIKOISTUJA
                    ),
                    ArviointiasteikonTaso(
                        taso = 3,
                        nimi = ArviointiasteikonTasoTyyppi.EDISTYNYT_ERIKOISTUJA
                    ),
                    ArviointiasteikonTaso(
                        taso = 4,
                        nimi = ArviointiasteikonTasoTyyppi.ERIKOISLAAKARI
                    ),
                    ArviointiasteikonTaso(
                        taso = 5,
                        nimi = ArviointiasteikonTasoTyyppi.ERITYINEN_OSAAMINEN
                    )
                )
            )

            return arviointiasteikko
        }
    }
}
