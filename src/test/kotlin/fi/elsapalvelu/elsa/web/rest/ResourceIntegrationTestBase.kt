package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajahallintaResourceHelper
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureMockMvc
open class ResourceIntegrationTestBase {
    @Autowired
    protected lateinit var em: EntityManager
    @Autowired
    protected lateinit var testMockMvc: MockMvc

    protected lateinit var vastuuhenkilo: Kayttaja
    protected lateinit var virkailija: Kayttaja

    protected fun initErikoistuvaLaakari(yliopisto: Yliopisto? = null, erikoisala: Erikoisala? = null): ErikoistuvaLaakari {
        val erikoistuvaLaakariUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        em.persist(erikoistuvaLaakariUser)
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, erikoistuvaLaakariUser, yliopisto = yliopisto, erikoisala = erikoisala)
        em.persist(erikoistuvaLaakari)

        return erikoistuvaLaakari
    }

    protected fun persistAndFlush(entity: Any) {
        em.persist(entity)
        em.flush()
    }

    protected fun persistYliopisto(yliopistoNimi: YliopistoEnum): Yliopisto {
        val yliopisto = Yliopisto(nimi = yliopistoNimi)
        em.persist(yliopisto)
        return yliopisto
    }

    protected fun createPersistedErikoisala(): Erikoisala {
        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        return erikoisala
    }

    protected fun createPersistedErikoistuvaLaakari(): ErikoistuvaLaakari {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)
        return erikoistuvaLaakari
    }

    protected fun createPersistedVastuuhenkilo(yliopisto: Yliopisto, erikoisala: Erikoisala): Kayttaja {
        val vastuuhenkilo = KayttajahallintaResourceHelper.createVastuuhenkilo(em, yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        return vastuuhenkilo
    }

    protected fun createPersistedVirkailija(yliopisto: Yliopisto): Kayttaja {
        val virkailija = KayttajahallintaResourceHelper.createVirkailija(em, yliopisto)
        em.persist(virkailija)
        return virkailija
    }

    protected fun createPersistedPaakayttaja(): Kayttaja {
        val paakayttaja = KayttajahallintaResourceHelper.createPaakayttaja(em)
        em.persist(paakayttaja)
        return paakayttaja
    }

    /**
     * Sets up a [Saml2Authentication] in the test security context for the given user.
     */
    protected fun setSecurityContext(userId: String, authority: String) {
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(userId, emptyMap()),
            "test",
            listOf(SimpleGrantedAuthority(authority))
        )
    }

    /**
     * Creates and persists a vastuuhenkilo [Kayttaja] with a
     * [KayttajaYliopistoErikoisala] derived from [erikoistuvaLaakari]'s active
     * opintooikeus. When [addTehtavatyyppiForKoejakso] is true (default) the
     * [VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN]
     * tehtavatyyppi is added.
     *
     * Pass the already-authenticated [user] when the vastuuhenkilo must share the
     * same [User] as the security context (VastuuhenkiloKoejaksoResourceIT pattern).
     * Omit it to create a fresh VASTUUHENKILO user (ErikoistuvaLaakariKoejaksoResourceIT pattern).
     */
    protected fun createVastuuhenkiloForKoejakso(
        erikoistuvaLaakari: ErikoistuvaLaakari,
        yliopisto: Yliopisto? = null,
        erikoisala: Erikoisala? = null,
        addTehtavatyyppiForKoejakso: Boolean = true,
        user: User? = null
    ): Kayttaja {
        val kayttajaUser = user ?: KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(VASTUUHENKILO)
        ).also { em.persist(it) }

        val vastuuhenkilo = KayttajaHelper.createEntity(em, kayttajaUser)
        em.persist(vastuuhenkilo)

        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val yliopistoErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto ?: opintooikeus?.yliopisto,
            erikoisala = erikoisala ?: opintooikeus?.erikoisala
        )
        em.persist(yliopistoErikoisala)

        if (addTehtavatyyppiForKoejakso) {
            em.findAll(VastuuhenkilonTehtavatyyppi::class)
                .firstOrNull { it.nimi == VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN }
                ?.let { yliopistoErikoisala.vastuuhenkilonTehtavat.add(it) }
        }
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoErikoisala)
        return vastuuhenkilo
    }
}
