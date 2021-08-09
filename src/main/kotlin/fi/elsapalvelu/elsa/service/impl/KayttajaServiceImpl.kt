package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.KOULUTTAJA
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
    override fun findAll(): MutableList<KayttajaDTO> {
        return kayttajaRepository.findAll()
            .mapTo(mutableListOf(), kayttajaMapper::toDto)
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

    override fun findKouluttajat(): MutableList<KayttajaDTO> {
        return kayttajaRepository.findAllByUserAuthority(KOULUTTAJA)
            .mapTo(mutableListOf(), kayttajaMapper::toDto)
    }

    override fun findKouluttajatAndVastuuhenkilot(userId: String): MutableList<KayttajaDTO> {
        val existingUser = kayttajaRepository.findOneByUserId(userId).get()
        return (kayttajaRepository.findAllByUserAuthority(KOULUTTAJA) +
            kayttajaRepository.findAllByUserAuthorityAndYliopistoId(
                VASTUUHENKILO,
                existingUser.yliopisto?.id
            ))
            .mapTo(mutableListOf(), kayttajaMapper::toDto)
    }

    override fun delete(id: Long) {
        kayttajaRepository.deleteById(id)
    }
}
