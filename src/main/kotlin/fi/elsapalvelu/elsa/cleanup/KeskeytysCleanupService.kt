package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika

import org.springframework.stereotype.Component

interface  KeskeytysCleanupService {
    fun cleanKeskeytysajat(keskeytysajat: Set<Keskeytysaika>?)
    fun cleanKeskeytysaika(keskeytys: Keskeytysaika)
}

@Component
class KeskeytysCleanupServiceImpl : KeskeytysCleanupService {

    override fun cleanKeskeytysajat(keskeytysajat: Set<Keskeytysaika>?) {
        keskeytysajat?.forEach { cleanKeskeytysaika(it) }
    }


    override fun cleanKeskeytysaika(keskeytys: Keskeytysaika) {
        val s = "s t o p"
    }

}
