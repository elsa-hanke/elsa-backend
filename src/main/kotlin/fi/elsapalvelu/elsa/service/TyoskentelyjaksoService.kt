package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import java.util.Optional

interface TyoskentelyjaksoService {

    fun save(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): TyoskentelyjaksoDTO

    fun findAll(): MutableList<TyoskentelyjaksoDTO>

    fun findAllWhereTyoskentelypaikkaIsNull(): MutableList<TyoskentelyjaksoDTO>

    fun findOne(id: Long): Optional<TyoskentelyjaksoDTO>

    fun delete(id: Long)
}
