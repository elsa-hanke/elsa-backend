package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.OsaamisalueenArviointiDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.OsaamisalueenArviointi].
 */
interface OsaamisalueenArviointiService {

    /**
     * Save a osaamisalueenArviointi.
     *
     * @param osaamisalueenArviointiDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(osaamisalueenArviointiDTO: OsaamisalueenArviointiDTO): OsaamisalueenArviointiDTO

    /**
     * Get all the osaamisalueenArviointis.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<OsaamisalueenArviointiDTO>

    /**
     * Get the "id" osaamisalueenArviointi.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<OsaamisalueenArviointiDTO>

    /**
     * Delete the "id" osaamisalueenArviointi.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
