package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OsaamisenArviointiDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.OsaamisenArviointi].
 */
interface OsaamisenArviointiService {

    /**
     * Save a osaamisenArviointi.
     *
     * @param osaamisenArviointiDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(osaamisenArviointiDTO: OsaamisenArviointiDTO): OsaamisenArviointiDTO

    /**
     * Get all the osaamisenArviointis.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<OsaamisenArviointiDTO>

    /**
     * Get the "id" osaamisenArviointi.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<OsaamisenArviointiDTO>

    /**
     * Delete the "id" osaamisenArviointi.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
