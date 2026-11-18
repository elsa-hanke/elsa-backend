package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError

class ArkistointiProcessingException(val error: ArkistointiError, val retryable: Boolean = true) : RuntimeException(error.code)
