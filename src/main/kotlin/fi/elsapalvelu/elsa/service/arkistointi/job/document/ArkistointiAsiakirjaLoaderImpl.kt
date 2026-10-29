package fi.elsapalvelu.elsa.service.arkistointi.job.document

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArkistointiAsiakirjaLoaderImpl(
    private val jdbc: JdbcTemplate
) : ArkistointiAsiakirjaLoader {

    @Transactional(readOnly = true)
    override fun load(viite: ArkistointiDocumentDescriptor): LoadedArkistointiAsiakirja {
        val asiakirjaId = viite.asiakirjaId
        // Each retry reads a fresh array without a managed entity graph or scheduler/job locks.
        val data = jdbc.query(
            "select d.data from asiakirja a join asiakirja_data d on d.id = a.asiakirja_data_id where a.id = ?",
            { rs, _ -> rs.getBytes(1) },
            asiakirjaId
        ).firstOrNull()
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing())
        if (data.isEmpty()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.empty())
        }
        val expectedChecksum = viite.sha256
        if (!ArkistointiChecksum.sha256(data).equals(expectedChecksum, ignoreCase = true)) {
            throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.checksumMismatch()
            )
        }

        // Only saved job metadata crosses the transaction boundary. The adapter must not mutate sisalto.
        return LoadedArkistointiAsiakirja(
            asiakirjaId,
            viite.asiakirjatyyppi,
            viite.jarjestysnumero,
            viite.filename,
            viite.contentType,
            expectedChecksum,
            data
        )
    }
}
