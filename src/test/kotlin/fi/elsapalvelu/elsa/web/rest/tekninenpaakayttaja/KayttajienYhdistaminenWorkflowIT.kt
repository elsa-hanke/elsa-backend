package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokalunTila
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninKommentti
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKehittamistoimenpiteet
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.domain.koejakso.KoulutussopimuksenKouluttaja
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ArviointityokaluKysymysTyyppi
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import fi.elsapalvelu.elsa.web.rest.helpers.SeurantajaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.SuoritusarviointiHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Instant

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class KayttajienYhdistaminenWorkflowIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var retained: Kayttaja
    private lateinit var source: Kayttaja
    private lateinit var otherTrainer: Kayttaja
    private lateinit var recordOwner: ErikoistuvaLaakari

    @Autowired private lateinit var snapshots: fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService

    @BeforeEach
    fun setup() {
        retained = createAccount(ERIKOISTUVA_LAAKARI)
        ErikoistuvaLaakariHelper.createEntity(em, user = retained.user)
        source = createAccount(KOULUTTAJA)
        otherTrainer = createAccount(KOULUTTAJA)
        recordOwner = ErikoistuvaLaakariHelper.createEntity(em)
        val admin = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(TEKNINEN_PAAKAYTTAJA))
        em.persist(admin)
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(
            testSamlPrincipal(admin.id, emptyMap()),
            "test",
            listOf(SimpleGrantedAuthority(TEKNINEN_PAAKAYTTAJA))
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [ERIKOISTUVA_LAAKARI, fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA])
    fun `merge preserves retained and third party archive snapshots`(role: String) {
        retained.user!!.activeAuthority = em.getReference(Authority::class.java, role)
        retained.user!!.authorities.add(em.getReference(Authority::class.java, role))
        val trainee = em.createQuery("select e from ErikoistuvaLaakari e where e.kayttaja.id=:id", ErikoistuvaLaakari::class.java)
            .setParameter("id", retained.id).singleResult
        createKoejaksonVaiheet(recordOwner, source, source, true)
        val references = listOf(archiveSnapshot(trainee), archiveSnapshot(recordOwner))
        em.flush() // Immutable database triggers protect both snapshots before the merge starts.
        mergeAccounts()
        references.forEach(::assertSnapshotUnchanged)
        assertThat(em.find(User::class.java, retained.user!!.id).authorities.map { it.name }).contains(role, KOULUTTAJA)
    }

    @Test
    fun `deleting invited trainer preserves trainee archive snapshots`() {
        source.tila = fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila.KUTSUTTU
        val sourceId = source.id!!
        val sourceUserId = source.user!!.id!!
        val reference = archiveSnapshot(recordOwner)
        em.flush()
        em.clear()
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
            "/api/tekninen-paakayttaja/kayttajat/$sourceId")
            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(
                fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaReassignedKouluttajaDTO(otherTrainer.id)
            )))
            .andExpect(status().isNoContent)
        em.flush()
        em.clear()
        assertThat(em.find(Kayttaja::class.java, sourceId)).isNull()
        assertThat(em.find(User::class.java, sourceUserId)).isNull()
        assertSnapshotUnchanged(reference)
    }

    private fun archiveSnapshot(trainee: ErikoistuvaLaakari) = snapshots.createSnapshot(
        trainee.getOpintooikeusKaytossa()!!, "archived.pdf", "application/pdf",
        fi.elsapalvelu.elsa.service.pdf.PdfTestData.certificate(),
        fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.LIITE, 0
    )

    private fun assertSnapshotUnchanged(reference: fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference) {
        val stored = em.find(fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja::class.java, reference.asiakirja.id)
        assertThat(stored.archiveSnapshot).isTrue()
        assertThat(stored.opintooikeus!!.id).isEqualTo(reference.asiakirja.opintooikeus!!.id)
        assertThat(stored.nimi).isEqualTo(reference.filename)
        assertThat(stored.tyyppi).isEqualTo(reference.contentType)
        assertThat(fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum.sha256(stored.asiakirjaData!!.data!!))
            .isEqualTo(reference.sha256)
    }

    @ParameterizedTest
    @CsvSource("TRAINER, false", "SUPERVISOR, false", "BOTH, false", "TRAINER, true", "SUPERVISOR, true", "BOTH, true")
    fun shouldMoveTrialPeriodRecordsForEitherRole(position: String, approved: Boolean) {
        val lahikouluttaja = if (position == "SUPERVISOR") otherTrainer else source
        val lahiesimies = if (position == "TRAINER") otherTrainer else source
        val phases = createKoejaksonVaiheet(recordOwner, lahikouluttaja, lahiesimies, approved)
        val unrelatedOwner = ErikoistuvaLaakariHelper.createEntity(em)
        val unrelatedPhases = createKoejaksonVaiheet(unrelatedOwner, otherTrainer, otherTrainer, approved)

        mergeAccounts()

        assertKoejaksonVaiheet(
            phases,
            if (lahikouluttaja == source) retained.id else otherTrainer.id,
            if (lahiesimies == source) retained.id else otherTrainer.id,
            recordOwner.getOpintooikeusKaytossa()!!.id,
            approved
        )
        assertKoejaksonVaiheet(
            unrelatedPhases, otherTrainer.id, otherTrainer.id, unrelatedOwner.getOpintooikeusKaytossa()!!.id, approved
        )
    }

    @ParameterizedTest
    @CsvSource("true, false, false", "false, true, false", "true, true, false", "true, false, true", "false, true, true")
    fun shouldMoveDraftAndIncompleteInitialDiscussions(hasTrainer: Boolean, hasSupervisor: Boolean, sent: Boolean) {
        val discussion = KoejaksonVaiheetHelper.createAloituskeskustelu(recordOwner, source, source).apply {
            lahikouluttaja = if (hasTrainer) source else null
            lahiesimies = if (hasSupervisor) source else null
            lahetetty = sent
        }
        em.persist(discussion)

        mergeAccounts()

        assertPersistedFields(
            discussion,
            "lahikouluttaja.id" to if (hasTrainer) retained.id else null,
            "lahiesimies.id" to if (hasSupervisor) retained.id else null,
            "opintooikeus.id" to recordOwner.getOpintooikeusKaytossa()!!.id,
            "lahetetty" to sent,
            "lahikouluttajaHyvaksynyt" to false,
            "lahiesimiesHyvaksynyt" to false,
            "koejaksonOsaamistavoitteet" to discussion.koejaksonOsaamistavoitteet
        )
    }


    @ParameterizedTest
    @CsvSource("false, false", "true, false", "false, true")
    fun shouldMoveDraftSubmittedAndReturnedTrainingContracts(sent: Boolean, returned: Boolean) {
        val correction = if (returned) "Please correct the training site" else null
        val koulutussopimus = KoejaksonVaiheetHelper.createKoulutussopimus(recordOwner, otherTrainer).apply {
            lahetetty = sent
            korjausehdotus = correction
        }
        val trainers = listOf(null, source, otherTrainer, source).mapIndexed { index, lahikouluttaja ->
            KoulutussopimuksenKouluttaja(
                koulutussopimus = koulutussopimus,
                kouluttaja = lahikouluttaja,
                toimipaikka = "Training site $index",
                lahiosoite = "Address $index",
                sopimusHyvaksytty = sent && lahikouluttaja == source,
                kuittausaika = if (sent && lahikouluttaja == source) APPROVAL_DATE else null
            )
        }
        koulutussopimus.kouluttajat = trainers.toMutableSet()
        em.persist(koulutussopimus)
        val unrelated = KoejaksonVaiheetHelper.createKoulutussopimus(ErikoistuvaLaakariHelper.createEntity(em), otherTrainer)
        val unrelatedTrainer = KoejaksonVaiheetHelper.createKoulutussopimuksenKouluttaja(unrelated, otherTrainer)
        unrelated.kouluttajat!!.add(unrelatedTrainer)
        em.persist(unrelated)

        mergeAccounts()

        assertPersistedFields(
            koulutussopimus,
            "opintooikeus.id" to recordOwner.getOpintooikeusKaytossa()!!.id,
            "vastuuhenkilo.id" to otherTrainer.id,
            "lahetetty" to sent,
            "korjausehdotus" to correction,
            "koejaksonAlkamispaiva" to koulutussopimus.koejaksonAlkamispaiva
        )
        val savedContract = em.find(KoejaksonKoulutussopimus::class.java, koulutussopimus.id)
        assertThat(savedContract.kouluttajat!!.map { it.id }).containsExactlyInAnyOrderElementsOf(trainers.map { it.id })
        trainers.forEach { lahikouluttaja ->
            assertPersistedFields(
                lahikouluttaja,
                "kouluttaja.id" to if (lahikouluttaja.kouluttaja == source) retained.id else lahikouluttaja.kouluttaja?.id,
                "koulutussopimus.id" to koulutussopimus.id,
                "toimipaikka" to lahikouluttaja.toimipaikka,
                "lahiosoite" to lahikouluttaja.lahiosoite,
                "sopimusHyvaksytty" to lahikouluttaja.sopimusHyvaksytty,
                "kuittausaika" to lahikouluttaja.kuittausaika
            )
        }
        assertPersistedFields(unrelatedTrainer, "kouluttaja.id" to otherTrainer.id, "koulutussopimus.id" to unrelated.id)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun shouldMovePendingAndCompletedEvaluationsAndFollowUpPeriods(completed: Boolean) {
        val records = listOf(source, retained, otherTrainer).map { lahikouluttaja ->
            val suoritusarviointi = SuoritusarviointiHelper.createEntity(em).apply {
                arvioinninAntaja = lahikouluttaja
                tyoskentelyjakso!!.opintooikeus = recordOwner.getOpintooikeusKaytossa()
                arviointiAika = if (completed) APPROVAL_DATE else null
                lukittu = completed
                keskenerainen = !completed
            }
            em.persist(suoritusarviointi)
            val followUp = SeurantajaksoHelper.createUpdatedEntity(recordOwner, lahikouluttaja).apply { hyvaksytty = completed }
            em.persist(followUp)
            Triple(lahikouluttaja, suoritusarviointi, followUp)
        }

        mergeAccounts()

        records.forEach { (lahikouluttaja, suoritusarviointi, followUp) ->
            val expectedTrainerId = if (lahikouluttaja == source) retained.id else lahikouluttaja.id
            assertPersistedFields(
                suoritusarviointi,
                "arvioinninAntaja.id" to expectedTrainerId,
                "tyoskentelyjakso.id" to suoritusarviointi.tyoskentelyjakso!!.id,
                "tyoskentelyjakso.opintooikeus.id" to recordOwner.getOpintooikeusKaytossa()!!.id,
                "sanallinenArviointi" to suoritusarviointi.sanallinenArviointi,
                "sanallinenItsearviointi" to suoritusarviointi.sanallinenItsearviointi,
                "arviointiAika" to suoritusarviointi.arviointiAika,
                "lukittu" to completed,
                "keskenerainen" to !completed
            )
            assertPersistedFields(
                followUp,
                "kouluttaja.id" to expectedTrainerId,
                "opintooikeus.id" to recordOwner.getOpintooikeusKaytossa()!!.id,
                "omaArviointi" to followUp.omaArviointi,
                "seurantakeskustelunYhteisetMerkinnat" to followUp.seurantakeskustelunYhteisetMerkinnat,
                "alkamispaiva" to followUp.alkamispaiva,
                "paattymispaiva" to followUp.paattymispaiva,
                "hyvaksytty" to completed
            )
        }
    }

    @ParameterizedTest
    @CsvSource("TRAINER, false", "SUPERVISOR, false", "BOTH, false", "TRAINER, true", "SUPERVISOR, true", "BOTH, true")
    fun shouldPreservePartialApprovalsAndReturnedForms(position: String, returned: Boolean) {
        val lahikouluttaja = if (position == "SUPERVISOR") otherTrainer else source
        val lahiesimies = if (position == "TRAINER") otherTrainer else source
        val correction = if (returned) "Please clarify the agreed objectives" else null
        val phases = createKoejaksonVaiheet(recordOwner, lahikouluttaja, lahiesimies, false, !returned, correction)

        mergeAccounts()

        assertKoejaksonVaiheet(
            phases,
            if (lahikouluttaja == source) retained.id else otherTrainer.id,
            if (lahiesimies == source) retained.id else otherTrainer.id,
            recordOwner.getOpintooikeusKaytossa()!!.id,
            false,
            !returned,
            correction
        )
    }

    @Test
    fun shouldMoveCommentsWithoutChangingTheirContentOrOtherAuthors() {
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em).apply {
            arvioinninAntaja = source
            tyoskentelyjakso!!.opintooikeus = recordOwner.getOpintooikeusKaytossa()
        }
        em.persist(suoritusarviointi)
        val comments = listOf(source, source, retained, recordOwner.kayttaja!!).mapIndexed { index, author ->
            SuoritusarvioinninKommentti(
                kommentoija = author,
                suoritusarviointi = suoritusarviointi,
                teksti = "Existing discussion comment $index",
                luontiaika = Instant.parse("2025-01-15T10:00:00Z").plusSeconds(index.toLong()),
                muokkausaika = Instant.parse("2025-01-16T11:00:00Z").plusSeconds(index.toLong())
            ).also { em.persist(it) }
        }

        mergeAccounts()

        assertPersistedFields(suoritusarviointi, "arvioinninAntaja.id" to retained.id)
        comments.forEach {
            assertPersistedFields(
                it,
                "kommentoija.id" to if (it.kommentoija == source) retained.id else it.kommentoija!!.id,
                "suoritusarviointi.id" to suoritusarviointi.id,
                "teksti" to it.teksti,
                "luontiaika" to it.luontiaika,
                "muokkausaika" to it.muokkausaika
            )
        }
    }

    @Test
    fun shouldMoveAllLegacyOwnedToolVersionsAndPreserveTheirQuestions() {
        val tools = listOf(source, source, retained, otherTrainer).mapIndexed { index, owner ->
            Arviointityokalu(
                kayttaja = owner,
                nimi = "Legacy tool $index",
                ohjeteksti = "Existing instructions",
                versio = index + 1L,
                kaytossa = index != 1,
                tila = if (index == 0) ArviointityokalunTila.LUONNOS else ArviointityokalunTila.JULKAISTU,
                luontiaika = Instant.parse("2025-01-15T10:00:00Z"),
                muokkausaika = Instant.parse("2025-01-16T11:00:00Z")
            ).also { tool ->
                tool.kysymykset.add(ArviointityokaluKysymys(
                    arviointityokalu = tool,
                    otsikko = "Existing question $index",
                    tyyppi = ArviointityokaluKysymysTyyppi.TEKSTIKENTTAKYSYMYS,
                    pakollinen = true,
                    jarjestysnumero = 1
                ))
                em.persist(tool)
            }
        }
        tools.take(2).forEach { it.alkuperainenId = tools.first().id }
        val questionIds = tools.map { it.kysymykset.single().id }

        mergeAccounts()

        tools.forEachIndexed { index, tool ->
            assertPersistedFields(
                tool,
                "kayttaja.id" to if (tool.kayttaja == source) retained.id else tool.kayttaja?.id,
                "nimi" to tool.nimi,
                "ohjeteksti" to tool.ohjeteksti,
                "versio" to tool.versio,
                "alkuperainenId" to tool.alkuperainenId,
                "kaytossa" to tool.kaytossa,
                "tila" to tool.tila,
                "luontiaika" to tool.luontiaika,
                "muokkausaika" to tool.muokkausaika
            )
            val saved = em.find(Arviointityokalu::class.java, tool.id)
            assertThat(saved.kysymykset.map { it.id }).containsExactly(questionIds[index])
            assertThat(saved.kysymykset.single().otsikko).isEqualTo("Existing question $index")
            assertThat(saved.kysymykset.single().pakollinen).isTrue()
        }
    }

    private fun createAccount(role: String): Kayttaja {
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(role)).apply {
            activeAuthority = Authority(role)
        }
        em.persist(user)
        return KayttajaHelper.createEntity(em, user).also { em.persist(it) }
    }

    private fun createKoejaksonVaiheet(
        owner: ErikoistuvaLaakari, lahikouluttaja: Kayttaja, lahiesimies: Kayttaja, approved: Boolean,
        trainerApproved: Boolean = approved, correction: String? = null
    ): List<Any> {
        val trainerApprovalDate = if (trainerApproved) APPROVAL_DATE else null
        val supervisorApprovalDate = if (approved) APPROVAL_DATE.plusDays(1) else null
        val phases = listOf(
            KoejaksonVaiheetHelper.createAloituskeskustelu(owner, lahikouluttaja, lahiesimies).apply {
                lahetetty = correction == null
                if (correction != null) erikoistuvanKuittausaika = null
                lahikouluttajaHyvaksynyt = trainerApproved
                lahiesimiesHyvaksynyt = approved
                lahikouluttajanKuittausaika = trainerApprovalDate
                lahiesimiehenKuittausaika = supervisorApprovalDate
                korjausehdotus = correction
            },
            KoejaksonVaiheetHelper.createValiarviointi(owner, lahikouluttaja, lahiesimies).apply {
                lahikouluttajaHyvaksynyt = trainerApproved
                lahiesimiesHyvaksynyt = approved
                lahikouluttajanKuittausaika = trainerApprovalDate
                lahiesimiehenKuittausaika = supervisorApprovalDate
                korjausehdotus = correction
                vahvuudet = "Existing strengths"
            },
            KoejaksonVaiheetHelper.createKehittamistoimenpiteet(owner, lahikouluttaja, lahiesimies).apply {
                lahikouluttajaHyvaksynyt = trainerApproved
                lahiesimiesHyvaksynyt = approved
                lahikouluttajanKuittausaika = trainerApprovalDate
                lahiesimiehenKuittausaika = supervisorApprovalDate
                korjausehdotus = correction
                kehittamistoimenpiteetRiittavat = true
            },
            KoejaksonVaiheetHelper.createLoppukeskustelu(owner, lahikouluttaja, lahiesimies).apply {
                lahikouluttajaHyvaksynyt = trainerApproved
                lahiesimiesHyvaksynyt = approved
                lahikouluttajanKuittausaika = trainerApprovalDate
                lahiesimiehenKuittausaika = supervisorApprovalDate
                korjausehdotus = correction
                jatkotoimenpiteet = "Existing next steps"
            }
        )
        phases.forEach { em.persist(it) }
        return phases
    }

    private fun assertKoejaksonVaiheet(
        phases: List<Any>, trainerId: Long?, supervisorId: Long?, studyRightId: Long?, approved: Boolean,
        trainerApproved: Boolean = approved, correction: String? = null
    ) {
        phases.forEach { phase ->
            assertPersistedFields(
                phase,
                "lahikouluttaja.id" to trainerId,
                "lahiesimies.id" to supervisorId,
                "opintooikeus.id" to studyRightId,
                "lahikouluttajaHyvaksynyt" to trainerApproved,
                "lahiesimiesHyvaksynyt" to approved,
                "lahikouluttajanKuittausaika" to if (trainerApproved) APPROVAL_DATE else null,
                "lahiesimiehenKuittausaika" to if (approved) APPROVAL_DATE.plusDays(1) else null,
                "korjausehdotus" to correction
            )
            when (phase) {
                is KoejaksonAloituskeskustelu -> assertPersistedFields(
                    phase, "koejaksonOsaamistavoitteet" to phase.koejaksonOsaamistavoitteet,
                    "lahetetty" to (correction == null), "erikoistuvanKuittausaika" to phase.erikoistuvanKuittausaika
                )
                is KoejaksonValiarviointi -> assertPersistedFields(phase, "vahvuudet" to phase.vahvuudet)
                is KoejaksonKehittamistoimenpiteet -> assertPersistedFields(phase, "kehittamistoimenpiteetRiittavat" to true)
                is KoejaksonLoppukeskustelu -> assertPersistedFields(phase, "jatkotoimenpiteet" to phase.jatkotoimenpiteet)
            }
        }
    }

    private fun assertPersistedFields(original: Any, vararg fields: Pair<String, Any?>) {
        val id = em.entityManagerFactory.persistenceUnitUtil.getIdentifier(original)
        val saved = em.find(original.javaClass, id)
        assertThat(saved).describedAs("%s %s", original.javaClass.simpleName, id).isNotNull
        assertThat(saved)
            .extracting(*fields.map { it.first }.toTypedArray())
            .containsExactly(*fields.map { it.second }.toTypedArray())
    }

    private fun mergeAccounts() {
        val sourceUserId = source.user!!.id
        em.flush()
        em.clear()
        mockMvc.perform(
            patch("/api/tekninen-paakayttaja/yhdista-kayttajatilit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(KayttajienYhdistaminenDTO(retained.id, source.id, "merged.workflow@example.com")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(Matchers.hasSize<Int>(14)))
            .andExpect(jsonPath("$[*].onnistui").value(Matchers.everyItem(Matchers.equalTo(true))))
        em.flush()
        em.clear()
        assertThat(em.find(Kayttaja::class.java, source.id)).isNull()
        assertThat(em.find(User::class.java, sourceUserId)).isNull()
        assertThat(em.find(Kayttaja::class.java, retained.id)).isNotNull
        assertThat(em.find(Kayttaja::class.java, otherTrainer.id)).isNotNull
    }

    companion object {
        private val APPROVAL_DATE = LocalDate.of(2025, 1, 15)
    }
}
