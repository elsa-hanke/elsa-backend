package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.HopsDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.Hops].
 */
interface HopsService {

    /**
     * Save a hops.
     *
     * @param hopsDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(hopsDTO: HopsDTO): HopsDTO

    /**
     * Get all the hops.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<HopsDTO>

    /**
     * Get the "id" hops.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<HopsDTO>

    /**
     * Delete the "id" hops.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
