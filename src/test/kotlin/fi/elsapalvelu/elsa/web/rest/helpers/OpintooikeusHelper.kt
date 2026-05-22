package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.web.rest.findAll
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

        val baseEntities = createBaseEntities(em)

        val opintooikeus = createOpintooikeus(
            erikoistuvaLaakari,
            baseEntities.yliopisto,
            baseEntities.erikoisala,
            baseEntities.opintoopas,
            baseEntities.asetus,
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

        val baseEntities = createBaseEntities(em)

        val erikoisala = ErikoisalaHelper.createEntityWithId(61L)

        val opintooikeus = createOpintooikeus(
            erikoistuvaLaakari,
            baseEntities.yliopisto,
            erikoisala,
            baseEntities.opintoopas,
            baseEntities.asetus,
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

    private fun createBaseEntities(em: EntityManager): BaseEntities {
        val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val opintoopas =
            OpintoopasHelper.createEntity(em, erikoisala = erikoisala)
        em.persist(opintoopas)

        val asetus = getOrCreateAsetus(em)

        em.flush()

        return BaseEntities(yliopisto, erikoisala, opintoopas, asetus)
    }

    private fun getOrCreateAsetus(em: EntityManager): Asetus {
        val existing = em.findAll(Asetus::class)
        return if (existing.isEmpty()) {
            Asetus(nimi = DEFAULT_ASETUS).also {
                em.persist(it)
                em.flush()
            }
        } else {
            existing.first()
        }
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

    private data class BaseEntities(
        val yliopisto: Yliopisto,
        val erikoisala: Erikoisala,
        val opintoopas: Opintoopas,
        val asetus: Asetus
    )
}
