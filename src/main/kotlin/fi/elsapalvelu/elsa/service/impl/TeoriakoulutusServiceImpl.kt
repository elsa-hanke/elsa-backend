package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.service.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.TeoriakoulutusMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TeoriakoulutusServiceImpl(
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val teoriakoulutusMapper: TeoriakoulutusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val asiakirjaMapper: AsiakirjaMapper,
) : TeoriakoulutusService {

    override fun save(
        teoriakoulutusDTO: TeoriakoulutusDTO,
        todistukset: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        userId: String
    ): TeoriakoulutusDTO? {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuvaLaakari ->
            teoriakoulutusDTO.erikoistuvaLaakariId = erikoistuvaLaakari.id
            if (teoriakoulutusDTO.id != null) {
                teoriakoulutusRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserId(
                    teoriakoulutusDTO.id!!,
                    userId
                )?.let {
                    it.koulutuksenNimi = teoriakoulutusDTO.koulutuksenNimi
                    it.koulutuksenPaikka = teoriakoulutusDTO.koulutuksenPaikka
                    it.alkamispaiva = teoriakoulutusDTO.alkamispaiva
                    it.paattymispaiva = teoriakoulutusDTO.paattymispaiva
                    it.erikoistumiseenHyvaksyttavaTuntimaara = teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara
                    mapTodistukset(it, todistukset, deletedAsiakirjaIds, erikoistuvaLaakari)
                }
                    ?.let {
                        teoriakoulutusMapper.toDto(it)
                    }
            } else {
                var teoriakoulutus = teoriakoulutusMapper.toEntity(teoriakoulutusDTO).let {
                    mapTodistukset(it, todistukset, deletedAsiakirjaIds, erikoistuvaLaakari)
                }
                teoriakoulutus = teoriakoulutusRepository.save(teoriakoulutus)
                teoriakoulutusMapper.toDto(teoriakoulutus)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(
        userId: String
    ): List<TeoriakoulutusDTO> {
        return teoriakoulutusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
            .map(teoriakoulutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long,
        userId: String
    ): TeoriakoulutusDTO? {
        return teoriakoulutusRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)?.let {
            teoriakoulutusMapper.toDto(it)
        }
    }

    override fun delete(
        id: Long,
        userId: String
    ) {
        teoriakoulutusRepository.deleteByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)
    }

    private fun mapTodistukset(
        teoriakoulutus: Teoriakoulutus,
        asiakirjat: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        kirjautunutErikoistuvaLaakari: ErikoistuvaLaakari
    ): Teoriakoulutus {

        asiakirjat?.let {
            val asiakirjaEntities = it.map { asiakirjaDTO ->
                asiakirjaDTO.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.id
                asiakirjaDTO.lisattypvm = LocalDateTime.now()

                asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                    this.teoriakoulutus = teoriakoulutus
                    this.asiakirjaData?.data =
                        BlobProxy.generateProxy(
                            asiakirjaDTO.asiakirjaData?.fileInputStream,
                            asiakirjaDTO.asiakirjaData?.fileSize!!
                        )
                }
            }

            teoriakoulutus.todistukset.addAll(asiakirjaEntities)
        }

        deletedAsiakirjaIds?.map { x -> x.toLong() }?.let {
            teoriakoulutus.todistukset.removeIf { asiakirja ->
                asiakirja.id in it
            }
        }

        return teoriakoulutus
    }
}
