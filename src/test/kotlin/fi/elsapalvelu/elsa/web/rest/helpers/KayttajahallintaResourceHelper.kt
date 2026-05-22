package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import jakarta.persistence.EntityManager
import java.security.SecureRandom
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

object KayttajahallintaResourceHelper {

    const val DEFAULT_EPPN = "johdoe"
    val count: AtomicLong = AtomicLong(SecureRandom().nextInt().toLong() + (2L * Integer.MAX_VALUE))

    fun createKouluttaja(em: EntityManager, yliopisto: Yliopisto, erikoisala: Erikoisala): Kayttaja {
        val kouluttajaUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(KOULUTTAJA))
        em.persist(kouluttajaUser)
        return createKayttajaWithYliopistoAndErikoisala(em, kouluttajaUser, yliopisto, erikoisala)
    }

    fun createVastuuhenkilo(em: EntityManager, yliopisto: Yliopisto, erikoisala: Erikoisala): Kayttaja {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
        em.persist(vastuuhenkiloUser)
        return createKayttajaWithYliopistoAndErikoisala(em, vastuuhenkiloUser, yliopisto, erikoisala)
    }

    fun createVirkailija(em: EntityManager, yliopisto: Yliopisto): Kayttaja {
        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA))
        em.persist(virkailijaUser)
        return createKayttajaWithYliopisto(em, virkailijaUser, yliopisto)
    }

    fun createKayttajaWithYliopistoAndErikoisala(em: EntityManager, user: User, yliopisto: Yliopisto, erikoisala: Erikoisala): Kayttaja {
        val kayttaja = KayttajaHelper.createEntity(em, user)
        em.persist(kayttaja)

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(kayttaja = kayttaja, yliopisto = yliopisto, erikoisala = erikoisala)

        em.persist(yliopistoAndErikoisala)
        kayttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        return kayttaja
    }

    fun createKayttajaWithYliopisto(em: EntityManager, user: User, yliopisto: Yliopisto): Kayttaja {
        val kayttaja = KayttajaHelper.createEntity(em, user).apply { yliopistot = mutableSetOf(yliopisto) }
        em.persist(kayttaja)
        return kayttaja
    }

    fun createPaakayttaja(em: EntityManager): Kayttaja {
        val paakayttajaUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(TEKNINEN_PAAKAYTTAJA))
        em.persist(paakayttajaUser)

        val kayttaja = KayttajaHelper.createEntity(em, paakayttajaUser)
        em.persist(kayttaja)

        return kayttaja
    }

    fun persistErikoistuvaLaakari(em: EntityManager, yliopisto: Yliopisto): Long? {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()
        return erikoistuvaLaakari.kayttaja?.id
    }

    fun persistVastuuhenkilo(em: EntityManager, yliopisto: Yliopisto, erikoisala: Erikoisala): Long? {
        val vastuuhenkilo = createVastuuhenkilo(em, yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        em.flush()
        return vastuuhenkilo.id
    }

    fun persistKouluttaja(em: EntityManager, yliopisto: Yliopisto, erikoisala: Erikoisala): Long? {
        val kouluttaja = createKouluttaja(em, yliopisto, erikoisala)
        em.persist(kouluttaja)
        em.flush()
        return kouluttaja.id
    }

    fun persistPaakayttaja(em: EntityManager): Long? {
        val paakayttaja = createPaakayttaja(em)
        em.persist(paakayttaja)
        em.flush()
        return paakayttaja.id
    }

    fun persistVirkailija(em: EntityManager, yliopisto: Yliopisto): Long? {
        val virkailija = createVirkailija(em, yliopisto)
        em.persist(virkailija)
        em.flush()
        return virkailija.id
    }

    fun getDefaultErikoistuvaLaakariDTO(): KayttajahallintaErikoistuvaLaakariDTO {
        return KayttajahallintaErikoistuvaLaakariDTO(etunimi = "John", sukunimi = "DOE", sahkopostiosoite = KayttajaHelper.DEFAULT_EMAIL,
            opiskelijatunnus = "123456", opintooikeusAlkaa = LocalDate.ofEpochDay(0L), opintooikeusPaattyy = LocalDate.ofEpochDay(30L),
            osaamisenArvioinninOppaanPvm = LocalDate.ofEpochDay(0L))
    }

    fun getDefaultKayttajaDTO(): KayttajahallintaKayttajaDTO {
        return KayttajahallintaKayttajaDTO(etunimi = "John", sukunimi = "DOE", sahkoposti = KayttajaHelper.DEFAULT_EMAIL, eppn = DEFAULT_EPPN)
    }
}
