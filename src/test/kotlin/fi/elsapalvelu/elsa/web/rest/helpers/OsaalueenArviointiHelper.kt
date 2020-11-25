package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.OsaalueenArviointi

class OsaalueenArviointiHelper {

    companion object {

        private const val DEFAULT_ARVIO: Int = 1
        private const val UPDATED_ARVIO: Int = 2

        @JvmStatic
        fun createEntity(): OsaalueenArviointi {
            val osaalueenArviointi = OsaalueenArviointi(
                arvio = DEFAULT_ARVIO
            )

            return osaalueenArviointi
        }

        @JvmStatic
        fun createUpdatedEntity(): OsaalueenArviointi {
            val osaalueenArviointi = OsaalueenArviointi(
                arvio = UPDATED_ARVIO
            )

            return osaalueenArviointi
        }
    }
}
