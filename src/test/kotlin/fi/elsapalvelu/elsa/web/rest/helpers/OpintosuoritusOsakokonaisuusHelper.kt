package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Opintosuoritus
import fi.elsapalvelu.elsa.domain.OpintosuoritusOsakokonaisuus
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class OpintosuoritusOsakokonaisuusHelper {

    companion object {

        const val DEFAULT_NIMI_FI = "CCCCCCCCCC"
        const val DEFAULT_NIMI_SV = "CCCCCCCCCD"
        const val DEFAULT_OPINTOPISTEET = 10.0
        const val DEFAULT_KURSSIKOODI = "DDDDDDDDDD"
        const val DEFAULT_ARVIO_FI = "DDDDDDDDDD"
        const val DEFAULT_ARVIO_SV = "DDDDDDDDDE"
        val DEFAULT_SUORITUSPAIVA = LocalDate.ofEpochDay(0L)
        val DEFAULT_VANHENEMISPAIVA = LocalDate.ofEpochDay(5L)
        val DEFAULT_MUOKKAUSAIKA = LocalDate.ofEpochDay(5L).atStartOfDay(ZoneId.systemDefault()).toInstant()

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            opintosuoritus: Opintosuoritus,
            kurssikoodi: String? = DEFAULT_KURSSIKOODI
        ): OpintosuoritusOsakokonaisuus {
            val opintosuoritusOsakokonaisuus = OpintosuoritusOsakokonaisuus(
                nimi_fi = DEFAULT_NIMI_FI,
                nimi_sv = DEFAULT_NIMI_SV,
                kurssikoodi = kurssikoodi,
                suorituspaiva = DEFAULT_SUORITUSPAIVA,
                opintopisteet = DEFAULT_OPINTOPISTEET,
                hyvaksytty = true,
                arvio_fi = DEFAULT_ARVIO_FI,
                arvio_sv = DEFAULT_ARVIO_SV,
                vanhenemispaiva = DEFAULT_VANHENEMISPAIVA,
                muokkausaika = DEFAULT_MUOKKAUSAIKA,
                opintosuoritus = opintosuoritus
            )
            em.persist(opintosuoritusOsakokonaisuus)

            return opintosuoritusOsakokonaisuus
        }
    }
}
