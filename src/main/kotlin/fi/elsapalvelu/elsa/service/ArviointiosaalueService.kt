package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.ArviointiosaalueDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.Arviointiosaalue].
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
