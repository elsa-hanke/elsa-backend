package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.required

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.repository.koulutus.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.service.koulutus.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.TeoriakoulutusMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TeoriakoulutusServiceImpl(
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val teoriakoulutusMapper: TeoriakoulutusMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val asiakirjaMapper: AsiakirjaMapper,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val pdfTextFieldValidator: PdfTextFieldValidator
) : TeoriakoulutusService {

    override fun save(
        teoriakoulutusDTO: TeoriakoulutusDTO,
        todistukset: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        opintooikeusId: Long
    ): TeoriakoulutusDTO? {
        pdfTextFieldValidator.validate(
            fields = listOf(
                "koulutuksen-nimi" to teoriakoulutusDTO.koulutuksenNimi,
                "paikka" to teoriakoulutusDTO.koulutuksenPaikka
            ),
            pdfSource = "teoriakoulutus",
            sourceId = teoriakoulutusDTO.id,
            sourceDate = teoriakoulutusDTO.alkamispaiva
        )
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let { opintooikeus ->
            if (teoriakoulutusDTO.id != null) {
                teoriakoulutusRepository.findOneByIdAndOpintooikeusId(
                    teoriakoulutusDTO.id.required(),
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
                    this.asiakirjaData?.data = asiakirjaDTO.asiakirjaData?.fileInputStream?.readAllBytes()
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
