package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.Koejakso].
 */
interface KoejaksoService {

    /**
     * Save a koejakso.
     *
     * @param koejaksoDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(koejaksoDTO: KoejaksoDTO): KoejaksoDTO

    /**
     * Get all the koejaksos.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<KoejaksoDTO>

    /**
     * Get the "id" koejakso.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<KoejaksoDTO>

    /**
     * Delete the "id" koejakso.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
