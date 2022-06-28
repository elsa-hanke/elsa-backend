package fi.elsapalvelu.elsa.extensions

// Testaa täsmäävyyttä syötteeseen, jossa on mukana wildcardeja (? ja/tai *). ? täsmää mihin tahansa merkkiin,
// mutta lukumäärän on oltava sama. Esim. "testi" täsmää syötteeseen "te?ti", mutta ei syötteeseen "te??ti".
// * täsmää myös mihin tahansa merkkiin, muttu merkkien määrä voi toistua n kertaa. Esim. "teabcdefgsti" täsmää
// syötteeseen "te*sti".
fun String.match(wildcardInput: String): Boolean {
    if ((this.isEmpty() && wildcardInput.isEmpty()) ||
        (wildcardInput.length == 1 && wildcardInput.first() == '*') ||
        (this.length == 1 && (wildcardInput == "?" || wildcardInput == "*"))) return true

    if (wildcardInput.length > 1 && wildcardInput.first() == '*' && this.isEmpty()) return false

    if ((this.isNotEmpty() && wildcardInput.length > 1 && wildcardInput.first() == '?') || (wildcardInput.isNotEmpty() &&
            this.isNotEmpty() && wildcardInput.first() == this.first())
    ) {
        return this.substring(1).match(wildcardInput.substring(1))
    }

    if (this.isNotEmpty() && wildcardInput.isNotEmpty() && wildcardInput.first() == '*') {
        return this.substring(1).match(wildcardInput) || this.match(wildcardInput.substring(1))
    }

    return false
}
