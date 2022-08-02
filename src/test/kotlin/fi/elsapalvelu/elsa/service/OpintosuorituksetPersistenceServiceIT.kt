package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.OpintosuoritusKurssikoodi
import fi.elsapalvelu.elsa.domain.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.OpintosuoritusRepository
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusOsakokonaisuusDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusMapper
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusOsakokonaisuusMapper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OpintosuorituksetPersistenceServiceIT {

    @Autowired
    private lateinit var opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var opintosuoritusRepository: OpintosuoritusRepository

    @Autowired
    private lateinit var opintosuoritusMapper: OpintosuoritusMapper

    @Autowired
    private lateinit var opintosuoritusOsakokonaisuusMapper: OpintosuoritusOsakokonaisuusMapper

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var opintosuoritusTyyppi1: OpintosuoritusTyyppi

    private lateinit var opintosuoritusTyyppi2: OpintosuoritusTyyppi

    @BeforeEach
    fun setup() {
        val yliopisto = Yliopisto(nimi = yliopistoEnum)
        em.persist(yliopisto)

        erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(em, yliopistoOpintooikeusId = yliopistoOpintooikeusId)

        opintosuoritusTyyppi1 = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO)
        em.persist(opintosuoritusTyyppi1)

        opintosuoritusTyyppi2 = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO)
        em.persist(opintosuoritusTyyppi2)

        em.persist(
            OpintosuoritusKurssikoodi(
                tunniste = opintosuoritus1Kurssikoodi,
                tyyppi = opintosuoritusTyyppi1,
                isOsakokonaisuus = false,
                yliopisto = yliopisto
            )
        )

        em.persist(
            OpintosuoritusKurssikoodi(
                tunniste = opintosuoritusOsakokonaisuusKurssikoodi,
                tyyppi = opintosuoritusTyyppi1,
                isOsakokonaisuus = true,
                yliopisto = yliopisto
            )
        )

        em.persist(
            OpintosuoritusKurssikoodi(
                tunniste = opintosuoritus2Kurssikoodi,
                tyyppi = opintosuoritusTyyppi2,
                isOsakokonaisuus = false,
                yliopisto = yliopisto
            )
        )

        em.flush()
    }

    @Test
    @Transactional
    fun shouldPersistopintosuoritukset() {
        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val opintosuoritusDTO1 = createOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createOpintosuoritusOsakokonaisuusDTO())
        }
        val opintosuoritusDTO2 = createOpintosuoritus2DTO()
        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopisto = yliopistoEnum, items = listOf(opintosuoritusDTO1, opintosuoritusDTO2))

        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()

        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate + 2)

        val opintosuoritus1 = opintosuoritukset.first()

        assertThat(opintosuoritus1.nimi_fi).isEqualTo(opintosuoritus1NimiFi)
        assertThat(opintosuoritus1.nimi_sv).isEqualTo(opintosuoritus1NimiSv)
        assertThat(opintosuoritus1.kurssikoodi).isEqualTo(opintosuoritus1Kurssikoodi)
        assertThat(opintosuoritus1.suorituspaiva).isEqualTo(opintosuoritus1Suorituspaiva)
        assertThat(opintosuoritus1.tyyppi).isEqualTo(opintosuoritusTyyppi1)
        assertThat(opintosuoritus1.opintopisteet).isEqualTo(opintosuoritus1Opintopisteet)
        assertThat(opintosuoritus1.hyvaksytty).isEqualTo(opintosuoritus1Hyvaksytty)
        assertThat(opintosuoritus1.arvio_fi).isEqualTo(opintosuoritus1ArvioFi)
        assertThat(opintosuoritus1.arvio_sv).isEqualTo(opintosuoritus1ArvioSv)
        assertThat(opintosuoritus1.vanhenemispaiva).isEqualTo(opintosuoritus1Vanhenemispaiva)
        assertThat(opintosuoritus1.opintooikeus).isEqualTo(opintooikeus)

        assertThat(opintosuoritus1.osakokonaisuudet).size().isEqualTo(1)

        val osakokonaisuus = opintosuoritus1.osakokonaisuudet?.first()
        assertNotNull(osakokonaisuus)

        assertThat(osakokonaisuus.nimi_fi).isEqualTo(opintosuoritusOsakokonaisuusNimiFi)
        assertThat(osakokonaisuus.nimi_sv).isEqualTo(opintosuoritusOsakokonaisuusNimiSv)
        assertThat(osakokonaisuus.kurssikoodi).isEqualTo(opintosuoritusOsakokonaisuusKurssikoodi)
        assertThat(osakokonaisuus.suorituspaiva).isEqualTo(opintosuoritusOsakokonaisuusSuorituspaiva)
        assertThat(osakokonaisuus.opintopisteet).isEqualTo(opintosuoritusOsakokonaisuusOpintopisteet)
        assertThat(osakokonaisuus.hyvaksytty).isEqualTo(opintosuoritusOsakokonaisuusHyvaksytty)
        assertThat(osakokonaisuus.arvio_fi).isEqualTo(opintosuoritusOsakokonaisuusArvioFi)
        assertThat(osakokonaisuus.arvio_sv).isEqualTo(opintosuoritusOsakokonaisuusArvioSv)
        assertThat(osakokonaisuus.vanhenemispaiva).isEqualTo(opintosuoritusOsakokonaisuusVanhenemispaiva)

        val opintosuoritus2 = opintosuoritukset[1]

        assertThat(opintosuoritus2.nimi_fi).isEqualTo(opintosuoritus2NimiFi)
        assertThat(opintosuoritus2.nimi_sv).isEqualTo(opintosuoritus2NimiSv)
        assertThat(opintosuoritus2.kurssikoodi).isEqualTo(opintosuoritus2Kurssikoodi)
        assertThat(opintosuoritus2.tyyppi).isEqualTo(opintosuoritusTyyppi2)
        assertThat(opintosuoritus2.opintopisteet).isEqualTo(opintosuoritus2Opintopisteet)
        assertThat(opintosuoritus2.hyvaksytty).isEqualTo(opintosuoritus2Hyvaksytty)
        assertThat(opintosuoritus2.arvio_fi).isEqualTo(opintosuoritus2ArvioFi)
        assertThat(opintosuoritus2.arvio_sv).isEqualTo(opintosuoritus2ArvioSv)
        assertThat(opintosuoritus2.vanhenemispaiva).isNull()
        assertThat(opintosuoritus2.opintooikeus).isEqualTo(opintooikeus)
    }



    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithMissingKurssikoodi() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            kurssikoodi = null
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithUnknownKurssikoodi() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            kurssikoodi = "ZZZ-ZZZ"
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithMissingName() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            nimi_fi = null
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithMissingSuorituspaiva() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            suorituspaiva = null
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithMissingOpintooikeusId() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            yliopistoOpintooikeusId = null
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithUnknownOpintooikeusId() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            this.yliopistoOpintooikeusId = "hgfedcba"
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusWithMissingArvio() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            hyvaksytty = null
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusIfHasOsakokonaisuusWithMissingName() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createOpintosuoritusOsakokonaisuusDTO().apply {
                nimi_fi = null
            })
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusIfHasOsakokonaisuusWithMissingSuorituspaiva() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createOpintosuoritusOsakokonaisuusDTO().apply {
                suorituspaiva = null
            })
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldNotPersistOpintosuoritusIfHasOsakokonaisuusWithMissingArvio() {
        val opintosuoritusDTO = createOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createOpintosuoritusOsakokonaisuusDTO().apply {
                hyvaksytty = null
            })
        }

        val opintosuorituksetPersistenceDTO =
            OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(opintosuoritusDTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun shouldPersistNewOpintosuoritusAndUpdateExistingOneWithOsakokonaisuus() {
        persistExistingOpintosuoritusWithOsakokonaisuus()

        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createUpdatedOpintosuoritusOsakokonaisuusDTO())
        }
        val opintosuoritusDTO2 = createOpintosuoritus2DTO()
        val opintosuorituksetPersistenceDTO = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO, opintosuoritusDTO2))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()

        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate + 1)

        val opintosuoritus1 = opintosuoritukset.first()

        assertThat(opintosuoritus1.nimi_fi).isEqualTo(opintosuoritus1UpdatedNimiFi)
        assertThat(opintosuoritus1.nimi_sv).isEqualTo(opintosuoritus1UpdatedNimiSv)
        assertThat(opintosuoritus1.suorituspaiva).isEqualTo(opintosuoritus1UpdatedSuorituspaiva)
        assertThat(opintosuoritus1.opintopisteet).isEqualTo(opintosuoritus1UpdatedOpintopisteet)
        assertThat(opintosuoritus1.hyvaksytty).isEqualTo(opintosuoritus1UpdatedHyvaksytty)
        assertThat(opintosuoritus1.arvio_fi).isEqualTo(opintosuoritus1UpdatedArvioFi)
        assertThat(opintosuoritus1.arvio_sv).isEqualTo(opintosuoritus1UpdatedArvioSv)
        assertThat(opintosuoritus1.vanhenemispaiva).isEqualTo(opintosuoritus1UpdatedVanhenemispaiva)
        assertThat(opintosuoritus1.opintooikeus).isEqualTo(opintooikeus)

        assertThat(opintosuoritus1.osakokonaisuudet).size().isEqualTo(1)

        val osakokonaisuus = opintosuoritus1.osakokonaisuudet?.first()
        assertNotNull(osakokonaisuus)

        assertThat(osakokonaisuus.nimi_fi).isEqualTo(opintosuoritusOsakokonaisuusUpdatedNimiFi)
        assertThat(osakokonaisuus.nimi_sv).isEqualTo(opintosuoritusOsakokonaisuusUpdatedNimiSv)
        assertThat(osakokonaisuus.suorituspaiva).isEqualTo(opintosuoritusOsakokonaisuusUpdatedSuorituspaiva)
        assertThat(osakokonaisuus.opintopisteet).isEqualTo(opintosuoritusOsakokonaisuusUpdatedOpintopisteet)
        assertThat(osakokonaisuus.hyvaksytty).isEqualTo(opintosuoritusOsakokonaisuusUpdatedHyvaksytty)
        assertThat(osakokonaisuus.arvio_fi).isEqualTo(opintosuoritusOsakokonaisuusUpdatedArvioFi)
        assertThat(osakokonaisuus.arvio_sv).isEqualTo(opintosuoritusOsakokonaisuusUpdatedArvioSv)
        assertThat(osakokonaisuus.vanhenemispaiva).isEqualTo(opintosuoritusOsakokonaisuusUpdatedVanhenemispaiva)

        val opintosuoritus2 = opintosuoritukset[1]

        assertThat(opintosuoritus2.nimi_fi).isEqualTo(opintosuoritus2NimiFi)
        assertThat(opintosuoritus2.nimi_sv).isEqualTo(opintosuoritus2NimiSv)
        assertThat(opintosuoritus2.kurssikoodi).isEqualTo(opintosuoritus2Kurssikoodi)
        assertThat(opintosuoritus2.tyyppi).isEqualTo(opintosuoritusTyyppi2)
        assertThat(opintosuoritus2.opintopisteet).isEqualTo(opintosuoritus2Opintopisteet)
        assertThat(opintosuoritus2.hyvaksytty).isEqualTo(opintosuoritus2Hyvaksytty)
        assertThat(opintosuoritus2.arvio_fi).isEqualTo(opintosuoritus2ArvioFi)
        assertThat(opintosuoritus2.arvio_sv).isEqualTo(opintosuoritus2ArvioSv)
        assertThat(opintosuoritus2.vanhenemispaiva).isNull()
        assertThat(opintosuoritus2.opintooikeus).isEqualTo(opintooikeus)
    }

    @Test
    @Transactional
    fun shouldUpdateExistingOpintosuoritusAndCreateNewOsakokonaisuus() {
        persistExistingOpintosuoritus()

        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createOpintosuoritusOsakokonaisuusDTO())
        }
        val opintosuorituksetPersistenceDTO = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTO
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()

        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritus1 = opintosuoritukset.first()

        assertThat(opintosuoritus1.nimi_fi).isEqualTo(opintosuoritus1UpdatedNimiFi)
        assertThat(opintosuoritus1.nimi_sv).isEqualTo(opintosuoritus1UpdatedNimiSv)
        assertThat(opintosuoritus1.suorituspaiva).isEqualTo(opintosuoritus1UpdatedSuorituspaiva)
        assertThat(opintosuoritus1.opintopisteet).isEqualTo(opintosuoritus1UpdatedOpintopisteet)
        assertThat(opintosuoritus1.hyvaksytty).isEqualTo(opintosuoritus1UpdatedHyvaksytty)
        assertThat(opintosuoritus1.arvio_fi).isEqualTo(opintosuoritus1UpdatedArvioFi)
        assertThat(opintosuoritus1.arvio_sv).isEqualTo(opintosuoritus1UpdatedArvioSv)
        assertThat(opintosuoritus1.vanhenemispaiva).isEqualTo(opintosuoritus1UpdatedVanhenemispaiva)
        assertThat(opintosuoritus1.opintooikeus).isEqualTo(opintooikeus)

        assertThat(opintosuoritus1.osakokonaisuudet).size().isEqualTo(1)

        val osakokonaisuus = opintosuoritus1.osakokonaisuudet?.first()
        assertNotNull(osakokonaisuus)

        assertThat(osakokonaisuus.nimi_fi).isEqualTo(opintosuoritusOsakokonaisuusNimiFi)
        assertThat(osakokonaisuus.nimi_sv).isEqualTo(opintosuoritusOsakokonaisuusNimiSv)
        assertThat(osakokonaisuus.suorituspaiva).isEqualTo(opintosuoritusOsakokonaisuusSuorituspaiva)
        assertThat(osakokonaisuus.opintopisteet).isEqualTo(opintosuoritusOsakokonaisuusOpintopisteet)
        assertThat(osakokonaisuus.hyvaksytty).isEqualTo(opintosuoritusOsakokonaisuusHyvaksytty)
        assertThat(osakokonaisuus.arvio_fi).isEqualTo(opintosuoritusOsakokonaisuusArvioFi)
        assertThat(osakokonaisuus.arvio_sv).isEqualTo(opintosuoritusOsakokonaisuusArvioSv)
        assertThat(osakokonaisuus.vanhenemispaiva).isEqualTo(opintosuoritusOsakokonaisuusVanhenemispaiva)
    }

    @Test
    @Transactional
    fun shouldNotUpdateOpintosuoritusWithMissingName() {
        persistExistingOpintosuoritus()

        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            nimi_fi = null
        }

        val opintosuorituksetPersistenceDTOs = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTOs
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritus = opintosuoritukset.first()
        assertThat(opintosuoritus.nimi_fi).isEqualTo(opintosuoritus1NimiFi)
    }

    @Test
    @Transactional
    fun shouldNotUpdateOpintosuoritusWithMissingOpintooikeusId() {
        persistExistingOpintosuoritus()

        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            yliopistoOpintooikeusId = null
            nimi_fi = opintosuoritus1UpdatedNimiFi
        }

        val opintosuorituksetPersistenceDTOs = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTOs
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritus = opintosuoritukset.first()
        assertThat(opintosuoritus.nimi_fi).isEqualTo(opintosuoritus1NimiFi)
    }

    @Test
    @Transactional
    fun shouldNotUpdateOpintosuoritusWithMissingKurssikoodi() {
        persistExistingOpintosuoritus()

        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            kurssikoodi = null
        }

        val opintosuorituksetPersistenceDTOs = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTOs
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritus = opintosuoritukset.first()
        assertThat(opintosuoritus.kurssikoodi).isEqualTo(opintosuoritus1Kurssikoodi)
    }

    @Test
    @Transactional
    fun shouldNotUpdateOpintosuoritusWithMissingArvio() {
        persistExistingOpintosuoritus()

        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            hyvaksytty = null
        }

        val opintosuorituksetPersistenceDTOs = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTOs
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritus = opintosuoritukset.first()
        assertThat(opintosuoritus.hyvaksytty).isEqualTo(opintosuoritus1Hyvaksytty)
    }

    @Test
    @Transactional
    fun shouldNotUpdateOpintosuoritusOsakokonaisuusWithMissingNimi() {
        persistExistingOpintosuoritusWithOsakokonaisuus()

        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createUpdatedOpintosuoritusOsakokonaisuusDTO().apply {
                nimi_fi = null
            })
        }

        val opintosuorituksetPersistenceDTOs = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTOs
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritusOsakokonaisuus = opintosuoritukset.first().osakokonaisuudet?.first()
        assertThat(opintosuoritusOsakokonaisuus?.nimi_fi).isEqualTo(opintosuoritusOsakokonaisuusNimiFi)
    }

    @Test
    @Transactional
    fun shouldNotUpdateOpintosuoritusOsakokonaisuusWithMissingArvio() {
        persistExistingOpintosuoritusWithOsakokonaisuus()

        val updatedOpintosuoritus1DTO = createUpdatedOpintosuoritus1DTO().apply {
            osakokonaisuudet = listOf(createUpdatedOpintosuoritusOsakokonaisuusDTO().apply {
                hyvaksytty = null
            })
        }

        val opintosuorituksetPersistenceDTOs = OpintosuorituksetPersistenceDTO(yliopistoEnum, listOf(updatedOpintosuoritus1DTO))
        val databaseSizeBeforeCreate = opintosuoritusRepository.findAll().size

        opintosuorituksetPersistenceService.createOrUpdateIfChanged(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            opintosuorituksetPersistenceDTOs
        )

        val opintosuoritukset = opintosuoritusRepository.findAll()
        assertThat(opintosuoritukset).hasSize(databaseSizeBeforeCreate)

        val opintosuoritusOsakokonaisuus = opintosuoritukset.first().osakokonaisuudet?.first()
        assertThat(opintosuoritusOsakokonaisuus?.hyvaksytty).isEqualTo(opintosuoritus1Hyvaksytty)
    }

    private fun persistExistingOpintosuoritus() {
        val opintosuoritusDTO = createOpintosuoritus1DTO()
        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val opintosuoritus = opintosuoritusMapper.toEntity(opintosuoritusDTO).apply {
            this.opintooikeus = opintooikeus!!
            tyyppi = opintosuoritusTyyppi1
        }

        em.persist(opintosuoritus)
        em.flush()
    }

    private fun persistExistingOpintosuoritusWithOsakokonaisuus() {
        val opintosuoritusDTO = createOpintosuoritus1DTO()
        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val opintosuoritus = opintosuoritusMapper.toEntity(opintosuoritusDTO).apply {
            this.opintooikeus = opintooikeus!!
            tyyppi = opintosuoritusTyyppi1
        }
        em.persist(opintosuoritus)
        em.flush()

        val osakokonaisuusDTO = createOpintosuoritusOsakokonaisuusDTO()
        val osakokonaisuus = opintosuoritusOsakokonaisuusMapper.toEntity(osakokonaisuusDTO).apply {
            this.opintosuoritus = opintosuoritus
        }
        opintosuoritus.osakokonaisuudet?.add(osakokonaisuus)
    }

    companion object {

        private const val opintosuoritus1NimiFi = "opintosuoritus 1 fi"
        private const val opintosuoritus1NimiSv = "opintosuoritus 1 sv"
        private const val opintosuoritus1Kurssikoodi = "XXX-YYY"
        private val opintosuoritus1Suorituspaiva = LocalDate.ofEpochDay(5L)
        private val opintosuoritus1Opintopisteet = 10.0
        private val opintosuoritus1Hyvaksytty = true
        private val opintosuoritus1ArvioFi = "opintosuoritus 1 arvio fi"
        private val opintosuoritus1ArvioSv = "opintosuoritus 1 arvio sv"
        private val opintosuoritus1Vanhenemispaiva = LocalDate.ofEpochDay(3650L)

        private const val opintosuoritusOsakokonaisuusNimiFi = "opintosuoritus osakokonaisuus fi"
        private const val opintosuoritusOsakokonaisuusNimiSv = "opintosuoritus osakokonaisuus sv"
        private const val opintosuoritusOsakokonaisuusKurssikoodi = "XXX-ZZZ"
        private val opintosuoritusOsakokonaisuusSuorituspaiva = LocalDate.ofEpochDay(7L)
        private val opintosuoritusOsakokonaisuusOpintopisteet = 2.0
        private val opintosuoritusOsakokonaisuusHyvaksytty = true
        private val opintosuoritusOsakokonaisuusArvioFi = "opintosuoritus osakokonaisuus arvio fi"
        private val opintosuoritusOsakokonaisuusArvioSv = "opintosuoritus osakokonaisuus arvio sv"
        private val opintosuoritusOsakokonaisuusVanhenemispaiva = LocalDate.ofEpochDay(700L)

        private const val opintosuoritus2NimiFi = "opintosuoritus 2 fi"
        private const val opintosuoritus2NimiSv = "opintosuoritus 2 sv"
        private const val opintosuoritus2Kurssikoodi = "YYY-XXX"
        private val opintosuoritus2Suorituspaiva = LocalDate.ofEpochDay(6L)
        private val opintosuoritus2Opintopisteet = 4.5
        private val opintosuoritus2ArvioFi = "opintosuoritus 2 arvio fi"
        private val opintosuoritus2ArvioSv = "opintosuoritus 2 arvio sv"
        private val opintosuoritus2Hyvaksytty = true

        private const val opintosuoritus1UpdatedNimiFi = "opintosuoritus 1 updated fi"
        private const val opintosuoritus1UpdatedNimiSv = "opintosuoritus 1 updated sv"
        private val opintosuoritus1UpdatedSuorituspaiva = LocalDate.ofEpochDay(10L)
        private val opintosuoritus1UpdatedOpintopisteet = 12.0
        private val opintosuoritus1UpdatedHyvaksytty = false
        private val opintosuoritus1UpdatedArvioFi = "opintosuoritus 1 updated arvio fi"
        private val opintosuoritus1UpdatedArvioSv = "opintosuoritus 1 updated arvio sv"
        private val opintosuoritus1UpdatedVanhenemispaiva = LocalDate.ofEpochDay(1000L)

        private const val opintosuoritusOsakokonaisuusUpdatedNimiFi = "opintosuoritus osakokonaisuus updated fi"
        private const val opintosuoritusOsakokonaisuusUpdatedNimiSv = "opintosuoritus osakokonaisuus updated sv"
        private val opintosuoritusOsakokonaisuusUpdatedSuorituspaiva = LocalDate.ofEpochDay(12L)
        private val opintosuoritusOsakokonaisuusUpdatedOpintopisteet = 8.0
        private val opintosuoritusOsakokonaisuusUpdatedHyvaksytty = false
        private val opintosuoritusOsakokonaisuusUpdatedArvioFi = "opintosuoritus osakokonaisuus arvio updated fi"
        private val opintosuoritusOsakokonaisuusUpdatedArvioSv = "opintosuoritus osakokonaisuus arvio updated sv"
        private val opintosuoritusOsakokonaisuusUpdatedVanhenemispaiva = LocalDate.ofEpochDay(900L)

        private val yliopistoEnum = YliopistoEnum.HELSINGIN_YLIOPISTO
        private val yliopistoOpintooikeusId = "abcdefgh"

        @JvmStatic
        fun createOpintosuoritus1DTO(): OpintosuoritusDTO {
            return OpintosuoritusDTO(
                nimi_fi = opintosuoritus1NimiFi,
                nimi_sv = opintosuoritus1NimiSv,
                kurssikoodi = opintosuoritus1Kurssikoodi,
                suorituspaiva = opintosuoritus1Suorituspaiva,
                opintopisteet = opintosuoritus1Opintopisteet,
                hyvaksytty = opintosuoritus1Hyvaksytty,
                arvio_fi = opintosuoritus1ArvioFi,
                arvio_sv = opintosuoritus1ArvioSv,
                vanhenemispaiva = opintosuoritus1Vanhenemispaiva,
                yliopistoOpintooikeusId = yliopistoOpintooikeusId
            )
        }

        @JvmStatic
        fun createOpintosuoritusOsakokonaisuusDTO(): OpintosuoritusOsakokonaisuusDTO {
            return OpintosuoritusOsakokonaisuusDTO(
                nimi_fi = opintosuoritusOsakokonaisuusNimiFi,
                nimi_sv = opintosuoritusOsakokonaisuusNimiSv,
                kurssikoodi = opintosuoritusOsakokonaisuusKurssikoodi,
                suorituspaiva = opintosuoritusOsakokonaisuusSuorituspaiva,
                opintopisteet = opintosuoritusOsakokonaisuusOpintopisteet,
                hyvaksytty = opintosuoritusOsakokonaisuusHyvaksytty,
                arvio_fi = opintosuoritusOsakokonaisuusArvioFi,
                arvio_sv = opintosuoritusOsakokonaisuusArvioSv,
                vanhenemispaiva = opintosuoritusOsakokonaisuusVanhenemispaiva
            )
        }

        @JvmStatic
        fun createUpdatedOpintosuoritus1DTO(): OpintosuoritusDTO {
            return OpintosuoritusDTO(
                nimi_fi = opintosuoritus1UpdatedNimiFi,
                nimi_sv = opintosuoritus1UpdatedNimiSv,
                kurssikoodi = opintosuoritus1Kurssikoodi,
                suorituspaiva = opintosuoritus1UpdatedSuorituspaiva,
                opintopisteet = opintosuoritus1UpdatedOpintopisteet,
                hyvaksytty = opintosuoritus1UpdatedHyvaksytty,
                arvio_fi = opintosuoritus1UpdatedArvioFi,
                arvio_sv = opintosuoritus1UpdatedArvioSv,
                vanhenemispaiva = opintosuoritus1UpdatedVanhenemispaiva,
                yliopistoOpintooikeusId = yliopistoOpintooikeusId
            )
        }

        @JvmStatic
        fun createUpdatedOpintosuoritusOsakokonaisuusDTO(): OpintosuoritusOsakokonaisuusDTO {
            return OpintosuoritusOsakokonaisuusDTO(
                nimi_fi = opintosuoritusOsakokonaisuusUpdatedNimiFi,
                nimi_sv = opintosuoritusOsakokonaisuusUpdatedNimiSv,
                kurssikoodi = opintosuoritusOsakokonaisuusKurssikoodi,
                suorituspaiva = opintosuoritusOsakokonaisuusUpdatedSuorituspaiva,
                opintopisteet = opintosuoritusOsakokonaisuusUpdatedOpintopisteet,
                hyvaksytty = opintosuoritusOsakokonaisuusUpdatedHyvaksytty,
                arvio_fi = opintosuoritusOsakokonaisuusUpdatedArvioFi,
                arvio_sv = opintosuoritusOsakokonaisuusUpdatedArvioSv,
                vanhenemispaiva = opintosuoritusOsakokonaisuusUpdatedVanhenemispaiva
            )
        }

        @JvmStatic
        fun createOpintosuoritus2DTO(): OpintosuoritusDTO {
            return OpintosuoritusDTO(
                nimi_fi = opintosuoritus2NimiFi,
                nimi_sv = opintosuoritus2NimiSv,
                kurssikoodi = opintosuoritus2Kurssikoodi,
                suorituspaiva = opintosuoritus2Suorituspaiva,
                opintopisteet = opintosuoritus2Opintopisteet,
                hyvaksytty = opintosuoritus2Hyvaksytty,
                arvio_fi = opintosuoritus2ArvioFi,
                arvio_sv = opintosuoritus2ArvioSv,
                yliopistoOpintooikeusId = yliopistoOpintooikeusId
            )
        }
    }
}
