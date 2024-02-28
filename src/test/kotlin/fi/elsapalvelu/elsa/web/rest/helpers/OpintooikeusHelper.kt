package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Asetus
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.web.rest.findAll
import jakarta.persistence.EntityManager
import java.time.LocalDate

class OpintooikeusHelper {

    companion object {
        val DEFAULT_YLIOPISTO = YliopistoEnum.HELSINGIN_YLIOPISTO
        const val DEFAULT_ASETUS = "55/2020"
        private const val DEFAULT_OPISKELIJATUNNUS = "CCCCCCCCCC"
        private val DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(20L)

        @JvmStatic
        fun addOpintooikeusForErikoistuvaLaakari(
            em: EntityManager,
            erikoistuvaLaakari: ErikoistuvaLaakari,
            alkamispaiva: LocalDate = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
            paattymispaiva: LocalDate = DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA,
            tila: OpintooikeudenTila = OpintooikeudenTila.AKTIIVINEN,
            viimeinenKatselupaiva: LocalDate? = null
        ): Opintooikeus {
            val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
            em.persist(yliopisto)

            val erikoisala = ErikoisalaHelper.createEntity()
            em.persist(erikoisala)
            em.flush()

            val opintoopas = OpintoopasHelper.createEntity(em, erikoisala = erikoisala)
            em.persist(opintoopas)
            em.flush()

            val asetus: Asetus
            if (em.findAll(Asetus::class).isEmpty()) {
                asetus = Asetus(nimi = DEFAULT_ASETUS)
                em.persist(asetus)
                em.flush()
            } else {
                asetus = em.findAll(Asetus::class).get(0)
            }

            val opintooikeus = Opintooikeus(
                opintooikeudenMyontamispaiva = alkamispaiva,
                opintooikeudenPaattymispaiva = paattymispaiva,
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                osaamisenArvioinninOppaanPvm = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
                erikoistuvaLaakari = erikoistuvaLaakari,
                yliopisto = yliopisto,
                erikoisala = erikoisala,
                opintoopas = opintoopas,
                asetus = asetus,
                kaytossa = false,
                tila = tila,
                viimeinenKatselupaiva = viimeinenKatselupaiva
            )
            em.persist(opintooikeus)
            em.flush()

            erikoistuvaLaakari.opintooikeudet.add(opintooikeus)

            return opintooikeus
        }

        @JvmStatic
        fun setOpintooikeusKaytossa(erikoistuvaLaakari: ErikoistuvaLaakari, opintooikeus: Opintooikeus) {
            erikoistuvaLaakari.opintooikeudet.forEach {
                it.kaytossa = false
            }

            erikoistuvaLaakari.opintooikeudet.find { it.id == opintooikeus.id }?.let {
                it.kaytossa = true
            }
        }

        @JvmStatic
        fun addOpintooikeusForYekKoulutettava(
            em: EntityManager,
            erikoistuvaLaakari: ErikoistuvaLaakari,
            alkamispaiva: LocalDate = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
            paattymispaiva: LocalDate = DEFAULT_OPINTOOIKEUDEN_PAATTYMISPAIVA,
            tila: OpintooikeudenTila = OpintooikeudenTila.AKTIIVINEN,
            viimeinenKatselupaiva: LocalDate? = null
        ): Opintooikeus {
            val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
            em.persist(yliopisto)

            val erikoisala = ErikoisalaHelper.createEntityWithId(61L)

            val opintoopas = OpintoopasHelper.createEntity(em, erikoisala = erikoisala)
            em.persist(opintoopas)
            em.flush()

            val asetus: Asetus
            if (em.findAll(Asetus::class).isEmpty()) {
                asetus = Asetus(nimi = DEFAULT_ASETUS)
                em.persist(asetus)
                em.flush()
            } else {
                asetus = em.findAll(Asetus::class).get(0)
            }

            val opintooikeus = Opintooikeus(
                opintooikeudenMyontamispaiva = alkamispaiva,
                opintooikeudenPaattymispaiva = paattymispaiva,
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                osaamisenArvioinninOppaanPvm = DEFAULT_OPINTOOIKEUDEN_ALKAMISPAIVA,
                erikoistuvaLaakari = erikoistuvaLaakari,
                yliopisto = yliopisto,
                erikoisala = erikoisala,
                opintoopas = opintoopas,
                asetus = asetus,
                kaytossa = false,
                tila = tila,
                viimeinenKatselupaiva = viimeinenKatselupaiva
            )
            em.persist(opintooikeus)
            em.flush()

            erikoistuvaLaakari.opintooikeudet.add(opintooikeus)

            return opintooikeus
        }
    }
}
