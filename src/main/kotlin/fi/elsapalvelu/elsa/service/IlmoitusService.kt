package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.IlmoitusDTO

interface IlmoitusService {

    fun findAll(): List<IlmoitusDTO>

    fun findOne(id: Long): IlmoitusDTO?

    fun create(ilmoitusDTO: IlmoitusDTO): IlmoitusDTO

    fun update(ilmoitusDTO: IlmoitusDTO): IlmoitusDTO?

    fun delete(id: Long)

}
