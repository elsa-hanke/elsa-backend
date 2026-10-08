package fi.elsapalvelu.elsa.web.rest

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaYliopistoErikoisalaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintoopasRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.KuntaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajahallintaResourceHelper
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import java.io.File
import kotlin.test.assertNotNull

const val API_TYOSKENTELYJAKSOT = "/api/erikoistuva-laakari/tyoskentelyjaksot"

@AutoConfigureMockMvc
open class ResourceIntegrationTestBase {

    @Autowired protected lateinit var em: EntityManager
    @Autowired protected lateinit var testMockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var userRepository: UserRepository
    @Autowired protected lateinit var kayttajaRepository: KayttajaRepository
    @Autowired protected lateinit var asiakirjaRepository: AsiakirjaRepository
    @Autowired protected lateinit var opintooikeusRepository: OpintooikeusRepository
    @Autowired protected lateinit var opintoopasRepository: OpintoopasRepository
    @Autowired protected lateinit var kuntaMapper: KuntaMapper
    @Autowired protected lateinit var erikoisalaMapper: ErikoisalaMapper
    @Autowired protected lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository
    @Autowired protected lateinit var suoritusarviointiRepository: SuoritusarviointiRepository
    @Autowired protected lateinit var keskeytysaikaRepository: KeskeytysaikaRepository
    @Autowired protected lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
    @Autowired protected lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository
    @Autowired protected lateinit var koulutusjaksoRepository: KoulutusjaksoRepository
    @Autowired protected lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    protected lateinit var tyoskentelyjakso: Tyoskentelyjakso
    protected lateinit var keskeytysaika: Keskeytysaika


    protected lateinit var vastuuhenkilo: Kayttaja
    protected lateinit var virkailija: Kayttaja
    protected lateinit var user: User

    protected lateinit var tempFile1: File
    protected lateinit var tempFile2: File
    protected lateinit var mockMultipartFile1: MockMultipartFile
    protected lateinit var mockMultipartFile2: MockMultipartFile

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

    protected fun flushClear() {
        em.flush()
        em.clear()
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

    protected fun persistTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso): Long {
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        val tyoskentelyjaksoId = tyoskentelyjakso.id
        assertNotNull(tyoskentelyjaksoId)
        return tyoskentelyjaksoId
    }

    protected fun persistKoulutusjakso(koulutusjakso: Koulutusjakso): Long {
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)
        val koulutusjaksoId = koulutusjakso.id
        assertNotNull(koulutusjaksoId)
        return koulutusjaksoId
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

    protected fun initMockFiles() {
        tempFile1 = File.createTempFile("file", "pdf")
        tempFile1.writeBytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        tempFile1.deleteOnExit()
        tempFile2 = File.createTempFile("file", "png")
        tempFile2.writeBytes(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
        tempFile2.deleteOnExit()
        mockMultipartFile1 = MockMultipartFile("files", AsiakirjaHelper.ASIAKIRJA_PDF_NIMI, AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI, tempFile1.readBytes())
        mockMultipartFile2 = MockMultipartFile("files", AsiakirjaHelper.ASIAKIRJA_PNG_NIMI, AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes())
    }
}
