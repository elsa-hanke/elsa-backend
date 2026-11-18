package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import jakarta.persistence.EntityManager
import java.time.LocalDate

object OpintooikeusHelper {

    val DEFAULT_YLIOPISTO = YliopistoEnum.HELSINGIN_YLIOPISTO
    const val DEFAULT_ASETUS = "55/2020"
    private const val DEFAULT_OPISKELIJATUNNUS = "CCCCCCCCCC"

    private val DEFAULT_ALKAMISPAIVA: LocalDate =
        LocalDate.ofEpochDay(0L)

    private val DEFAULT_PAATTYMISPAIVA: LocalDate =
        LocalDate.now().plusYears(2)

    fun addOpintooikeusForErikoistuvaLaakari(
        em: EntityManager,
        erikoistuvaLaakari: ErikoistuvaLaakari,
        alkamispaiva: LocalDate = DEFAULT_ALKAMISPAIVA,
        paattymispaiva: LocalDate = DEFAULT_PAATTYMISPAIVA,
        tila: OpintooikeudenTila = OpintooikeudenTila.AKTIIVINEN,
        viimeinenKatselupaiva: LocalDate? = null
    ): Opintooikeus {

        val perustiedot = luoPerustiedot(em)

        val opintooikeus = createOpintooikeus(
            erikoistuvaLaakari,
            perustiedot.yliopisto,
            perustiedot.erikoisala,
            perustiedot.opintoopas,
            perustiedot.asetus,
            alkamispaiva,
            paattymispaiva,
            tila,
            viimeinenKatselupaiva
        )

        em.persist(opintooikeus)
        em.flush()

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)

        return opintooikeus
    }

    fun addOpintooikeusForYekKoulutettava(
        em: EntityManager,
        erikoistuvaLaakari: ErikoistuvaLaakari,
        alkamispaiva: LocalDate = DEFAULT_ALKAMISPAIVA,
        paattymispaiva: LocalDate = DEFAULT_PAATTYMISPAIVA,
        tila: OpintooikeudenTila = OpintooikeudenTila.AKTIIVINEN,
        viimeinenKatselupaiva: LocalDate? = null
    ): Opintooikeus {

        val erikoisala = requireNotNull(em.find(Erikoisala::class.java, YEK_ERIKOISALA_ID))
        val perustiedot = luoPerustiedot(em, erikoisala)

        val opintooikeus = createOpintooikeus(
            erikoistuvaLaakari,
            perustiedot.yliopisto,
            erikoisala,
            perustiedot.opintoopas,
            perustiedot.asetus,
            alkamispaiva,
            paattymispaiva,
            tila,
            viimeinenKatselupaiva
        )

        em.persist(opintooikeus)
        em.flush()

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)

        return opintooikeus
    }

    fun setOpintooikeusKaytossa(
        erikoistuvaLaakari: ErikoistuvaLaakari,
        opintooikeus: Opintooikeus
    ) {
        erikoistuvaLaakari.opintooikeudet.forEach {
            it.kaytossa = false
        }

        erikoistuvaLaakari.opintooikeudet
            .firstOrNull { it.id == opintooikeus.id }
            ?.kaytossa = true
    }

    // ----------------- helpers -----------------

    private fun luoPerustiedot(
        em: EntityManager,
        erikoisala: Erikoisala = ErikoisalaHelper.createEntity().also(em::persist)
    ): Perustiedot {
        val yliopisto = Testisuhteet.yliopisto(em, DEFAULT_YLIOPISTO)

        val opintoopas =
            OpintoopasHelper.createEntity(em, erikoisala = erikoisala)
        em.persist(opintoopas)

        val asetus = Testisuhteet.asetus(em, DEFAULT_ASETUS)

        em.flush()

        return Perustiedot(yliopisto, erikoisala, opintoopas, asetus)
    }

    private fun createOpintooikeus(
        erikoistuvaLaakari: ErikoistuvaLaakari,
        yliopisto: Yliopisto,
        erikoisala: Erikoisala,
        opintoopas: Opintoopas,
        asetus: Asetus,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate,
        tila: OpintooikeudenTila,
        viimeinenKatselupaiva: LocalDate?
    ): Opintooikeus =
        Opintooikeus(
            opintooikeudenMyontamispaiva = alkamispaiva,
            opintooikeudenPaattymispaiva = paattymispaiva,
            opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
            osaamisenArvioinninOppaanPvm = DEFAULT_ALKAMISPAIVA,
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala,
            opintoopas = opintoopas,
            asetus = asetus,
            kaytossa = false,
            tila = tila,
            viimeinenKatselupaiva =
                viimeinenKatselupaiva ?: paattymispaiva.plusMonths(6)
        )

    private data class Perustiedot(
        val yliopisto: Yliopisto,
        val erikoisala: Erikoisala,
        val opintoopas: Opintoopas,
        val asetus: Asetus
    )
}
