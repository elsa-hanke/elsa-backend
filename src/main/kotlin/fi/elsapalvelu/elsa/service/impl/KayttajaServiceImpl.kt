package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.UserRepository
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

@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val userRepository: UserRepository,
    private val kayttajaMapper: KayttajaMapper,
    private val userMapper: UserMapper
) : KayttajaService {

    override fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO {
        var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja = kayttajaRepository.save(kayttaja)
        return kayttajaMapper.toDto(kayttaja)
    }

    override fun save(kayttajaDTO: KayttajaDTO, userDTO: UserDTO): KayttajaDTO {
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
            kayttaja = kayttajaRepository.save(kayttaja)
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

    override fun findByUserId(id: String): Optional<KayttajaDTO> {
        return kayttajaRepository.findOneByUserId(id)
            .map(kayttajaMapper::toDto)
    }

    override fun findKouluttajat(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByUserAuthorities(listOf(KOULUTTAJA))
            .map(kayttajaMapper::toDto)
    }

    override fun findVastuuhenkilot(userId: String): List<KayttajaDTO> {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opiskeluoikeus = it.opiskeluoikeudet.firstOrNull()
            return kayttajaRepository.findAllByAuthoritiesAndYliopistoAndErikoisala(
                listOf(VASTUUHENKILO),
                opiskeluoikeus?.yliopisto?.id,
                opiskeluoikeus?.erikoisala?.id
            ).map(kayttajaMapper::toDto)
        } ?: return listOf()
    }

    override fun findKouluttajatAndVastuuhenkilot(userId: String): List<KayttajaDTO> {
        val kouluttajat = kayttajaRepository.findAllByUserAuthorities(listOf(KOULUTTAJA)).map(kayttajaMapper::toDto)
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opiskeluoikeus = it.opiskeluoikeudet.firstOrNull()
            return kouluttajat + kayttajaRepository.findAllByAuthoritiesAndYliopistoAndErikoisala(
                listOf(KOULUTTAJA, VASTUUHENKILO),
                opiskeluoikeus?.yliopisto?.id,
                opiskeluoikeus?.erikoisala?.id
            ).map(kayttajaMapper::toDto)
        } ?: return kouluttajat
    }

    override fun findTeknisetPaakayttajat(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByUserAuthorities(listOf(TEKNINEN_PAAKAYTTAJA))
            .map(kayttajaMapper::toDto)
    }

    override fun delete(id: Long) {
        kayttajaRepository.deleteById(id)
    }
}
