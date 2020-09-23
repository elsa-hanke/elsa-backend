package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.OsoiteDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Osoite].
 */
interface OsoiteService {

    /**
     * Save a osoite.
     *
     * @param osoiteDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(osoiteDTO: OsoiteDTO): OsoiteDTO

    /**
     * Get all the osoites.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<OsoiteDTO>

    /**
     * Get the "id" osoite.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<OsoiteDTO>

    /**
     * Delete the "id" osoite.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
