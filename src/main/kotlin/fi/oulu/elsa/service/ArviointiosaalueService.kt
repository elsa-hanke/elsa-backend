package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.ArviointiosaalueDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Arviointiosaalue].
 */
interface ArviointiosaalueService {

    /**
     * Save a arviointiosaalue.
     *
     * @param arviointiosaalueDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(arviointiosaalueDTO: ArviointiosaalueDTO): ArviointiosaalueDTO

    /**
     * Get all the arviointiosaalues.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<ArviointiosaalueDTO>

    /**
     * Get the "id" arviointiosaalue.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<ArviointiosaalueDTO>

    /**
     * Delete the "id" arviointiosaalue.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
