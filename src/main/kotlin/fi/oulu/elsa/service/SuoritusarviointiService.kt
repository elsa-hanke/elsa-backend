package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.SuoritusarviointiDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Suoritusarviointi].
 */
interface SuoritusarviointiService {

    /**
     * Save a suoritusarviointi.
     *
     * @param suoritusarviointiDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO

    /**
     * Get all the suoritusarviointis.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<SuoritusarviointiDTO>

    /**
     * Get the "id" suoritusarviointi.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<SuoritusarviointiDTO>

    /**
     * Delete the "id" suoritusarviointi.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
