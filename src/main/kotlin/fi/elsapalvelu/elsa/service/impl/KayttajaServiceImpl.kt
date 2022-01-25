package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.UserMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityExistsException

@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val userRepository: UserRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val kayttajaMapper: KayttajaMapper,
    private val userMapper: UserMapper
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
    override fun findKouluttajat(userId: String): List<KayttajaDTO> {
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
    override fun findVastuuhenkilot(userId: String): List<KayttajaDTO> {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            return kayttajaRepository.findAllByAuthoritiesAndYliopistoAndErikoisala(
                listOf(VASTUUHENKILO),
                opintooikeus?.yliopisto?.id,
                opintooikeus?.erikoisala?.id
            ).map(kayttajaMapper::toDto)
        } ?: return listOf()
    }

    @Transactional(readOnly = true)
    override fun findKouluttajatAndVastuuhenkilot(userId: String): List<KayttajaDTO> {
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
}
