package fi.oulu.elsa.service
import fi.oulu.elsa.service.dto.KayttajaDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.oulu.elsa.domain.Kayttaja].
 */
interface KayttajaService {

    /**
     * Save a kayttaja.
     *
     * @param kayttajaDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO

    /**
     * Get all the kayttajas.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<KayttajaDTO>

    /**
     * Get the "id" kayttaja.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<KayttajaDTO>

    /**
     * Delete the "id" kayttaja.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
