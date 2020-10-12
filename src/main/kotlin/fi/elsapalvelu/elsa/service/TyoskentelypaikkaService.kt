package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.TyoskentelypaikkaDTO
import java.util.Optional

/**
 * Service Interface for managing [fi.elsapalvelu.elsa.domain.Tyoskentelypaikka].
 */
interface TyoskentelypaikkaService {

    /**
     * Save a tyoskentelypaikka.
     *
     * @param tyoskentelypaikkaDTO the entity to save.
     * @return the persisted entity.
     */
    fun save(tyoskentelypaikkaDTO: TyoskentelypaikkaDTO): TyoskentelypaikkaDTO

    /**
     * Get all the tyoskentelypaikkas.
     *
     * @return the list of entities.
     */
    fun findAll(): MutableList<TyoskentelypaikkaDTO>

    /**
     * Get the "id" tyoskentelypaikka.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    fun findOne(id: Long): Optional<TyoskentelypaikkaDTO>

    /**
     * Delete the "id" tyoskentelypaikka.
     *
     * @param id the id of the entity.
     */
    fun delete(id: Long)
}
