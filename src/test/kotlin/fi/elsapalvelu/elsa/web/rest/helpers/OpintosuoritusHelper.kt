package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class OpintosuoritusHelper {

    companion object {

        const val DEFAULT_NIMI_FI = "AAAAAAAAAA"
        const val DEFAULT_NIMI_SV = "AAAAAAAAAB"
        const val DEFAULT_OPINTOPISTEET = 10.0
        const val DEFAULT_KURSSIKOODI = "DDDDDDDDDD"
        const val DEFAULT_ARVIO_FI = "BBBBBBBBBB"
        const val DEFAULT_ARVIO_SV = "BBBBBBBBBC"
        val DEFAULT_OPINTOSUORITUSTYYPPI = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
        val DEFAULT_SUORITUSPAIVA = LocalDate.ofEpochDay(0L)
        val DEFAULT_VANHENEMISPAIVA = LocalDate.ofEpochDay(5L)
        val DEFAULT_MUOKKAUSAIKA = LocalDate.ofEpochDay(5L).atStartOfDay(ZoneId.systemDefault()).toInstant()

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            kurssikoodi: String? = DEFAULT_KURSSIKOODI,
            tyyppiEnum: OpintosuoritusTyyppiEnum? = DEFAULT_OPINTOSUORITUSTYYPPI,
            opintooikeus: Opintooikeus? = null,
            suorituspaiva: LocalDate = DEFAULT_SUORITUSPAIVA
        ): Opintosuoritus {
            val tyyppi = OpintosuoritusTyyppi(nimi = tyyppiEnum)
            em.persist(tyyppi)

            val opintosuoritus = Opintosuoritus(
                nimi_fi = DEFAULT_NIMI_FI,
                nimi_sv = DEFAULT_NIMI_SV,
                kurssikoodi = kurssikoodi,
                suorituspaiva = suorituspaiva,
                opintopisteet = DEFAULT_OPINTOPISTEET,
                hyvaksytty = true,
                arvio_fi = DEFAULT_ARVIO_FI,
                arvio_sv = DEFAULT_ARVIO_SV,
                vanhenemispaiva = DEFAULT_VANHENEMISPAIVA,
                muokkausaika = DEFAULT_MUOKKAUSAIKA,
                tyyppi = tyyppi
            )

            var opintooikeusKaytossa = opintooikeus
            if (opintooikeus == null) {
                val erikoistuvaLaakari: ErikoistuvaLaakari
                if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                    erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                    em.persist(erikoistuvaLaakari)
                    em.flush()
                } else {
                    erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
                }

                opintooikeusKaytossa = erikoistuvaLaakari.getOpintooikeusKaytossa()
            }

            opintosuoritus.opintooikeus = opintooikeusKaytossa
            em.persist(opintosuoritus)

            return opintosuoritus
        }
    }
}
