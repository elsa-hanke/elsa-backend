package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.required
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArkistointiAsiakirjaLoaderImpl(
    private val jdbc: JdbcTemplate
) : ArkistointiAsiakirjaLoader {

    @Transactional(readOnly = true)
    override fun load(viite: ArkistointiJobAsiakirja): LoadedArkistointiAsiakirja {
        val asiakirjaId = viite.asiakirja?.id
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing())
        // The ResultSet owns this byte array. No managed Asiakirja/AsiakirjaData or shared cache
        // retains it after prepare commits; each retry reads a fresh array from PostgreSQL.
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
            ?: throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.checksumMismatch()
            )
        if (!ArkistointiChecksum.sha256(data).equals(expectedChecksum, ignoreCase = true)) {
            throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.checksumMismatch()
            )
        }

        // Only saved job metadata crosses the transaction boundary. The adapter must not mutate sisalto.
        return LoadedArkistointiAsiakirja(
            asiakirjaId,
            viite.asiakirjatyyppi.required(),
            viite.jarjestysnumero.required(),
            viite.filename.required(),
            viite.contentType.required(),
            expectedChecksum,
            data
        )
    }
}
