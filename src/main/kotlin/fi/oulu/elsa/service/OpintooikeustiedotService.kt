package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.OpintooikeustiedotDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Opintooikeustiedot].
 */
interface OpintooikeustiedotService {

    /**
     * Save a opintooikeustiedot.
     *
     * @param opintooikeustiedotDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(opintooikeustiedotDTO: OpintooikeustiedotDTO): OpintooikeustiedotDTO

    /**
     * Get all the opintooikeustiedots.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<OpintooikeustiedotDTO>

    /**
     * Get the "id" opintooikeustiedot.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<OpintooikeustiedotDTO>

    /**
     * Delete the "id" opintooikeustiedot.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
