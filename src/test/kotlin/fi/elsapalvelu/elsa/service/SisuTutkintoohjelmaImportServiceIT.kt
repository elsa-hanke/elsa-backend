package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.ErikoisalaSisuTutkintoohjelma
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.service.impl.Entity
import fi.elsapalvelu.elsa.service.impl.Qualifications
import fi.elsapalvelu.elsa.service.impl.Requirement
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@SpringBootTest(classes = [ElsaBackendApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SisuTutkintoohjelmaImportServiceIT {

    @Autowired
    private lateinit var sisuTutkintoohjelmaImportService: SisuTutkintoohjelmaImportService

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var em: EntityManager

    private lateinit var qualifications: Qualifications

    private lateinit var erikoisala1: Erikoisala

    private lateinit var erikoisala2: Erikoisala

    private lateinit var erikoisala3: Erikoisala

    @BeforeAll
    fun setupForAll() {
        initQualifications()
    }

    @BeforeEach
    fun setupForEach() {
        erikoisala1 = ErikoisalaHelper.createEntity(virtaPatevyyskoodi = virtaPatevyyskoodi1)
        em.persist(erikoisala1)
        erikoisala2 = ErikoisalaHelper.createEntity(virtaPatevyyskoodi = virtaPatevyyskoodi2)
        em.persist(erikoisala2)
        erikoisala3 = ErikoisalaHelper.createEntity(virtaPatevyyskoodi = virtaPatevyyskoodi3)
        em.persist(erikoisala3)
    }

    @Test
    @Transactional
    fun shouldPersistNewSisuTutkintoohjelmaIds() {
        sisuTutkintoohjelmaImportService.import(qualifications)

        AssertSisuErikoisalaTutkintoOhjelmat()
    }

    @Test
    @Transactional
    fun shouldPersistNewSisuTutkintoohjelmaIdsAndRemoveNonExisting() {
        val existingErikoisalaSisuTutkintoohjelma1 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = removedDegreeProgrammeGroupId1, erikoisala = erikoisala1)
        em.persist(existingErikoisalaSisuTutkintoohjelma1)
        erikoisala1.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma1)

        val existingErikoisalaSisuTutkintoohjelma2 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = removedDegreeProgrammeGroupId2, erikoisala = erikoisala2)
        em.persist(existingErikoisalaSisuTutkintoohjelma2)
        erikoisala2.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma2)

        val existingErikoisalaSisuTutkintoohjelma3 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = removedDegreeProgrammeGroupId3, erikoisala = erikoisala3)
        em.persist(existingErikoisalaSisuTutkintoohjelma3)
        erikoisala3.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma3)

        val existingErikoisalaSisuTutkintoohjelma4 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = removedDegreeProgrammeGroupId4, erikoisala = erikoisala3)
        em.persist(existingErikoisalaSisuTutkintoohjelma4)
        erikoisala3.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma4)

        sisuTutkintoohjelmaImportService.import(qualifications)

        AssertSisuErikoisalaTutkintoOhjelmat()
    }

    @Test
    @Transactional
    fun shouldNotAddSisuTutkintoohjelmaIdIfAlreadyExists() {
        val existingErikoisalaSisuTutkintoohjelma1 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = degreeProgrammeGroupId1, erikoisala = erikoisala1)
        em.persist(existingErikoisalaSisuTutkintoohjelma1)
        erikoisala1.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma1)

        val existingErikoisalaSisuTutkintoohjelma2 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = degreeProgrammeGroupId2, erikoisala = erikoisala2)
        em.persist(existingErikoisalaSisuTutkintoohjelma2)
        erikoisala2.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma2)

        val existingErikoisalaSisuTutkintoohjelma3 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = degreeProgrammeGroupId3, erikoisala = erikoisala3)
        em.persist(existingErikoisalaSisuTutkintoohjelma3)
        erikoisala3.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma3)

        val existingErikoisalaSisuTutkintoohjelma4 =
            ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = degreeProgrammeGroupId4, erikoisala = erikoisala3)
        em.persist(existingErikoisalaSisuTutkintoohjelma4)
        erikoisala3.sisuTutkintoohjelmat.add(existingErikoisalaSisuTutkintoohjelma4)

        sisuTutkintoohjelmaImportService.import(qualifications)

        AssertSisuErikoisalaTutkintoOhjelmat()
    }

    @Test
    @Transactional
    fun shouldFilterDuplicateTutkintoohjelmaIdsFromExportData() {
        val existingDegreeProgrammeGroupIds = listOf(degreeProgrammeGroupId1, degreeProgrammeGroupId2)
        val entityWithExistingVirtaPatevyyskoodi = Entity(virtaPatevyyskoodi1, listOf(Requirement(existingDegreeProgrammeGroupIds)))
        qualifications = Qualifications(qualifications.entities!! + entityWithExistingVirtaPatevyyskoodi)

        sisuTutkintoohjelmaImportService.import(qualifications)

        AssertSisuErikoisalaTutkintoOhjelmat()
    }

    private fun AssertSisuErikoisalaTutkintoOhjelmat() {

        val erikoisala1SisuTutkintoohjelmat =
            erikoisalaRepository.findOneByVirtaPatevyyskoodi(virtaPatevyyskoodi1)?.sisuTutkintoohjelmat
        val erikoisala2SisuTutkintoohjelmat =
            erikoisalaRepository.findOneByVirtaPatevyyskoodi(virtaPatevyyskoodi2)?.sisuTutkintoohjelmat
        val erikoisala3SisuTutkintoohjelmat =
            erikoisalaRepository.findOneByVirtaPatevyyskoodi(virtaPatevyyskoodi3)?.sisuTutkintoohjelmat

        assertThat(erikoisala1SisuTutkintoohjelmat).isNotNull
        assertThat(erikoisala1SisuTutkintoohjelmat).size().isEqualTo(3)
        assertThat(erikoisala1SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId1 }?.erikoisala).isEqualTo(
            erikoisala1
        )
        assertThat(erikoisala1SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId2 }?.erikoisala).isEqualTo(
            erikoisala1
        )
        assertThat(erikoisala1SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId3 }?.erikoisala).isEqualTo(
            erikoisala1
        )

        assertThat(erikoisala2SisuTutkintoohjelmat).isNotNull
        assertThat(erikoisala2SisuTutkintoohjelmat).size().isEqualTo(2)
        assertThat(erikoisala2SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId4 }?.erikoisala).isEqualTo(
            erikoisala2
        )
        assertThat(erikoisala2SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId5 }?.erikoisala).isEqualTo(
            erikoisala2
        )

        assertThat(erikoisala3SisuTutkintoohjelmat).isNotNull
        assertThat(erikoisala3SisuTutkintoohjelmat).size().isEqualTo(4)
        assertThat(erikoisala3SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId6 }?.erikoisala).isEqualTo(
            erikoisala3
        )
        assertThat(erikoisala3SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId7 }?.erikoisala).isEqualTo(
            erikoisala3
        )
        assertThat(erikoisala3SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId8 }?.erikoisala).isEqualTo(
            erikoisala3
        )
        assertThat(erikoisala3SisuTutkintoohjelmat?.find { it.tutkintoohjelmaId == degreeProgrammeGroupId9 }?.erikoisala).isEqualTo(
            erikoisala3
        )
    }

    private fun initQualifications() {
        val firstEntityDegreeProgrammeGroupIds1 = listOf(degreeProgrammeGroupId1, degreeProgrammeGroupId2)
        val firstEntityDegreeProgrammeGroupIds2 = listOf(degreeProgrammeGroupId3)
        val firstEntityRequirement1 = Requirement(firstEntityDegreeProgrammeGroupIds1)
        val firstEntityRequirement2 = Requirement(firstEntityDegreeProgrammeGroupIds2)
        val firstEntityRequirements = listOf(firstEntityRequirement1, firstEntityRequirement2)
        val firstEntity = Entity(virtaPatevyyskoodi1, firstEntityRequirements)

        val secondEntityDegreeProgrammeGroupIds = listOf(degreeProgrammeGroupId4, degreeProgrammeGroupId5)
        val secondEntityRequirement = Requirement(secondEntityDegreeProgrammeGroupIds)
        val secondEntityRequirements = listOf(secondEntityRequirement)
        val secondEntity = Entity(virtaPatevyyskoodi2, secondEntityRequirements)

        val thirdEntityDegreeProgrammeGroupIds1 = listOf(degreeProgrammeGroupId6, degreeProgrammeGroupId7)
        val thirdEntityDegreeProgrammeGroupIds2 = listOf(degreeProgrammeGroupId8)
        val thirdEntityDegreeProgrammeGroupIds3 = listOf(degreeProgrammeGroupId9)
        val thirdEntityRequirement1 = Requirement(thirdEntityDegreeProgrammeGroupIds1)
        val thirdEntityRequirement2 = Requirement(thirdEntityDegreeProgrammeGroupIds2)
        val thirdEntityRequirement3 = Requirement(thirdEntityDegreeProgrammeGroupIds3)
        val thirdEntityRequirements = listOf(thirdEntityRequirement1, thirdEntityRequirement2, thirdEntityRequirement3)
        val thirdEntity = Entity(virtaPatevyyskoodi3, thirdEntityRequirements)

        val entities = listOf(firstEntity, secondEntity, thirdEntity)
        qualifications = Qualifications(entities)
    }

    companion object {

        private const val virtaPatevyyskoodi1 = "evp1"

        private const val virtaPatevyyskoodi2 = "evp2"

        private const val virtaPatevyyskoodi3 = "evp3"

        private const val degreeProgrammeGroupId1 = "dpgId1"

        private const val degreeProgrammeGroupId2 = "dpgId2"

        private const val degreeProgrammeGroupId3 = "dpgId3"

        private const val degreeProgrammeGroupId4 = "dpgId4"

        private const val degreeProgrammeGroupId5 = "dpgId5"

        private const val degreeProgrammeGroupId6 = "dpgId6"

        private const val degreeProgrammeGroupId7 = "dpgId7"

        private const val degreeProgrammeGroupId8 = "dpgId8"

        private const val degreeProgrammeGroupId9 = "dpgId9"

        private const val removedDegreeProgrammeGroupId1 = "rdpgId1"

        private const val removedDegreeProgrammeGroupId2 = "rdpgId2"

        private const val removedDegreeProgrammeGroupId3 = "rdpgId3"

        private const val removedDegreeProgrammeGroupId4 = "rdpgId4"

    }

}
