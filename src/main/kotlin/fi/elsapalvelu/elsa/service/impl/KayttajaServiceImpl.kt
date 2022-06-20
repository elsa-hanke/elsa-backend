package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.constants.erikoistuvaLaakariNotFoundError
import fi.elsapalvelu.elsa.service.constants.kayttajaNotFoundError
import fi.elsapalvelu.elsa.service.constants.vastuuhenkiloNotFoundError
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.ReassignedVastuuhenkilonTehtavaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.dto.VastuuhenkilonTehtavatyyppiDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ReassignedVastuuhenkilonTehtavaTyyppi
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaWrapperDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KayttajaYliopistoErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.UserMapper
import fi.elsapalvelu.elsa.service.mapper.VastuuhenkilonTehtavatyyppiMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val userRepository: UserRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val kayttajaYliopistoErikoisalaMapper: KayttajaYliopistoErikoisalaMapper,
    private val kayttajaMapper: KayttajaMapper,
    private val userMapper: UserMapper,
    private val kayttajaQueryService: KayttajaQueryService,
    private val vastuuhenkilonTehtavatyyppiRepository: VastuuhenkilonTehtavatyyppiRepository,
    private val vastuuhenkilonTehtavatyyppiMapper: VastuuhenkilonTehtavatyyppiMapper
) : KayttajaService {

    override fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO {
        var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja = kayttajaRepository.save(kayttaja)
        return kayttajaMapper.toDto(kayttaja)
    }

    override fun saveKouluttaja(
        erikoistuvaUserId: String,
        kayttajaDTO: KayttajaDTO,
        userDTO: UserDTO
    ): KayttajaDTO {
        val existingUser = userRepository.findOneByLogin(userDTO.login!!)
        return if (existingUser.isPresent) {
            val kayttaja = kayttajaRepository.findOneByUserId(existingUser.get().id!!).get()
            kayttajaMapper.toDto(kayttaja)
        } else {
            var user = userMapper.userDTOToUser(userDTO)!!
            val names = kayttajaDTO.nimi?.split(" ")
            user.firstName = names?.dropLast(1)?.joinToString()
            user.lastName = names?.last()
            user = userRepository.save(user)
            kayttajaDTO.tila = KayttajatilinTila.AKTIIVINEN
            var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
            kayttaja.user = user
            val opintooikeus =
                requireNotNull(
                    opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(
                        erikoistuvaUserId
                    )
                )
            val erikoistuvanYliopisto = requireNotNull(opintooikeus.yliopisto)
            val erikoistuvanErikoisala = requireNotNull(opintooikeus.erikoisala)
            val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
                kayttaja = kayttaja,
                yliopisto = erikoistuvanYliopisto,
                erikoisala = erikoistuvanErikoisala
            )
            kayttaja = kayttajaRepository.save(kayttaja)
            kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
            kayttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)
            kayttajaMapper.toDto(kayttaja)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KayttajaDTO> {
        return kayttajaRepository.findAll()
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KayttajaDTO> {
        return kayttajaRepository.findById(id)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByUserId(id: String): Optional<KayttajaDTO> {
        return kayttajaRepository.findOneByUserId(id)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findKouluttajatFromSameYliopisto(userId: String): List<KayttajaDTO> {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            return kayttajaRepository.findAllByAuthoritiesAndYliopistoAndErikoisala(
                listOf(KOULUTTAJA),
                opintooikeus?.yliopisto?.id,
                opintooikeus?.erikoisala?.id
            ).map(kayttajaMapper::toDto)
        } ?: return listOf()
    }

    @Transactional(readOnly = true)
    override fun findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
        userId: String,
        tehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum
    ): KayttajaDTO {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                opintooikeus?.yliopisto?.id,
                opintooikeus?.erikoisala?.id,
                tehtavatyyppi
            )?.let { v ->
                kayttajaMapper.toDto(v)
            } ?: throw EntityNotFoundException(vastuuhenkiloNotFoundError)
        } ?: throw EntityNotFoundException(erikoistuvaLaakariNotFoundError)
    }

    override fun findVastuuhenkilotByYliopisto(yliopistoId: Long): List<KayttajaDTO> {
        return kayttajaRepository.findAllByAuthoritiesAndYliopisto(listOf(VASTUUHENKILO), yliopistoId)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findKouluttajatAndVastuuhenkilotFromSameYliopisto(userId: String): List<KayttajaDTO> {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            return kayttajaRepository.findAllByAuthoritiesAndYliopistoAndErikoisala(
                listOf(KOULUTTAJA, VASTUUHENKILO),
                opintooikeus?.yliopisto?.id,
                opintooikeus?.erikoisala?.id
            ).map(kayttajaMapper::toDto)
        } ?: return listOf()
    }

    @Transactional(readOnly = true)
    override fun findTeknisetPaakayttajat(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByUserAuthorities(listOf(TEKNINEN_PAAKAYTTAJA))
            .map(kayttajaMapper::toDto)
    }

    override fun delete(id: Long) {
        kayttajaRepository.deleteById(id)
    }

    override fun updateKouluttajaYliopistoAndErikoisalaByEmail(
        erikoistuvaUserId: String,
        kouluttajaEmail: String
    ): KayttajaDTO? {
        return kayttajaRepository.findOneByUserEmail(kouluttajaEmail).orElse(null)?.let {
            val opintooikeus =
                requireNotNull(
                    opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(
                        erikoistuvaUserId
                    )
                )
            val erikoistuvanYliopisto = requireNotNull(opintooikeus.yliopisto)
            val erikoistuvanErikoisala = requireNotNull(opintooikeus.erikoisala)

            if (it.yliopistotAndErikoisalat.any { yliopistotAndErikoisalat ->
                    yliopistotAndErikoisalat.yliopisto?.id == erikoistuvanYliopisto.id &&
                        yliopistotAndErikoisalat.erikoisala?.id == erikoistuvanErikoisala.id
                }) {
                throw EntityExistsException()
            }

            val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
                kayttaja = it,
                yliopisto = erikoistuvanYliopisto,
                erikoisala = erikoistuvanErikoisala
            )
            kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
            it.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

            kayttajaMapper.toDto(it)
        }
    }

    override fun findVastuuhenkilotFromSameYliopistoAndErikoisala(kayttajaId: Long): List<KayttajaDTO> {
        val kayttaja =
            kayttajaRepository.findById(kayttajaId).orElseThrow { EntityNotFoundException(kayttajaNotFoundError) }
        val yliopistoIds = kayttaja.yliopistotAndErikoisalat.map { it.yliopisto!!.id }
        val erikoisalaIds = kayttaja.yliopistotAndErikoisalat.map { it.erikoisala!!.id }

        return kayttajaRepository.findAllByAuthoritiesAndYliopistotAndErikoisalat(
            listOf(VASTUUHENKILO),
            yliopistoIds,
            erikoisalaIds
        ).filter { it.id != kayttajaId }.map(kayttajaMapper::toDto)
    }

    override fun findByKayttajahallintaCriteriaFromSameYliopisto(
        userId: String,
        authority: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val kayttaja =
            kayttajaRepository.findOneByUserId(userId).orElseThrow { EntityNotFoundException(kayttajaNotFoundError) }
        val yliopisto =
            kayttaja.yliopistot.firstOrNull() ?: throw EntityNotFoundException("Käyttäjälle ei löydy yliopistoa")

        return kayttajaQueryService.findByAuthorityAndCriteriaAndYliopistoId(
            criteria,
            authority,
            pageable,
            yliopisto.id,
            kayttaja.user?.langKey
        )
    }

    override fun findByKayttajahallintaCriteria(
        userId: String,
        authority: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val kayttaja =
            kayttajaRepository.findOneByUserId(userId).orElseThrow { EntityNotFoundException(kayttajaNotFoundError) }
        return kayttajaQueryService.findByAuthorityAndCriteria(
            criteria,
            authority,
            pageable,
            kayttaja.user?.langKey
        )
    }

    override fun activateKayttaja(kayttajaId: Long) {
        val kayttaja =
            kayttajaRepository.findById(kayttajaId).orElseThrow { EntityNotFoundException(kayttajaNotFoundError) }
        kayttaja.tila = KayttajatilinTila.AKTIIVINEN
    }

    override fun passivateKayttaja(kayttajaId: Long) {
        val kayttaja =
            kayttajaRepository.findById(kayttajaId).orElseThrow { EntityNotFoundException(kayttajaNotFoundError) }
        kayttaja.tila = KayttajatilinTila.PASSIIVINEN
    }

    override fun saveVastuuhenkilo(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        kayttajaId: Long?
    ): KayttajahallintaKayttajaWrapperDTO {
        val kayttaja = kayttajaId?.let { id ->
            kayttajaRepository.findByIdOrNull(id)?.let {
                updateExistingKayttajahallintaKayttaja(kayttajahallintaKayttajaDTO, it)
            }
        } ?: createNewKayttaja(kayttajahallintaKayttajaDTO, setOf(VASTUUHENKILO))

        saveYliopistotAndErikoisalat(kayttajahallintaKayttajaDTO, kayttaja)
        saveReassignedTehtavat(kayttajahallintaKayttajaDTO.reassignedTehtavat)

        return KayttajahallintaKayttajaWrapperDTO(
            kayttaja = kayttajaMapper.toDto(kayttaja)
        )
    }

    override fun saveKayttajahallintaKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        authorities: Set<String>?,
        kayttajaId: Long?
    ): KayttajahallintaKayttajaWrapperDTO {
        val kayttaja = kayttajaId?.let { id ->
            kayttajaRepository.findByIdOrNull(id)?.let {
                updateExistingKayttajahallintaKayttaja(kayttajahallintaKayttajaDTO, it)
            }
        } ?: authorities?.let { createNewKayttaja(kayttajahallintaKayttajaDTO, it) }

        return KayttajahallintaKayttajaWrapperDTO(
            kayttaja = kayttaja?.let { kayttajaMapper.toDto(it) }
        )
    }

    private fun saveYliopistotAndErikoisalat(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        kayttaja: Kayttaja
    ) {
        kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat.forEach {
            if (it.id != null) {
                val existingKayttajaYliopistoErikoisala =
                    kayttajaYliopistoErikoisalaRepository.findById(it.id!!).orElseThrow {
                        getKayttajaYliopistoErikoisalaNotFoundException()
                    }
                existingKayttajaYliopistoErikoisala.vastuuhenkilonTehtavat =
                    mapVastuuhenkilonTehtavat(it.vastuuhenkilonTehtavat)
            } else {
                val kayttajaYliopistoErikoisala = kayttajaYliopistoErikoisalaMapper.toEntity(it).apply {
                    this.kayttaja = kayttaja
                }
                kayttaja.yliopistotAndErikoisalat.add(kayttajaYliopistoErikoisala)
            }
        }

        kayttaja.yliopistotAndErikoisalat.filter { existingErikoisala ->
            existingErikoisala.id !in kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat.map { it.id }
        }.forEach {
            kayttaja.yliopistotAndErikoisalat.remove(it)
        }

        kayttajaRepository.save(kayttaja)
    }

    private fun saveReassignedTehtavat(reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO>) {
        reassignedTehtavat.forEach {
            val kayttajaYliopistoErikoisala =
                kayttajaYliopistoErikoisalaRepository.findById(it.kayttajaYliopistoErikoisala?.id!!)
                    .orElseThrow { getKayttajaYliopistoErikoisalaNotFoundException() }
            val tehtava = vastuuhenkilonTehtavatyyppiRepository.findById(it.tehtavaId!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön tehtävää ei löydy") }
            if (it.tyyppi == ReassignedVastuuhenkilonTehtavaTyyppi.ADD) {
                kayttajaYliopistoErikoisala.vastuuhenkilonTehtavat.add(tehtava)
            } else {
                kayttajaYliopistoErikoisala.vastuuhenkilonTehtavat.remove(tehtava)
            }
        }
    }

    private fun mapVastuuhenkilonTehtavat(tehtavat: MutableSet<VastuuhenkilonTehtavatyyppiDTO>?): MutableSet<VastuuhenkilonTehtavatyyppi> =
        tehtavat?.map { vastuuhenkilonTehtavatyyppiMapper.toEntity(it) }?.toMutableSet() ?: mutableSetOf()

    private fun createNewKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        authorities: Set<String>
    ): Kayttaja {
        val userDTO = UserDTO(
            login = kayttajahallintaKayttajaDTO.sahkoposti,
            firstName = kayttajahallintaKayttajaDTO.etunimi,
            lastName = kayttajahallintaKayttajaDTO.sukunimi,
            email = kayttajahallintaKayttajaDTO.sahkoposti,
            activated = true,
            eppn = kayttajahallintaKayttajaDTO.eppn,
            authorities = authorities
        )
        val persistedUser = userRepository.save(userMapper.userDTOToUser(userDTO)!!)
        val kayttajaDTO = KayttajaDTO(
            tila = KayttajatilinTila.KUTSUTTU,
            yliopistot = kayttajahallintaKayttajaDTO.yliopisto?.let { mutableSetOf(it) }
        )
        val kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja.user = persistedUser
        return kayttajaRepository.save(kayttaja)
    }

    private fun updateExistingKayttajahallintaKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        existingKayttaja: Kayttaja
    ): Kayttaja {
        existingKayttaja.user?.email = kayttajahallintaKayttajaDTO.sahkoposti

        if (existingKayttaja.tila == KayttajatilinTila.KUTSUTTU) {
            existingKayttaja.user?.eppn = kayttajahallintaKayttajaDTO.eppn
        }

        return existingKayttaja
    }

    private fun getKayttajaYliopistoErikoisalaNotFoundException() =
        EntityNotFoundException("KayttajaYliopistoErikoisala entiteettiä ei löydy")
}
