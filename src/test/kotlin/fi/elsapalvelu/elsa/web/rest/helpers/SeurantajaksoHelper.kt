package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Seurantajakso
import java.time.LocalDate
import java.time.ZoneId

class SeurantajaksoHelper {

    companion object {

        private const val DEFAULT_OMA_ARVIOINTI = "AAAAAAAAAA"
        private const val UPDATED_OMA_ARVIOINTI = "BBBBBBBBBB"

        private const val DEFAULT_LISAHUOMIOITA = "AAAAAAAAAA"
        private const val UPDATED_LISAHUOMIOITA = "BBBBBBBBBB"

        private const val DEFAULT_TAVOITTEET = "AAAAAAAAAA"
        private const val UPDATED_TAVOITTEET = "BBBBBBBBBB"

        private const val UPDATED_YHTEISET_MERKINNAT = "AAAAAAAAAA"
        private val UPDATED_SEURAAVA_AJANKOHTA = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val DEFAULT_PAATTYMISPAIVA: LocalDate = DEFAULT_ALKAMISPAIVA.plusDays(10)

        @JvmStatic
        fun createEntity(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            kouluttaja: Kayttaja
        ): Seurantajakso {
            return Seurantajakso(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                omaArviointi = DEFAULT_OMA_ARVIOINTI,
                lisahuomioita = DEFAULT_LISAHUOMIOITA,
                seuraavanJaksonTavoitteet = DEFAULT_TAVOITTEET,
                kouluttaja = kouluttaja
            )
        }

        @JvmStatic
        fun createUpdatedEntity(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            kouluttaja: Kayttaja
        ): Seurantajakso {
            return Seurantajakso(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                omaArviointi = UPDATED_OMA_ARVIOINTI,
                lisahuomioita = UPDATED_LISAHUOMIOITA,
                seuraavanJaksonTavoitteet = UPDATED_TAVOITTEET,
                kouluttaja = kouluttaja,
                seurantakeskustelunYhteisetMerkinnat = UPDATED_YHTEISET_MERKINNAT,
                seuraavanKeskustelunAjankohta = UPDATED_SEURAAVA_AJANKOHTA
            )
        }
    }
}
