package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArkistointiAsiakirjaLoaderImpl(
    private val asiakirjaRepository: AsiakirjaRepository
) : ArkistointiAsiakirjaLoader {

    @Transactional(readOnly = true)
    override fun load(viite: ArkistointiJobAsiakirja): LoadedArkistointiAsiakirja {
        val identifier = viite.filename ?: viite.id?.toString() ?: "tuntematon"
        val asiakirjaId = viite.asiakirja?.id
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        val asiakirja = asiakirjaRepository.findOneWithDataById(asiakirjaId)
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        val data = asiakirja.asiakirjaData?.data
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        if (data.isEmpty()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.empty(identifier))
        }
        val expectedChecksum = viite.sha256
            ?: throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.checksumMismatch(identifier)
            )
        if (!ArkistointiChecksum.sha256(data).equals(expectedChecksum, ignoreCase = true)) {
            throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.checksumMismatch(identifier)
            )
        }

        return LoadedArkistointiAsiakirja(viite, data.copyOf())
    }
}
