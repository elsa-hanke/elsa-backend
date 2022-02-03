package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.service.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.TeoriakoulutusMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class TeoriakoulutusServiceImpl(
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val teoriakoulutusMapper: TeoriakoulutusMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val asiakirjaMapper: AsiakirjaMapper,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository
) : TeoriakoulutusService {

    override fun save(
        teoriakoulutusDTO: TeoriakoulutusDTO,
        todistukset: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        opintooikeusId: Long
    ): TeoriakoulutusDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let { opintooikeus ->
            if (teoriakoulutusDTO.id != null) {
                teoriakoulutusRepository.findOneByIdAndOpintooikeusId(
                    teoriakoulutusDTO.id!!,
                    opintooikeusId
                )?.let {
                    it.koulutuksenNimi = teoriakoulutusDTO.koulutuksenNimi
                    it.koulutuksenPaikka = teoriakoulutusDTO.koulutuksenPaikka
                    it.alkamispaiva = teoriakoulutusDTO.alkamispaiva
                    it.paattymispaiva = teoriakoulutusDTO.paattymispaiva
                    it.erikoistumiseenHyvaksyttavaTuntimaara =
                        teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara
                    mapTodistukset(it, todistukset, deletedAsiakirjaIds, opintooikeus)
                }?.let {
                    teoriakoulutusMapper.toDto(it)
                }
            } else {
                var teoriakoulutus = mapTodistukset(
                    teoriakoulutusMapper.toEntity(teoriakoulutusDTO),
                    todistukset,
                    deletedAsiakirjaIds,
                    opintooikeus
                ).apply { this.opintooikeus = opintooikeus }
                teoriakoulutus = teoriakoulutusRepository.save(teoriakoulutus)
                teoriakoulutusMapper.toDto(teoriakoulutus)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(
        opintooikeusId: Long
    ): List<TeoriakoulutusDTO> {
        return teoriakoulutusRepository.findAllByOpintooikeusId(opintooikeusId)
            .map(teoriakoulutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long,
        opintooikeusId: Long
    ): TeoriakoulutusDTO? {
        return teoriakoulutusRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)
            ?.let {
                teoriakoulutusMapper.toDto(it)
            }
    }

    @Transactional(readOnly = true)
    override fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<TeoriakoulutusDTO> {
        return teoriakoulutusRepository.findForSeurantajakso(
            opintooikeusId,
            alkamispaiva,
            paattymispaiva
        )
            .map(teoriakoulutusMapper::toDto)
    }

    override fun delete(
        id: Long,
        opintooikeusId: Long
    ) {
        paivakirjamerkintaRepository.findAllByTeoriakoulutusId(id).forEach {
            it.teoriakoulutus = null
        }
        teoriakoulutusRepository.deleteByIdAndOpintooikeusId(id, opintooikeusId)
    }

    private fun mapTodistukset(
        teoriakoulutus: Teoriakoulutus,
        asiakirjat: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        opintooikeus: Opintooikeus
    ): Teoriakoulutus {

        asiakirjat?.let {
            val asiakirjaEntities = it.map { asiakirjaDTO ->
                asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                    this.lisattypvm = LocalDateTime.now()
                    this.opintooikeus = opintooikeus
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
