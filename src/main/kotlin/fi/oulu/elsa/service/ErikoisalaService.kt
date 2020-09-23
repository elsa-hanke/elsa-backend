package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.ErikoisalaDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Erikoisala].
 */
interface ErikoisalaService {

    /**
     * Save a erikoisala.
     *
     * @param erikoisalaDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO

    /**
     * Get all the erikoisalas.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<ErikoisalaDTO>

    /**
     * Get the "id" erikoisala.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<ErikoisalaDTO>

    /**
     * Delete the "id" erikoisala.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
