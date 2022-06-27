package fi.elsapalvelu.elsa.web.rest.errors

import java.net.URI

const val ERR_CONCURRENCY_FAILURE: String = "error.concurrencyFailure"
const val ERR_VALIDATION: String = "error.validation"
const val PROBLEM_BASE_URL: String = "https://www.jhipster.tech/problem"
@JvmField
val DEFAULT_TYPE: URI = URI.create("$PROBLEM_BASE_URL/problem-with-message")
@JvmField
val CONSTRAINT_VIOLATION_TYPE: URI = URI.create("$PROBLEM_BASE_URL/constraint-violation")
val JSON_FETCHING_ERROR = "Datan hakeminen epäonnistui"
val JSON_DATA_PROSESSING_ERROR = "Datan prosessointio epäonnistui"
val JSON_MAPPING_ERROR = "Data mäppäys epäonnistui"
