package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Asetus
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import jakarta.persistence.EntityManager

internal object Testisuhteet {
    fun yliopisto(em: EntityManager, nimi: YliopistoEnum): Yliopisto =
        em.createQuery("select y from Yliopisto y where y.nimi = :nimi", Yliopisto::class.java)
            .setParameter("nimi", nimi).singleResultOrNull ?: Yliopisto(nimi = nimi).also(em::persist)

    fun asetus(em: EntityManager, nimi: String): Asetus =
        em.createQuery("select a from Asetus a where a.nimi = :nimi", Asetus::class.java)
            .setParameter("nimi", nimi).singleResultOrNull ?: Asetus(nimi = nimi).also(em::persist)

    /** Liquibasen perustama akuuttilääketieteen erikoisala. */
    fun akuuttilaaketiede(em: EntityManager): Erikoisala =
        requireNotNull(em.find(Erikoisala::class.java, 1L))

    fun opintooikeusViitetiedoilla(em: EntityManager, yliopistonNimi: YliopistoEnum): Opintooikeus {
        val yliopisto = em.createQuery("select y from Yliopisto y where y.nimi = :nimi", Yliopisto::class.java)
            .setParameter("nimi", yliopistonNimi).singleResultOrNull
            ?: YliopistoHelper.createReferenceData(em, yliopistonNimi)
        return opintooikeusViitetiedoilla(em, yliopisto)
    }

    fun opintooikeusViitetiedoilla(em: EntityManager, yliopisto: Yliopisto): Opintooikeus =
        requireNotNull(ErikoistuvaLaakariHelper.createEntity(
            em, yliopisto = yliopisto, erikoisala = akuuttilaaketiede(em)
        ).getOpintooikeusKaytossa())

    fun kayttaja(em: EntityManager, kayttajatili: User?): Kayttaja {
        val olemassaOleva = kayttajatili?.id?.let { tunnus ->
            em.createQuery("select k from Kayttaja k where k.user.id = :tunnus", Kayttaja::class.java)
                .setParameter("tunnus", tunnus).singleResultOrNull
        }
        return olemassaOleva ?: KayttajaHelper.createEntity(em, kayttajatili).also(em::persist)
    }

    fun opintooikeus(em: EntityManager, kayttajatili: User?): Opintooikeus {
        val erikoistuva = kayttajatili?.id?.let { tunnus ->
            em.createQuery("select e from ErikoistuvaLaakari e where e.kayttaja.user.id = :tunnus", ErikoistuvaLaakari::class.java)
                .setParameter("tunnus", tunnus).singleResultOrNull
        } ?: ErikoistuvaLaakariHelper.createEntity(em, kayttajatili)
        return requireNotNull(erikoistuva.getOpintooikeusKaytossa())
    }
}
