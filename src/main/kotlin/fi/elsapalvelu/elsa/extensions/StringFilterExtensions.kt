package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.User_
import tech.jhipster.service.filter.StringFilter
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Join
import javax.persistence.criteria.Predicate

fun StringFilter?.toNimiPredicate(
    userJoin: Join<Kayttaja, User>,
    criteriaBuilder: CriteriaBuilder,
    langkey: String?
): Predicate? {
    return this?.let {
        val locale = Locale.forLanguageTag(langkey ?: "fi")
        val searchTerm = "%${this.contains?.lowercase(locale)}%"

        var firstNameLastNameExpr = criteriaBuilder.concat(criteriaBuilder.lower(userJoin.get(User_.firstName)), " ")
        firstNameLastNameExpr =
            criteriaBuilder.concat(firstNameLastNameExpr, criteriaBuilder.lower(userJoin.get(User_.lastName)))

        var lastNameFirstNameExpr = criteriaBuilder.concat(criteriaBuilder.lower(userJoin.get(User_.lastName)), " ")
        lastNameFirstNameExpr =
            criteriaBuilder.concat(lastNameFirstNameExpr, criteriaBuilder.lower(userJoin.get(User_.firstName)))

        criteriaBuilder.or(
            criteriaBuilder.like(
                firstNameLastNameExpr,
                searchTerm
            ),
            criteriaBuilder.like(
                lastNameFirstNameExpr,
                searchTerm
            )
        )
    }
}
