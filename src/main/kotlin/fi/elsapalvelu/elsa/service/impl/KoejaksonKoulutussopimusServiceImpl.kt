package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KoejaksoRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*


@Service
@Transactional
class KoejaksonKoulutussopimusServiceImpl(
    private val koejaksoRepository: KoejaksoRepository,
    private val koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository
) : KoejaksonKoulutussopimusService {

    override fun save(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        koejaksoId: Long,
        userId: String
    ): KoejaksonKoulutussopimusDTO {
        val koejakso =
            koejaksoRepository.findById(koejaksoId).orElseThrow {
                BadRequestAlertException(
                    "Epäkelvollinen id",
                    "koejakso",
                    "idnull"
                )
            }

        if (koejakso.erikoistuvaLaakari?.kayttaja?.user?.id != userId) {
            throw BadRequestAlertException(
                "Koejakson täytyy kuulua kirjautuneelle käyttäjälle",
                "koejakso",
                "dataillegal"
            )
        }

        var koulutussopimus = koejaksonKoulutussopimusMapper.toEntity(koejaksonKoulutussopimusDTO)
        koulutussopimus.muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
        koulutussopimus.koejakso = koejakso
        koulutussopimus = koejaksonKoulutussopimusRepository.save(koulutussopimus)
        return koejaksonKoulutussopimusMapper.toDto(koulutussopimus)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO> {
        return koejaksonKoulutussopimusRepository.findById(id)
            .map(koejaksonKoulutussopimusMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksonKoulutussopimusRepository.deleteById(id)
    }
}
