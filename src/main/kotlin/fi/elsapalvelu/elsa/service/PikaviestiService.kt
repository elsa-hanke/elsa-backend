package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.PikaviestiDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.Pikaviesti].
 */
interface PikaviestiService {

    /**
     * Save a pikaviesti.
     *
     * @param pikaviestiDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(pikaviestiDTO: PikaviestiDTO): PikaviestiDTO

    /**
     * Get all the pikaviestis.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<PikaviestiDTO>

    /**
     * Get the "id" pikaviesti.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<PikaviestiDTO>

    /**
     * Delete the "id" pikaviesti.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
